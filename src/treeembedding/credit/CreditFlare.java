package treeembedding.credit;

import gtna.data.Single;
import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.DataWriter;
import gtna.io.graphWriter.GtnaGraphWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.Config;
import gtna.util.Distribution;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;

import treeembedding.credit.partioner.Partitioner;
import treeembedding.treerouting.Treeroute;

public class CreditFlare extends Metric {
	//input parameters
	Vector<Transaction> transactions; //vector of transactions, sorted by time 
	Partitioner part; //method to partition overall transaction value on paths
	int k; //max routes considered
	int ntab; //max intermediate nodes considered
	int rn; //diameter of neighborhood information stored in rt
	int beacs; //number of beacon nodes
	double epoch; //time for stabilization costs 
	Queue<double[]> newLinks;
	boolean update;
	HashMap<Edge, Double> originalWeight;
	
	Vector<Edge> zeroEdges;
	Graph graph;
	int[][][] beacons;
	BigInteger[] ids;
	
	
	//computed metrics
	double[] stab; //stabilization overhead over time (in #messages)
	double stab_av; //average stab overhead
	Distribution transactionMess; //distribution of #messages needed for one transaction trial 
	                             //(i.e., each retransaction count as new transactions)
	Distribution transactionMessSucc; //messages successful transactions
	Distribution transactionMessFail; //messages failed transactions 
	Distribution pathL; //distribution of path length (sum over all trees!)
	Distribution pathLSucc; //path length successful transactions
	Distribution pathLFail; //path length failed transactions 
	Distribution path_single; //distribution of single paths
	Distribution path_singleFound; //distribution of single paths, only discovered paths 
	Distribution path_singleNF; //distribution of single paths, not found dest
	Distribution delay; //distribution of hop delay
	Distribution delaySucc; //distribution of hop delay
	Distribution delayFail; //distribution of hop delay
	double success_first; //fraction of transactions successful in first try
	double success; // fraction of transactions successful at all
	int initCost;
	
	boolean log = true;
	Random rand;
	
	
	public CreditFlare(String file, String name, int rn, int k, int nb, int ntab, 
			double epoch, Partitioner part, String links, boolean up){
		super("CREDIT_FLARE", new Parameter[]{new StringParameter("NAME", name), new IntParameter("REACH", rn),
				new IntParameter("ROUTES", k), new IntParameter("BEACONS", nb), new IntParameter("MAXNODES", ntab),
				new StringParameter("PARTITIONER", part.getName())});
		transactions = this.readList(file);
        this.part = part; 
        this.k = k;
        this.rn = rn;
        this.beacs = nb;
        this.ntab = ntab;
        this.epoch = epoch;
		if (links != null){
			this.newLinks = this.readLinks(links);
		} else {
			this.newLinks = new LinkedList<double[]>();
		}
		this.update = up;
	}
	
	public CreditFlare(String file, String name, int rn, int k, int nb, int ntab, double epoch, Partitioner part, boolean up){
		this(file, name,rn,k,nb,ntab,epoch,part,null,up);
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
        //init: construct trees (currently randomly) and init variables
		rand = new Random();
		this.initCost = init(g,n,m);
		
		int count = 0;
		long[] path = new long[2];
		long[] pathSucc = new long[2];
		long[] pathFail = new long[2];
		long[] mes = new long[2];
		long[] mesSucc = new long[2];
		long[] mesFail = new long[2];
		long[] pathS = new long[2];
		long[] pathSF = new long[2];
		long[] pathSNF = new long[2];
		int[] cAllPath = new int[2];
		long[] del = new long[2];
		long[] delSucc = new long[2];
		long[] delFail = new long[2];
		success_first = 0;
		success = 0;
		Vector<Integer> stabMes = new Vector<Integer>();
		Node[] nodes = g.getNodes();
		boolean[] exclude = new boolean[nodes.length]; 
		
		//go over transactions
		Queue<Transaction> toRetry = new LinkedList<Transaction>(); 
		int epoch_old = 0;
		int epoch_cur = 0;
		int cur_stab = 0;
		int c = 0;
		
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
					this.addLink((int)link[1], (int)link[2], link[3], g);
					nt = this.newLinks.isEmpty()?Double.MAX_VALUE:this.newLinks.peek()[0];
				}
			}
			if (epoch_cur != epoch_old){
				stabMes.add(cur_stab);
					for (int j = epoch_old +2; j <= epoch_cur; j++){
						stabMes.add(0);
					}
					cur_stab = 0;
			}
			
			count++;
			//if (log){
				System.out.println("Perform transaction s="+ cur.src + " d= "+ cur.dst + 
						" val= " + cur.val + " time= "+ cur.time + " " + (new Date()));
			//}
				
			
 
			
			//2: execute the transaction
			originalWeight = new HashMap<Edge, Double>();	
			int[] results = this.route(cur, g, nodes, exclude);
			//re-queue if necessary
			cur.addPath(results[1]);
			cur.addMes(results[4]);
			//reset to old weights if failed
			if (results[0] == -1){
				this.weightUpdate(edgeweights, originalWeight);
				this.zeroEdges = new Vector<Edge>();
			}
		
			
			//3 update metrics accordingly
			path = this.inc(path, results[1]);
			mes = this.inc(mes, results[4]);
			del = this.inc(del, results[5]);
			if (results[0] == 0){
				this.success++;
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
				cAllPath[index]++;
				int val = Math.abs(results[j]);
				pathS = this.inc(pathS, val);
				if (index == 0){
					pathSF = this.inc(pathSF, val);
				} else {
					pathSNF = this.inc(pathSNF, val);
				}
			}
			
			//4 post-processing: remove edges set to 0, update spanning tree if dynRapir
			epoch_old = epoch_cur;
			if (zeroEdges != null){
				//inform affected nodes 
				Iterator<Edge> it = originalWeight.keySet().iterator();
				while (it.hasNext()){
					Edge e = it.next();
					HashSet<Edge> eds = this.allEdgesInNeigh(nodes, e.getSrc(), this.rn, edgeweights);
					eds.addAll(this.allEdgesInNeigh(nodes, e.getDst(), this.rn, edgeweights));
					int neighs = eds.size();
					cur_stab = cur_stab + neighs;
				}
				if (!this.update){
					this.weightUpdate(edgeweights, originalWeight);
				}
			}
		}
		stabMes.add(cur_stab);
		
		
        //compute metrics
		this.pathL = new Distribution(path,count);
		this.transactionMess = new Distribution(mes, count);
		this.pathLSucc = new Distribution(pathSucc,(int)this.success);
		this.transactionMessSucc = new Distribution(mesSucc, (int)this.success);
		this.pathLFail = new Distribution(pathFail,this.transactions.size()-(int)this.success);
		this.transactionMessFail = new Distribution(mesFail, this.transactions.size()-(int)this.success);
		this.path_single = new Distribution(pathS, cAllPath[0] + cAllPath[1]);
		this.path_singleFound = new Distribution(pathSF, cAllPath[0]);
		this.path_singleNF = new Distribution(pathSNF, cAllPath[1]);
		this.delay = new Distribution(del, count);
		this.delaySucc = new Distribution(delSucc, (int)this.success);
		this.delayFail = new Distribution(delFail,count-(int)this.success);
		this.success = this.success/(double)transactions.size();
		this.success_first = this.success_first/(double)transactions.size();
		stab = new double[stabMes.size()];
		this.stab_av = 0;
		for (int i = 0; i < this.stab.length; i++){
			stab[i] = stabMes.get(i);
			this.stab_av = this.stab_av + stab[i];
		}
		this.stab_av = this.stab_av/(double)stab.length;
		this.graph = g;
		
	}
	
	private int init(Graph g, Network n, HashMap<String, Metric> m) {
		int cost = 0;
		//step 1: compute local routing table cost = edges in rn neighborhood fro all nodes
		Node[] nodes = g.getNodes();
		CreditLinks edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		for (int i = 0; i < nodes.length; i++){
			cost = cost + this.allEdgesInNeigh(nodes, i, this.rn, edgeweights).size();
		}
		
		//step 2: init beacons
		this.ids = new BigInteger[nodes.length];
		for (int i = 0; i < nodes.length; i++){
			this.ids[i] = new BigInteger(128,rand);
		}
		this.beacons = new int[nodes.length][this.beacs][];
		BigInteger[][] ds = new BigInteger[nodes.length][this.beacs];
		//2a: init with people from neighborhood
		for (int i = 0; i < nodes.length; i++){
			Vector<int[]> rt = this.generateRT(nodes, i, edgeweights); 
			for (int j = 0; j < rt.size(); j++){
				int[] path = rt.get(j);
				if (path[path.length-1] == i) continue;
				BigInteger dist = this.ids[i].xor(this.ids[path[path.length-1]]);
				for (int l = 0; l < this.beacs; l++){
					if (this.beacons[i][l] == null){
						this.beacons[i][l] = path;
						ds[i][l] = dist;
						break;
					}
					if (ds[i][l].compareTo(dist) > 0){
						for (int c = this.beacs-1; c > l; c--){
							this.beacons[i][c] = this.beacons[i][c-1];
							ds[i][c] = ds[i][c-1];
						}
						this.beacons[i][l] = path;
						ds[i][l] = dist;
						break;
					}
				}
			}
		}
		//2b: update via cur
		boolean done = false;
		boolean[] stable = new boolean[nodes.length];
		boolean[] touched = new boolean[nodes.length]; 
		while (!done){
			done = true;
			for (int i = 0; i < nodes.length; i++){
				//iterate over beacons to get new candidate beacons
				Vector<int[]> pot = new Vector<int[]>();
				HashSet<Integer> con = new HashSet<Integer>();
				con.add(i);
				for (int j = 0; j < this.beacs; j++){
					if (this.beacons[i][j] != null){
						int[] path = this.beacons[i][j];
						con.add(path[path.length-1]);
					}
				}	
				for (int j = 0; j < this.beacs; j++){
					if (this.beacons[i][j] != null){
						int cur = this.beacons[i][j][this.beacons[i][j].length-1];
						if (stable[cur]) continue; 
						cost = cost + this.beacons[i][j].length-1;
						Vector<int[]> rt = this.generateRT(nodes, i, edgeweights);
						for (int l = 0; l < rt.size(); l++){
							int[] pt = rt.get(l);
							int cand = pt[pt.length-1];
                              BigInteger d = this.ids[i].xor(ids[cand]);
                            if (!con.contains(cand) && d.compareTo(ds[i][j]) < 0){
                            	con.add(cand);
                            	int[] concat = new int[pt.length+this.beacons[i][j].length-1];
                            	for (int c = 0; c < this.beacons[i][j].length; c++){
                            		concat[c] = this.beacons[i][j][c];
                            	}
                            	for (int c = this.beacons[i][j].length; c < pt.length+this.beacons[i][j].length-1; c++){
                            		concat[c] = pt[c-this.beacons[i][j].length+1];
                            	}
                            	pot.add(concat);
                            }
						}		
					}
				}
				//insert in beacon list 
				if (pot.size() > 0) {
					touched[i] = true;
				} 
				for (int j = 0; j < pot.size(); j++){
					int[] pt = pot.get(j);
					int cand = pt[pt.length-1];
                    BigInteger dist = this.ids[i].xor(ids[cand]);
                    for (int l = 0; l < this.beacs; l++){
    					if (this.beacons[i][l] == null){
    						this.beacons[i][l] = pt;
    						ds[i][l] = dist;
    						break;
    					}
    					if (ds[i][l].compareTo(dist) > 0){
    						for (int c = this.beacs-1; c > l; c--){
    							this.beacons[i][c] = this.beacons[i][c-1];
    							ds[i][c] = ds[i][c-1];
    						}
    						this.beacons[i][l] = pt;
    						ds[i][l] = dist;
    						break;
    					}
    				}
				}
				for (int l = 0; l < this.beacs; l++){
					if (this.beacons[i][l] != null){
						int neigh = this.beacons[i][l][this.beacons[i][l].length-1];
						if (pot.contains(neigh)){
							cost = cost + this.beacons[i][l].length-1;
							touched[neigh] = true; 
						}
					}
					
				}
				for (int h = 0; h < stable.length; h++){
					stable[h] = !touched[h];
				}
			}
		}
		
		return cost;
	}
	
	private Vector<int[]> generateRT(Node[] nodes, int i, CreditLinks edgeweights){
		Vector<int[]> rt = new Vector<int[]>();
		HashSet<Integer> contained = new HashSet<Integer>();
		for (int j = 0; j < this.beacs; j++){
			if (this.beacons[i][j] != null){
				rt.add(this.beacons[i][j]);
				contained.add(this.beacons[i][j][this.beacons[i][j].length-1]);
			}
		}
		
		LinkedList<Integer> q = new LinkedList<Integer>();
		HashMap<Integer, Integer> pred = new HashMap<Integer, Integer>();
		pred.put(i, -1);
		q.add(i);
		for (int j = 0; j < this.rn; j++){
			LinkedList<Integer> next = new LinkedList<Integer>();
			while (!q.isEmpty()){
				int node = q.poll();
				int[] out = nodes[node].getOutgoingEdges();
				for (int p: out){
					if (!pred.containsKey(p) && edgeweights.getPot(node, p)>0){
						pred.put(p, node);
						next.add(p);
						if (!contained.contains(p)){
							contained.add(p);
							rt.add(this.path(pred, p));
						}
					}
				}
			}
			q = next;
		}	
		return rt;
	}
	
	private int[] path(HashMap<Integer, Integer> pred, Integer cur){
		Vector<Integer> pathR = new Vector<Integer>();
		while (cur != -1){
			pathR.add(cur);
			cur = pred.get(cur);
		}
		int[] path = new int[pathR.size()];
		for (int i = pathR.size()-1; i > -1; i--){
			path[pathR.size()-1-i] = pathR.get(i);
		}
		return path;
	}
	
	private HashSet<Edge> allEdgesInNeigh(Node[] nodes, int centre, int r, CreditLinks edgeweights){
		HashSet<Edge> neighEdges = new HashSet<Edge>(); 
		Queue<Integer> q = new LinkedList<Integer>();
		q.add(centre);
		for (int j =0; j < r; j++){
			Queue<Integer> next = new LinkedList<Integer>();
			while (!q.isEmpty()){
				int node = q.poll();
				int[] peers = nodes[node].getOutgoingEdges();
				for (int p: peers){
					if (!neighEdges.contains(new Edge(node,p)) && edgeweights.getPot(node, p) > 0){
						neighEdges.add(new Edge(node,p));
						if (!next.contains(p)){
							next.add(p);
						}
					}
				}
			}
			q = next;
		}
		return neighEdges;
	}

	
	
	private int addLink(int src, int dst, double weight, Graph g){
		CreditLinks edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		if (log) System.out.println("Added link " + src + " " + dst + " " + weight);
		double[] ws = new double[3];
		if (src < dst){
			ws = edgeweights.getWeights(src, dst);
			ws[2] = weight;
		} else {
			ws = edgeweights.getWeights(dst,src);
			ws[0] = -weight;
		}
		
		//compute cost of update
		Node[] nodes = g.getNodes();
		HashSet<Edge> neighEdges = this.allEdgesInNeigh(nodes, src, this.rn, edgeweights);
		neighEdges.addAll(this.allEdgesInNeigh(nodes, dst, this.rn, edgeweights));
		return neighEdges.size();
	}
	
	/**
	 * find routes by iterative considering closest nodes to receiver
	 * @return {success?0:-1, sum(pathlength), receiver-landmark, landmarks-sender,overall message count, lengthP1,...}
	 */
	private int[] route(Transaction cur, Graph g, Node[] nodes, boolean[] exclude){
		int rec = cur.dst;
		CreditLinks edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		Vector<int[]> rtc = this.generateRT(nodes, cur.src, edgeweights);
		int seen = 0;
		Vector<int[]> paths = new Vector<int[]>();
		System.out.println("routing before loop");
		while (seen < this.ntab && paths.size() < this.k){
			//find all path in rtc
			seen++;
			Vector<int[]> curpaths = new Vector<int[]>();
			for (int j = 0; j < rtc.size(); j++){
				int[] p = rtc.get(j);
				if (p[p.length-1] == rec){
					curpaths.add(p);
				}
			}
			while (curpaths.size() + paths.size() > this.k){
				int max = 0;
				Vector<Integer> maxs = new Vector<Integer>();
				for (int i = 0; i < curpaths.size(); i++){
					int[] p = curpaths.get(i);
					if (p.length >= max){
						if (p.length > max){
							max = p.length;
							maxs = new Vector<Integer>();
						}
						maxs.add(i);
					}
				}
				curpaths.remove(maxs.get(rand.nextInt(maxs.size())));
			}
			paths.addAll(curpaths);
			if (paths.size() == k){
					break;
			}
			
			//get next routing table
			BigInteger min = (BigInteger.ONE.add(BigInteger.ONE)).pow(128);
			int[] minP = {};
			BigInteger id = ids[rec];
			for (int j = 0; j < rtc.size(); j++){
				int[] p = rtc.get(j);
				int node = p[p.length-1]; 
				if (node != rec){
					BigInteger d = id.xor(ids[node]);
					if (d.compareTo(min) < 0){
						min = d;
						minP = p;
					}
				}
			}
			if (minP.length == 0) break;
			Vector<int[]> rtcD = this.generateRT(nodes, minP[minP.length-1], edgeweights);
			rtc = new Vector<int[]>(rtcD.size());
			for (int j = 0; j < rtcD.size(); j++){
				int[] p = rtcD.get(j);
				if (p[p.length-1] == cur.src) continue;
				int[] full = new int[p.length+minP.length-1];
				for (int i = 0; i < minP.length; i++){
					full[i] = minP[i];
				}
				for (int i = minP.length; i < minP.length+p.length-1; i++){
					full[i] = p[i-minP.length+1];
				}
				rtc.add(full);
			}
		}
		
		int[] res = new int[6+paths.size()];
		//figure out if successful
		double allVal = 0;
		double[] mins = new double[paths.size()];
		for (int j = 0; j < paths.size(); j++){
			int[] p = paths.get(j);
			mins[j] = Double.MAX_VALUE;
			for (int i = 1; i < p.length; i++){
				
				double w = edgeweights.getPot(p[i-1], p[i]);
				if (w < mins[j]){
					mins[j] = w;
				}
			}
			res[6+j] = p.length-1; 
			res[1] = res[1] + p.length-1;
			if (p.length-1 > res[5]){
				res[5] = p.length-1;
			}
			allVal = allVal + mins[j];
		}
		
		if (allVal < cur.val){
			res[0] = -1;
			res[4] = 2*res[1];
			res[5] = 2*res[5];
		} else {
			boolean succ = true;
			//partition transaction value
			double[] vals = part.partition(g, cur.src,cur.dst,cur.val,mins);
			for (int j = 0; j < paths.size(); j++) {
				    int[] p = paths.get(j);
					int l = p[0];
					for (int i = 1; i < p.length; i++) {
						int k = p[i];
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
			if (!succ){
				res[0] = -1;
			}
			res[4] = 4*res[1];
			res[5] = 4*res[5];
		}
		
		
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
		succ &= DataWriter.writeWithIndex(this.transactionMessSucc.getDistribution(),
				this.key+"_MESSAGES_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.transactionMessFail.getDistribution(),
				this.key+"_MESSAGES_FAIL", folder);
		
		succ &= DataWriter.writeWithIndex(this.pathL.getDistribution(),
				this.key+"_PATH_LENGTH", folder);
		succ &= DataWriter.writeWithIndex(this.pathLSucc.getDistribution(),
				this.key+"_PATH_LENGTH_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.pathLFail.getDistribution(),
				this.key+"_PATH_LENGTH_FAIL", folder);
		

		succ &= DataWriter.writeWithIndex(this.stab,
				this.key+"_STABILIZATION", folder);
		succ &= DataWriter.writeWithIndex(this.delay.getDistribution(),
				this.key+"_DELAY", folder);
		succ &= DataWriter.writeWithIndex(this.delaySucc.getDistribution(),
				this.key+"_DELAY_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.delayFail.getDistribution(),
				this.key+"_DELAY_FAIL", folder);
		
		succ &= DataWriter.writeWithIndex(this.path_single.getDistribution(),
				this.key+"_PATH_SINGLE", folder);
		succ &= DataWriter.writeWithIndex(this.path_singleFound.getDistribution(),
				this.key+"_PATH_SINGLE_FOUND", folder);
		succ &= DataWriter.writeWithIndex(this.path_singleNF.getDistribution(),
				this.key+"_PATH_SINGLE_NF", folder);
		
        if (Config.getBoolean("SERIES_GRAPH_WRITE")) {
			(new GtnaGraphWriter()).writeWithProperties(graph, folder+"graph.txt");
		}
		
		
		return succ;
	}

	@Override
	public Single[] getSingles() {
		Single m_av = new Single("CREDIT_FLARE_MES_AV", this.transactionMess.getAverage());
		Single m_S_av = new Single("CREDIT_FLARE_MES_SUCC_AV", this.transactionMessSucc.getAverage());
		Single m_F_av = new Single("CREDIT_FLARE_MES_FAIL_AV", this.transactionMessFail.getAverage());
		
		Single p_av = new Single("CREDIT_FLARE_PATH_AV", this.pathL.getAverage());
		Single p_S_av = new Single("CREDIT_FLARE_PATH_SUCC_AV", this.pathLSucc.getAverage());
		Single p_F_av = new Single("CREDIT_FLARE_PATH_FAIL_AV", this.pathLFail.getAverage());
		
		
		Single pP_av = new Single("CREDIT_FLARE_PATH_SINGLE_AV", this.path_single.getAverage());
		Single pPF_av = new Single("CREDIT_FLARE_PATH_SINGLE_FOUND_AV", this.path_singleFound.getAverage());
		Single pPNF_av = new Single("CREDIT_FLARE_PATH_SINGLE_NF_AV", this.path_singleNF.getAverage());
		
		Single d1 = new Single("CREDIT_FLARE_DELAY_AV", this.delay.getAverage());
		Single d2 = new Single("CREDIT_FLARE_DELAY_SUCC_AV", this.delaySucc.getAverage());
		Single d3 = new Single("CREDIT_FLARE_DELAY_FAIL_AV", this.delayFail.getAverage());
		
		Single s_av = new Single("CREDIT_FLARE_STAB_AV", this.stab_av);
		Single s = new Single("CREDIT_FLARE_SUCCESS", this.success);
		
		Single init = new Single("CREDIT_FLARE_INIT", this.initCost);
		
		return new Single[]{m_av, m_S_av, m_F_av,p_av, p_S_av, p_F_av,
				s_av, s, pP_av, pPF_av, pPNF_av};
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
	

	


}
