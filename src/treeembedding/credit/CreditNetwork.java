package treeembedding.credit;

import gtna.data.Single;
import gtna.graph.Edge;
import gtna.graph.Edges;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.ParentChild;
import gtna.graph.spanningTree.SpanningTree;
import gtna.graph.weights.EdgeWeights;
import gtna.io.DataWriter;
import gtna.io.graphWriter.GtnaGraphWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.transformation.spanningtree.MultipleSpanningTree;
import gtna.transformation.spanningtree.MultipleSpanningTree.Direct;
import gtna.util.Config;
import gtna.util.Distribution;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;

import treeembedding.credit.partioner.Partitioner;
import treeembedding.treerouting.TreeCoordinates;
import treeembedding.treerouting.Treeroute;
import treeembedding.treerouting.TreerouteSilentW;
import treeembedding.vouteoverlay.Treeembedding;

public class CreditNetwork extends Metric {
	//input parameters
	double epoch; //interval for stabilization overhead (=epoch between spanning tree recomputations if !dynRepair)
	Vector<Transaction> transactions; //vector of transactions, sorted by time 
	Treeroute ra; //routing algorithm
	boolean dynRepair; //true if topology changes are immediately fixed rather than recomputation each epoch
	boolean multi; //using multi-party computation to determine minimum or do routing adhoc 
	double requeueInt; //interval until a failed transaction is re-tried; irrelevant if !dynRepair as 
	                   //retry is start of next epoch
	Partitioner part; //method to partition overall transaction value on paths
	int[] roots; // spanning tree roots
	int maxTries; 
	Queue<double[]> newLinks;
	
	Vector<Edge> zeroEdges;
	Graph graph;
	boolean update;
	HashMap<Edge, Double> originalWeight;
	
	
	//computed metrics
	double[] stab; //stabilization overhead over time (in #messages)
	double stab_av; //average stab overhead
	Distribution transactionMess; //distribution of #messages needed for one transaction trial 
	                             //(i.e., each retransaction count as new transactions)
	Distribution transactionMessRe; //distribution of #messages needed, counting re-transactions as part of transaction
	Distribution transactionMessSucc; //messages successful transactions
	Distribution transactionMessFail; //messages failed transactions 
	Distribution pathL; //distribution of path length (sum over all trees!)
	Distribution pathLRe; //distribution of path length counting re-transactions as one
	Distribution pathLSucc; //path length successful transactions
	Distribution pathLFail; //path length failed transactions 
	Distribution reLandMes; //messages receiver-landmarks communication
	Distribution landSenMes; //messages receiver-landmarks communication
	Distribution trials; //Distribution of number of trials needed to get through
	Distribution path_single; //distribution of single paths
	Distribution delay; //distribution of hop delay
	Distribution[] pathsPerTree; //distribution of single paths per tree
	Distribution path_singleFound; //distribution of single paths, only discovered paths 
	Distribution delaySucc; //distribution of hop delay, successful queries
	Distribution[] pathsPerTreeFound; //distribution of single paths per tree, only discovered paths 
	Distribution path_singleNF; //distribution of single paths, not found dest
	Distribution delayFail; //distribution of hop delay, failed queries
	Distribution[] pathsPerTreeNF; //distribution of single paths per tree, not found dest
	double[] passRoot;
	double passRootAll = 0;
	int rootPath = 0;
	double success_first; //fraction of transactions successful in first try
	double success; // fraction of transactions successful at all
	double[] succs;
	
	boolean log = false;
	
	
	public CreditNetwork(String file, String name, double epoch, Treeroute ra, boolean dynRep, 
			boolean multi, double requeueInt, Partitioner part, int[] roots, int max, String links, boolean up){
		super("CREDIT_NETWORK", new Parameter[]{new StringParameter("NAME", name), new DoubleParameter("EPOCH", epoch),
				new StringParameter("RA", ra.getKey()), new BooleanParameter("DYN_REPAIR", dynRep), 
				new BooleanParameter("MULTI", multi), new IntParameter("TREES", roots.length),
				new DoubleParameter("REQUEUE_INTERVAL", requeueInt), new StringParameter("PARTITIONER", part.getName()),
				new IntParameter("MAX_TRIES",max)});
		this.epoch = epoch;
		this.ra = ra;
		this.multi = multi;
		this.dynRepair = dynRep;
		transactions = this.readList(file);
		this.requeueInt = requeueInt;
		this.part = part;
		this.roots = roots;
		this.maxTries = max;
		if (links != null){
			this.newLinks = this.readLinks(links);
		} else {
			this.newLinks = new LinkedList<double[]>();
		}
		this.update = up;
	}
	
	public CreditNetwork(String file, String name, double epoch, Treeroute ra, boolean dynRep, 
			boolean multi, double requeueInt, Partitioner part, int[] roots, int max, String links){
		this(file,name,epoch,ra,dynRep, multi, requeueInt, part, roots, max, links, true);
	}
	
	public CreditNetwork(String file, String name, double epoch, Treeroute ra, boolean dynRep, 
			boolean multi, double requeueInt, Partitioner part, int[] roots, int max){
		this(file,name,epoch,ra,dynRep, multi, requeueInt, part, roots, max, null,true);
	}
	
	public CreditNetwork(String file, String name, double epoch, Treeroute ra, boolean dynRep, 
			boolean multi, double requeueInt, Partitioner part, int[] roots, int max, boolean up){
		this(file,name,epoch,ra,dynRep, multi, requeueInt, part, roots, max, null,up);
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
        //init: construct trees (currently randomly) and init variables
		
		  Treeembedding embed = new Treeembedding("T",60,roots, MultipleSpanningTree.Direct.TWOPHASE);
		if (!g.hasProperty("SPANNINGTREE_0")){  
		  g = embed.transform(g);
		}
		
		int count = 0;
		long[] trys = new long[2];
		long[] path = new long[2];
		long[] pathAll = new long[2];
		long[] pathSucc = new long[2];
		long[] pathFail = new long[2];
		long[] mes = new long[2];
		long[] mesAll = new long[2];
		long[] mesSucc = new long[2];
		long[] mesFail = new long[2];
		long[] landSen = new long[2];
		long[] reLand = new long[2];
		long[] pathS = new long[2];
		long[][] pathSs = new long[this.roots.length][2];
		long[] pathSF = new long[2];
		long[][] pathSsF = new long[this.roots.length][2];
		long[] pathSNF = new long[2];
		long[][] pathSsNF = new long[this.roots.length][2];
		int[][] cPerPath = new int[this.roots.length][2];
		int[] cAllPath = new int[2];
		long[] del = new long[2];
		long[] delSucc = new long[2];
		long[] delFail = new long[2];
		success_first = 0;
		success = 0;
		this.passRoot = new double[this.roots.length];
		Vector<Integer> stabMes = new Vector<Integer>();
		Vector<Double> succR = new Vector<Double>();
		Node[] nodes = g.getNodes();
		boolean[] exclude = new boolean[nodes.length]; 
		
		//go over transactions
		LinkedList<Transaction> toRetry = new LinkedList<Transaction>(); 
		int epoch_old = 0;
		int epoch_cur = 0;
		int cur_stab = 0;
		int c = 0;
		double cur_succ = 0;
		int cur_count = 0;
		Random rand = new Random();
		CreditLinks edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		while (c < this.transactions.size() || toRetry.size() > 0){
			//0: decide which is next transaction: previous one or new one? and add new links if any
			Transaction a = c < this.transactions.size()?this.transactions.get(c):null;
			Transaction b = toRetry.peek();
			Transaction cur = null;
			if (a != null && (b == null || a.time < b.time)) {
			   cur = a;
			   c++;
			} else {
				cur = toRetry.poll();
			}
			
			epoch_cur = (int)Math.floor(cur.time/epoch);
			if (!this.newLinks.isEmpty()){
				double nt = this.newLinks.peek()[0];
				while (nt <= cur.time){
					double[] link = this.newLinks.poll();
					int st = this.addLink((int)link[1], (int)link[2], link[3], g);
					cur_stab = cur_stab + st;
					nt = this.newLinks.isEmpty()?Double.MAX_VALUE:this.newLinks.peek()[0];
				}
			}
			
			count++;
			//if (log){
				System.out.println("Perform transaction s="+ cur.src + " d= "+ cur.dst + 
						" val= " + cur.val + " time= "+ cur.time);
			//}
				
			
			//1: check if and how many spanning tree re-construction took place since last transaction
			//do 1 (!) re-computation if there was any & set stabilization cost
			if (epoch_cur != epoch_old){
				if (!this.dynRepair){
					if (log){
						System.out.println("Recompute spt");
					}
					for (int i = 0; i < roots.length; i++){
						g.removeProperty("SPANNINGTREE_"+i);
						g.removeProperty("TREE_COORDINATES_"+i);
					}
					g = embed.transform(g);
					for (int j = epoch_old +1; j <= epoch_cur; j++){
						stabMes.add(this.roots.length*2*this.computeNonZeroEdges(g, edgeweights));
					}
				} else {
					stabMes.add(cur_stab);
					for (int j = epoch_old +2; j <= epoch_cur; j++){
						stabMes.add(0);
					}
					cur_stab = 0;
				}
				cur_succ = cur_count ==0?1:cur_succ/cur_count;
				succR.add(cur_succ);
				for (int j = epoch_old +2; j <= epoch_cur; j++){
					succR.add(1.0);
				}
				cur_count = 0;
				cur_succ = 0;
			} 
			
			//2: execute the transaction
			int[] results;
			originalWeight = new HashMap<Edge, Double>();
			if (this.multi){
				results = this.routeMulti(cur, g, nodes, exclude, edgeweights);
			} else {
				results = this.routeAdhoc(cur, g, nodes, exclude, edgeweights);
			}
			//reset to old weights if failed
			if (results[0] == -1){
				this.weightUpdate(edgeweights, originalWeight);
				this.zeroEdges = new Vector<Edge>();
			}
			cur_count++;
			if (results[0] == 0){
				cur_succ++;
				System.out.println("Success");
			}
			
			//re-queue if necessary
			cur.addPath(results[1]);
			cur.addMes(results[4]);
			if (results[0] == -1){
				cur.incRequeue(cur.time+rand.nextDouble()*this.requeueInt);
				if (cur.requeue <= this.maxTries){
					int st = 0;
					while (st < toRetry.size() && toRetry.get(st).time < cur.time){
						st++;
					}
				    toRetry.add(st, cur);
				} else {
					mesAll = this.inc(mesAll, cur.mes);
					pathAll = this.inc(pathAll, cur.path);
				}
			}
			
			//3 update metrics accordingly
			path = this.inc(path, results[1]);
			reLand = this.inc(reLand, results[2]);
			landSen = this.inc(landSen, results[3]);
			mes = this.inc(mes, results[4]);
			del = this.inc(del, results[5]);
			if (results[0] == 0){
				trys = this.inc(trys, cur.requeue);
				this.success++;
				if (cur.requeue == 0){
					this.success_first++;
				}
				mesAll = this.inc(mesAll, cur.mes);
				pathAll = this.inc(pathAll, cur.path);
				pathSucc = this.inc(pathSucc, results[1]);
				mesSucc = this.inc(mesSucc, results[4]);
				delSucc = this.inc(delSucc, results[5]);
			} else {
				pathFail = this.inc(pathFail, results[1]);
				mesFail = this.inc(mesFail, results[4]);
				delFail = this.inc(delFail, results[5]);
			}
			for (int j = 6; j < results.length; j++){
				int index = 0;
				if (results[j] < 0){
					index = 1;
				}
				cPerPath[j-6][index]++;
				cAllPath[index]++;
				int val = Math.abs(results[j]);
				pathS = this.inc(pathS, val);
				pathSs[j-6] = this.inc(pathSs[j-6], val);
				if (index == 0){
					pathSF = this.inc(pathSF, val);
					pathSsF[j-6] = this.inc(pathSsF[j-6], val);
				} else {
					pathSNF = this.inc(pathSNF, val);
					pathSsNF[j-6] = this.inc(pathSsNF[j-6], val);
				}
			}
			
			//4 post-processing: remove edges set to 0, update spanning tree if dynRapir
			epoch_old = epoch_cur;
			if (this.dynRepair && zeroEdges != null){
				for (int j = 0; j < this.roots.length; j++){
					SpanningTree sp =(SpanningTree)g.getProperty("SPANNINGTREE_"+j);
					for (int k = 0; k < this.zeroEdges.size(); k++){
						Edge e = this.zeroEdges.get(k);
						int s = e.getSrc();
						int t = e.getDst();
						int cut = -1;
						if (sp.getParent(s) == t){
							cut = s;
						}
						if (sp.getParent(t) == s){
							cut = t;
						}
						if (cut != -1){
							if (log){
								System.out.println("Repair tree " + j + " at expired edge ("+s + ","+t+")");
							}
						    TreeCoordinates coords = (TreeCoordinates)g.getProperty("TREE_COORDINATES_"+j);	
						    cur_stab = cur_stab + this.repairTree(nodes, sp, coords, 
						    		cut, (CreditLinks) g.getProperty("CREDIT_LINKS"));	
						}
					}
				}
			}
			if (!this.update){
				this.weightUpdate(edgeweights, originalWeight);
			}
		}
		if (this.dynRepair){
			stabMes.add(cur_stab);
		}
		
		
        //compute metrics
		this.pathL = new Distribution(path,count);
		this.transactionMess = new Distribution(mes, count);
		this.pathLRe = new Distribution(pathAll,transactions.size());
		this.transactionMessRe = new Distribution(mesAll, transactions.size());
		this.pathLSucc = new Distribution(pathSucc,(int)this.success);
		this.transactionMessSucc = new Distribution(mesSucc, (int)this.success);
		this.pathLFail = new Distribution(pathFail,count-(int)this.success);
		this.transactionMessFail = new Distribution(mesFail, count-(int)this.success);
		this.reLandMes = new Distribution(reLand,count);
		this.landSenMes = new Distribution(landSen,count);
		this.trials = new Distribution(trys,(int)this.success);
		this.path_single = new Distribution(pathS, cAllPath[0] + cAllPath[1]);
		this.path_singleFound = new Distribution(pathSF, cAllPath[0]);
		this.path_singleNF = new Distribution(pathSNF, cAllPath[1]);
		this.delay = new Distribution(del, count);
		this.delaySucc = new Distribution(delSucc, (int)this.success);
		this.delayFail = new Distribution(delFail,count-(int)this.success);
		this.pathsPerTree = new Distribution[this.roots.length];
		this.pathsPerTreeFound = new Distribution[this.roots.length];
		this.pathsPerTreeNF = new Distribution[this.roots.length];
		for (int j = 0; j < this.pathsPerTree.length; j++){
			this.pathsPerTree[j] = new Distribution(pathSs[j],cPerPath[j][0] + cPerPath[j][1]);
			this.pathsPerTreeFound[j] = new Distribution(pathSsF[j],cPerPath[j][0]);
			this.pathsPerTreeNF[j] = new Distribution(pathSsNF[j],cPerPath[j][1]);
		}
		this.success = this.success/(double)transactions.size();
		this.success_first = this.success_first/(double)transactions.size();
		stab = new double[stabMes.size()];
		this.stab_av = 0;
		for (int i = 0; i < this.stab.length; i++){
			stab[i] = stabMes.get(i);
			this.stab_av = this.stab_av + stab[i];
		}
		this.succs = new double[succR.size()];
		for (int i = 0; i < this.succs.length; i++){
			succs[i] = succR.get(i);
		}
		this.stab_av = this.stab_av/(double)stab.length;
		this.passRootAll = this.passRootAll/this.rootPath;
		for (int j = 0; j < this.passRoot.length; j++){
			this.passRoot[j] = this.passRoot[j]/count;
		}
		this.graph = g;
		
	}
	
	private int computeNonZeroEdges(Graph g, CreditLinks ew){
		Edges edges = g.getEdges();
		int c = 0;
		for (Edge e: edges.getEdges()){
			if (e.getSrc() < e.getDst()){
				if (ew.getPot(e.getSrc(), e.getDst()) > 0 || ew.getPot(e.getDst(), e.getSrc()) > 0){
					c++;
				}
			}
		}
		return c;
	}
	
	/**
	 * reconnect disconnected branch with root subroot
	 * @param sp
	 * @param subroot
	 */
	private int repairTree(Node[] nodes, SpanningTree sp, TreeCoordinates coords, int subroot, CreditLinks ew){
		if (!update){
			int mes = 0;
			Queue<Integer> q1 = new LinkedList<Integer>();
			Queue<Integer> q2 = new LinkedList<Integer>();
			q1.add(subroot);
			while (!q1.isEmpty()){
				int node = q1.poll();
				int[] kids = sp.getChildren(node);
				for (int i = 0; i < kids.length; i++){
					mes++;
					q1.add(kids[i]);
				}
				mes = mes + MultipleSpanningTree.potParents(graph, nodes[node],
						Direct.EITHER, ew).length;
			}
			return mes; 
		}
		//remove old tree info of all descendants of subroot
		int mes = 0;
		Queue<Integer> q1 = new LinkedList<Integer>();
		Queue<Integer> q2 = new LinkedList<Integer>();
		q1.add(subroot);
		while (!q1.isEmpty()){
			int node = q1.poll();
			int[] kids = sp.getChildren(node);
			for (int i = 0; i < kids.length; i++){
				mes++;
				q1.add(kids[i]);
			}
			sp.removeNode(node);
			q2.add(node);
		}
		
		
		Random rand = new Random();
		MultipleSpanningTree.Direct[] dir = {Direct.BOTH, Direct.EITHER, Direct.NONE};
		for (int k = 0; k < dir.length; k++) {
			int count = q2.size();
			while (count > 0) {
				int node = q2.poll();
				Vector<Integer> bestN = new Vector<Integer>();
				int mind = Integer.MAX_VALUE;
				int[] out = MultipleSpanningTree.potParents(graph, nodes[node],
						dir[k], ew);
				for (int i : out) {
					if (sp.getParent(i) != -2) {
						if (sp.getDepth(i) < mind) {
							mind = sp.getDepth(i);
							bestN = new Vector<Integer>();
						}
						if (sp.getDepth(i) == mind) {
							bestN.add(i);
						}
					}
				}
				if (bestN.size() > 0) {
					mes = mes + MultipleSpanningTree.potParents(graph, nodes[node],
							Direct.EITHER, ew).length;
					int pa = bestN.get(rand.nextInt(bestN.size()));
					sp.addParentChild(pa, node);
					int[] pa_coord = coords.getCoord(pa);
					int[] child_coord = new int[pa_coord.length + 1];
					for (int i = 0; i < pa_coord.length; i++) {
						child_coord[i] = pa_coord[i];
					}
					child_coord[pa_coord.length] = rand.nextInt();
					coords.setCoord(node, child_coord);
					count = q2.size();
				} else {
					q2.add(node);
					count--;
				}
			}
		}
		return mes;
	}
	
	private int addLink(int src, int dst, double weight, Graph g){
		CreditLinks edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		//if (log) System.out.println("Added link " + src + " " + dst + " " + weight);
		double[] ws = new double[3];
		double old;
		if (src < dst){
			ws = edgeweights.getWeights(src, dst);
			old = ws[2];
			ws[2] = weight;
		} else {
			ws = edgeweights.getWeights(dst,src);
			old = ws[0];
			ws[0] = -weight;
		}
		
		int st = 0;
		if (this.dynRepair){
			Node[] nodes = g.getNodes();
			if(old == 0 && weight != 0){
				//new edge might be useful if any of nodes connected to tree by zero edge
				for (int j = 0; j < this.roots.length; j++){
					SpanningTree sp =(SpanningTree)g.getProperty("SPANNINGTREE_"+j);
				    boolean zpSrc = this.isZeroPath(sp, src, edgeweights);
				    boolean zpDst = this.isZeroPath(sp, dst, edgeweights);
				    if (zpSrc){
				    	TreeCoordinates coords = (TreeCoordinates)g.getProperty("TREE_COORDINATES_"+j);
				    	st = st + this.repairTree(nodes, sp, coords, src, (CreditLinks) g.getProperty("CREDIT_LINKS"));
				    }
				    if (zpDst){
				    	TreeCoordinates coords = (TreeCoordinates)g.getProperty("TREE_COORDINATES_"+j);
				    	st = st + this.repairTree(nodes, sp, coords, dst, (CreditLinks) g.getProperty("CREDIT_LINKS"));
				    }
				}
			}
			if (old != 0 && weight == 0){
				//expired edge => reconnect
				for (int j = 0; j < this.roots.length; j++){
					SpanningTree sp =(SpanningTree)g.getProperty("SPANNINGTREE_"+j);
				    int cut = -1;
				    if (sp.getParent(src) == dst){
					   cut = src;
				    }
				    if (sp.getParent(dst) == src){
					   cut = dst;
				    }
				    if (cut != -1){
					   if (log){
						System.out.println("Repair tree " + j + " at expired edge ("+src + ","+dst+")");
					   }
				       TreeCoordinates coords = (TreeCoordinates)g.getProperty("TREE_COORDINATES_"+j);	
				       st = st + this.repairTree(nodes, sp, coords, cut, (CreditLinks) g.getProperty("CREDIT_LINKS"));	
				    }
				
				}
			}
//			for (int j = 0; j < this.roots.length; j++){
//				SpanningTree sp =(SpanningTree)g.getProperty("SPANNINGTREE_"+j);
//				for (int k = 0; k < this.zeroEdges.size(); k++){
//					Edge e = this.zeroEdges.get(k);
//					int s = e.getSrc();
//					int t = e.getDst();
//					int cut = -1;
//					if (sp.getParent(s) == t){
//						cut = s;
//					}
//					if (sp.getParent(t) == s){
//						cut = t;
//					}
//					if (cut != -1){
//						if (log){
//							System.out.println("Repair tree " + j + " at expired edge ("+s + ","+t+")");
//						}
//					    TreeCoordinates coords = (TreeCoordinates)g.getProperty("TREE_COORDINATES_"+j);	
//					    st = st + this.repairTree(nodes, sp, coords, cut, (CreditLinks) g.getProperty("CREDIT_LINKS"));	
//					}
//				}
//			}
		}
		return st;
	}
	
	private boolean isZeroPath(SpanningTree sp, int node, CreditLinks edgeweights){
		int parent = sp.getParent(node);
		while (parent != -1){
			if (edgeweights.getPot(node, parent) > 0 && edgeweights.getPot(parent, node) > 0){
				node = parent;
				parent = sp.getParent(node);
			} else {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * routing using a multi-part computation: costs are
	 * i) finding path (one-way, but 3x path length as each neighbor needs to sign its predecessor and successors)
	 * ii) sending shares to all landmarks/roots from receiver 
	 * iii) sending results to sender from all landmarks/roots
	 * iv) testing paths (two-way)
	 * v) updating credit (one-way)
	 * @return {success?0:-1, sum(pathlength), receiver-landmark, landmarks-sender,overall message count, delay, p1, p2,...}
	 */
	private int[] routeMulti(Transaction cur, Graph g, Node[] nodes, boolean[] exclude, CreditLinks edgeweights){
		int[][] paths = new int[roots.length][];
		double[] vals;
		int src = cur.src;
		int dest = cur.dst;
		//compute paths and minimum credit along the paths
		double[] mins = new double[roots.length];
		for (int j = 0; j < mins.length; j++){
			paths[j] = ra.getRoute(src, dest, j, g, nodes, exclude);
			String path = "";
			for (int i = 0; i < paths[j].length; i++){
				path = path + " " + paths[j][i];
			}
			//System.out.println(j + " " + path);
			if (paths[j][paths[j].length-1] == dest){
				int l = src;
				int i = 1;
				double min = Double.MAX_VALUE;
				while (i < paths[j].length){
					int k = paths[j][i];
					double w = edgeweights.getPot(l,k);
					if (w < min){
						min = w;
					}
					l = k;
					i++;
				}
				mins[j] = min;
			}
			
		}
		//partition transaction value
		vals = part.partition(g, src,dest,cur.val,mins);
		
		//check if transaction works
		boolean succ = false;
		if (vals != null) {
			succ = true;
			for (int j = 0; j < paths.length; j++) {
				if (vals[j] > 0) {
					int l = paths[j][0];
					for (int i = 1; i < paths[j].length; i++) {
						int k = paths[j][i];
						Edge e = edgeweights.makeEdge(l, k);
						double w = edgeweights.getWeight(e);
						if (!originalWeight.containsKey(e)){
							originalWeight.put(e, w);
						}
						
						if (!edgeweights.setWeight(l, k, vals[j])) {
							succ = false;
							break;
						} else {
							if (log){
								System.out.println("----Set weight of ("+l+","+k+") to " + edgeweights.getWeight(e)
										+ "(previous " +w+ ")");
							}
						}
						l = k;
						
					}
					if (!succ) {
						break;
					}
				}
			}
			// update weights
			if (succ) {
				this.setZeros(edgeweights, originalWeight);
				if (log){
					System.out.println("Success");
				}
			} else {
				
				if (log){
					System.out.println("Failure");
				}
			}

		} else {
			if (log){
				System.out.println("Failure");
			}
		}
		
		
		//compute metrics
		int[] res = new int[6+this.roots.length];
		//success 
		if (!succ){
			res[0] = -1;
		}
		//path length
		for (int j = 0; j < paths.length; j++){
			if (paths[j][paths[j].length-1] == dest){
				res[1] = res[1] + paths[j].length-1;
				res[6+j] = paths[j].length-1;
			} else {
				res[1] = res[1] + paths[j].length-2;
				res[6+j] = -(paths[j].length-2);
			}
		}
		//receiver-landmarks
		for (int j = 0; j < paths.length; j++){
		   SpanningTree sp =(SpanningTree)g.getProperty("SPANNINGTREE_"+j);
		   int d = sp.getDepth(dest);
		   if (paths[j][paths[j].length-1] == dest){
			   res[2] = res[2]+d*paths.length;
		   }
		}	
		//landmarks-sender
		for (int j = 0; j < paths.length; j++){
			SpanningTree sp =(SpanningTree)g.getProperty("SPANNINGTREE_"+j);
			int d = sp.getDepth(src);
			if (paths[j][paths[j].length-1] == dest){
				res[3] = res[3]+d;
			}
		}	
		//all
		res[4] = res[1]+res[2]+res[3];
		for (int j = 0; j < paths.length; j++){
			if (vals != null && vals[j] > 0){
				res[4] = res[4]+2*(paths[j].length-1);
			}
		}
		//max path
		int max = 0;
		int maxp = 0;
		for (int j = 0; j < paths.length; j++){
			int pl;
			int last;
			if (ra instanceof TreerouteSilentW){
				SpanningTree sp =(SpanningTree)g.getProperty("SPANNINGTREE_"+j);
				pl = Math.max(sp.getDepth(src), sp.getDepth(dest));
				last = sp.getSrc();
			} else {
				pl = paths[j].length-1;
				last = dest;
			}
			for (int i = 0; i < paths.length; i++){
				SpanningTree sp =(SpanningTree)g.getProperty("SPANNINGTREE_"+i);
				if (pl + sp.getDepth(last) > max){
					max = pl + sp.getDepth(last);
				}
			}
			if (vals != null && vals[j] > 0 && paths[j] != null){
				if (paths[j].length-1 > maxp){
					maxp = paths[j].length-1;
				}
			}
		}
		int d = 0;
		for (int i = 0; i < paths.length; i++){
			SpanningTree sp =(SpanningTree)g.getProperty("SPANNINGTREE_"+i);
			if (sp.getDepth(src) > d){
				d = sp.getDepth(src);
			}
		}
		max = max + d;
		if (vals != null){
			max = max + 2*maxp;
		}
		res[5] = max;
		this.setRoots(paths);
		return res; 
	}
	
	private int[] routeAdhoc(Transaction cur, Graph g, Node[] nodes, boolean[] exclude, CreditLinks edgeweights){
		int[][] paths = new int[roots.length][];
		int src = cur.src;
		int dest = cur.dst;
		//distribute values on paths
		double[] vals = this.part.partition(g,src, dest, cur.val, roots.length);
		
		//check if transaction works
		boolean succ = true;
		this.zeroEdges = new Vector<Edge>();
		for (int j = 0; j < paths.length; j++){
			if (vals[j] != 0){
				int s = src;
				int d = dest;
				if (vals[j] < 0) {
					s = dest;
					d = src;
				}
			   paths[j] = this.ra.getRoute(s, d, j, g, nodes, exclude, edgeweights, vals[j]);
			   if (paths[j][paths[j].length-1] == -1){
				  succ = false;
			   } else {
				   int l = paths[j][0];
				   for (int i = 1; i < paths[j].length; i++){
						int k = paths[j][i]; 
						Edge e = edgeweights.makeEdge(l, k);
						double w = edgeweights.getWeight(e);
						if (!originalWeight.containsKey(e)){
							originalWeight.put(e, w);
						}
						edgeweights.setWeight(l,k,vals[j]);
						if (log){
							System.out.println("----Set weight of ("+l+","+k+") to " + edgeweights.getWeight(e)
									+ " (previous "+w+")");
						}
						if (edgeweights.getPot(l, k) == 0){
							this.zeroEdges.add(e);
						}
						l = k;
					}
			   }
			}
		}
		if (!succ){
			if (log){
				System.out.println("Failure");
			}
		} else {
			this.setZeros(edgeweights, originalWeight);
			if (log){
				System.out.println("Success");
			}
		}
		
		//compute metrics
		int[] res = new int[6+this.roots.length];
		//success 
		if (!succ){
			res[0] = -1;
		}
		//path length
		for (int j = 0; j < paths.length; j++){
			if (vals[j] > 0){
			  if (paths[j][paths[j].length-1] == dest){
				 res[1] = res[1] + paths[j].length-1;
				 res[6+j] = paths[j].length-1;
			  } else {
				 res[1] = res[1] + paths[j].length-2;
				 res[6+j] = -(paths[j].length-2);
			  }
			}
		}
		//overall messages
		if (res[0] != 1){
			res[4] = 3*res[1];
		} else {
			res[4] = 2*res[2];
		}
		//max path length
		int max = 0;
		for (int j = 0; j < paths.length; j++){
			if (vals[j] > 0){
			  if (paths[j][paths[j].length-1] == dest){
				 if (paths[j].length > max){
					 max = paths[j].length;
				 }
			  } else {
				  if (paths[j].length-1 > max){
						 max = paths[j].length-1;
					 }
			  }
			}
		}
		res[5] = 2*max;
		this.setRoots(paths);
		return res;
	}
	
	private void weightUpdate(CreditLinks edgeweights, HashMap<Edge, Double> updateWeight){
		Iterator<Entry<Edge, Double>> it = updateWeight.entrySet().iterator();
		while (it.hasNext()){
			Entry<Edge, Double> entry = it.next();
			edgeweights.setWeight(entry.getKey(), entry.getValue());
		}
	}
	

	private void setZeros(CreditLinks edgeweights, HashMap<Edge, Double> updateWeight){
		this.zeroEdges = new Vector<Edge>();
		Iterator<Entry<Edge, Double>> it = updateWeight.entrySet().iterator();
		while (it.hasNext()){
			Entry<Edge, Double> entry = it.next();
			int src = entry.getKey().getSrc();
			int dst = entry.getKey().getDst();
			if (edgeweights.getPot(src, dst) == 0){
				this.zeroEdges.add(new Edge(src,dst));
			}
			if (edgeweights.getPot(dst, src) == 0){
				this.zeroEdges.add(new Edge(dst,src));
			}
		}
	}

	@Override
	public boolean writeData(String folder) {
		boolean succ = true;
		succ &= DataWriter.writeWithIndex(this.transactionMess.getDistribution(),
				this.key+"_MESSAGES", folder);
		succ &= DataWriter.writeWithIndex(this.transactionMessRe.getDistribution(),
				this.key+"_MESSAGES_RE", folder);
		succ &= DataWriter.writeWithIndex(this.transactionMessSucc.getDistribution(),
				this.key+"_MESSAGES_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.transactionMessFail.getDistribution(),
				this.key+"_MESSAGES_FAIL", folder);
		
		succ &= DataWriter.writeWithIndex(this.pathL.getDistribution(),
				this.key+"_PATH_LENGTH", folder);
		succ &= DataWriter.writeWithIndex(this.pathLRe.getDistribution(),
				this.key+"_PATH_LENGTH_RE", folder);
		succ &= DataWriter.writeWithIndex(this.pathLSucc.getDistribution(),
				this.key+"_PATH_LENGTH_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.pathLFail.getDistribution(),
				this.key+"_PATH_LENGTH_FAIL", folder);
		
		succ &= DataWriter.writeWithIndex(this.reLandMes.getDistribution(),
				this.key+"_REC_LANDMARK", folder);
		succ &= DataWriter.writeWithIndex(this.landSenMes.getDistribution(),
				this.key+"_LANDMARK_SRC", folder);
		succ &= DataWriter.writeWithIndex(this.trials.getDistribution(),
				this.key+"_TRIALS", folder);
		succ &= DataWriter.writeWithIndex(this.stab,
				this.key+"_STABILIZATION", folder);
		
		succ &= DataWriter.writeWithIndex(this.path_single.getDistribution(),
				this.key+"_PATH_SINGLE", folder);
		succ &= DataWriter.writeWithIndex(this.path_singleFound.getDistribution(),
				this.key+"_PATH_SINGLE_FOUND", folder);
		succ &= DataWriter.writeWithIndex(this.path_singleNF.getDistribution(),
				this.key+"_PATH_SINGLE_NF", folder);
		succ &= DataWriter.writeWithIndex(this.succs,
				this.key+"_SUCC_RATIOS", folder);
		
		succ &= DataWriter.writeWithIndex(this.delay.getDistribution(),
				this.key+"_DELAY", folder);
		succ &= DataWriter.writeWithIndex(this.delaySucc.getDistribution(),
				this.key+"_DELAY_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.delayFail.getDistribution(),
				this.key+"_DELAY_FAIL", folder);
		
		double[][] s1 = new double[this.roots.length][];
		double[][] s2 = new double[this.roots.length][];
		double[][] s3 = new double[this.roots.length][];
		double[] av1 = new double[this.roots.length];
		double[] av2 = new double[this.roots.length];
		double[] av3 = new double[this.roots.length];
		for (int i = 0; i < s1.length; i++){
			s1[i] = this.pathsPerTree[i].getDistribution(); 
			av1[i] = this.pathsPerTree[i].getAverage();
			s2[i] = this.pathsPerTreeFound[i].getDistribution(); 
			av2[i] = this.pathsPerTreeFound[i].getAverage();
			s3[i] = this.pathsPerTreeNF[i].getDistribution();
			av3[i] = this.pathsPerTreeNF[i].getAverage();
		}
		succ &= DataWriter.writeWithIndex(av1, this.key+"_PATH_PERTREE_AV", folder);
		succ &= DataWriter.writeWithIndex(av2, this.key+"_PATH_PERTREE_FOUND_AV", folder);
		succ &= DataWriter.writeWithIndex(av3, this.key+"_PATH_PERTREE_NF_AV", folder);
		succ &= DataWriter.writeWithoutIndex(s1, this.key+"_PATH_PERTREE", folder);
		succ &= DataWriter.writeWithoutIndex(s2, this.key+"_PATH_PERTREE_FOUND", folder);
		succ &= DataWriter.writeWithoutIndex(s3, this.key+"_PATH_PERTREE_NF", folder);
		
		succ &= DataWriter.writeWithIndex(this.passRoot, this.key+"_ROOT_TRAF", folder);
		
		if (Config.getBoolean("SERIES_GRAPH_WRITE")) {
			(new GtnaGraphWriter()).writeWithProperties(graph, folder+"graph.txt");
		}
		
		
		return succ;
	}

	@Override
	public Single[] getSingles() {
		Single m_av = new Single("CREDIT_NETWORK_MES_AV", this.transactionMess.getAverage());
		Single m_Re_av = new Single("CREDIT_NETWORK_MES_RE_AV", this.transactionMessRe.getAverage());
		Single m_S_av = new Single("CREDIT_NETWORK_MES_SUCC_AV", this.transactionMessSucc.getAverage());
		Single m_F_av = new Single("CREDIT_NETWORK_MES_FAIL_AV", this.transactionMessFail.getAverage());
		
		Single p_av = new Single("CREDIT_NETWORK_PATH_AV", this.pathL.getAverage());
		Single p_Re_av = new Single("CREDIT_NETWORK_PATH_RE_AV", this.pathLRe.getAverage());
		Single p_S_av = new Single("CREDIT_NETWORK_PATH_SUCC_AV", this.pathLSucc.getAverage());
		Single p_F_av = new Single("CREDIT_NETWORK_PATH_FAIL_AV", this.pathLFail.getAverage());
		
		Single reL_av = new Single("CREDIT_NETWORK_REC_LAND_MES_AV", this.reLandMes.getAverage());
		Single ls_av = new Single("CREDIT_NETWORK_LAND_SRC_MES_AV", this.landSenMes.getAverage());
		
		Single pP_av = new Single("CREDIT_NETWORK_PATH_SINGLE_AV", this.path_single.getAverage());
		Single pPF_av = new Single("CREDIT_NETWORK_PATH_SINGLE_FOUND_AV", this.path_singleFound.getAverage());
		Single pPNF_av = new Single("CREDIT_NETWORK_PATH_SINGLE_NF_AV", this.path_singleNF.getAverage());
		
		Single s_av = new Single("CREDIT_NETWORK_STAB_AV", this.stab_av);
		Single rt = new Single("CREDIT_NETWORK_ROOT_TRAF_AV", this.passRootAll);
		Single s1 = new Single("CREDIT_NETWORK_SUCCESS_DIRECT", this.success_first);
		Single s = new Single("CREDIT_NETWORK_SUCCESS", this.success);
		
		Single d1 = new Single("CREDIT_NETWORK_DELAY_AV", this.delay.getAverage());
		Single d2 = new Single("CREDIT_NETWORK_DELAY_SUCC_AV", this.delaySucc.getAverage());
		Single d3 = new Single("CREDIT_NETWORK_DELAY_FAIL_AV", this.delayFail.getAverage());
		
		return new Single[]{m_av, m_Re_av, m_S_av, m_F_av,p_av, p_Re_av, p_S_av, p_F_av,
				reL_av, ls_av, s_av, s1, s, pP_av, pPF_av, pPNF_av, rt,d1,d2,d3};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("CREDIT_LINKS");
	}
	
	private Vector<Transaction> readList(String file){
		Vector<Transaction> vec = new Vector<Transaction>();
		try{ 
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    int count = 0;
		    while ((line =br.readLine()) != null){
		    	String[] parts = line.split(" ");
		    	if (parts.length == 4){
		    		Transaction ta = new Transaction(Double.parseDouble(parts[0]),
		    				Double.parseDouble(parts[1]),
		    				Integer.parseInt(parts[2]),
		    				Integer.parseInt(parts[3]));
		    		vec.add(ta);
		    	}
		    	if (parts.length == 3){
		    		Transaction ta = new Transaction(count, 
		    				Double.parseDouble(parts[0]),
		    				Integer.parseInt(parts[1]),
		    				Integer.parseInt(parts[2]));
		    		vec.add(ta);
		    		count++;
		    	} 
		    }
		    br.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		 return vec;
	}
	
	private LinkedList<double[]> readLinks(String file){
		LinkedList<double[]> vec = new LinkedList<double[]>();
		try{ 
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    while ((line =br.readLine()) != null){
		    	String[] parts = line.split(" ");
		    	if (parts.length == 4){
		    		double[] link = new double[4];
		    		for (int i = 0; i < parts.length; i++){
		    			link[i] = Double.parseDouble(parts[i]);
		    		}
		    		vec.add(link);
		    	}
		    	
		    }
		    br.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		 return vec;
	}
	

	
	private long[] inc(long[] values, int index) {
		try {
			values[index]++;
			return values;
		} catch (ArrayIndexOutOfBoundsException e) {
			long[] valuesNew = new long[index + 1];
			System.arraycopy(values, 0, valuesNew, 0, values.length);
			valuesNew[index] = 1;
			return valuesNew;
		}
	}
	
	private void setRoots(int[][] paths){
		for (int i = 0; i < paths.length; i++){
			if (paths[i] == null) continue;
			this.rootPath++;
			for (int j = 0; j < paths[i].length; j++){
				if (paths[i][j] == this.roots[i]){
					this.passRootAll++;
					this.passRoot[i]++;
				}
			}
		}
		
	}
	


}
