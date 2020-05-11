package treeembedding.stepwise;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.transformation.churn.Test;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;

public class BalancedCA extends SpanningTreeDynamic {
	boolean virtual;
	boolean simple_join;
	int c;
	double est;
	double[][] lastId;
	int initIDs;
	int initMes;
	HashMap<Integer, Integer> nest;
	Vector<Integer> newIDs;
	Vector<Integer> messages;
	Vector<Integer> newIDsTot;
	Vector<Integer> messagesTot;
	Vector<Double> newIDs_frac;
	Vector<Double> mess_frac;
	Vector<Double> maxOverhead;
	Vector<Double> boundOver;
	Vector<Double> jain;
	double[][] frac_ids;
	int[] level_virtual;

	public BalancedCA(String name, String folder, String s, boolean random,
			double minus, int it, int granularity, int seed, boolean virtual, boolean simple_join, int c, double g) {
		super("BALANCED_CA", new Parameter[]{new BooleanParameter("VIRTUAL", virtual), new BooleanParameter("SIMPLE_JOIN", simple_join),
				new IntParameter("C", c), new DoubleParameter("G", g)}
		        ,name, folder, s, random, minus, it, granularity, seed);
		this.virtual = virtual;
		this.simple_join = simple_join;
		this.est = g;
		this.c = c;
	}
	
	public BalancedCA(String name, String folder, String s, boolean random,
			double minus, int it, int granularity, int seed, String m, boolean virtual, boolean simple_join, int c, double g) {
		super("BALANCED_CA", new Parameter[]{new BooleanParameter("VIRTUAL", virtual), new BooleanParameter("SIMPLE_JOIN", simple_join),
				new IntParameter("C", c), new DoubleParameter("G", g)}
		        ,name, folder, s, random, minus, it, granularity, seed,m);
		this.virtual = virtual;
		this.simple_join = simple_join;
		this.est = g;
		this.c = c;
	}
	
	public BalancedCA(String name, String folder, String s, boolean random,
			double minus, int it, int granularity, boolean virtual, boolean simple_join, int c, double g) {
		super("BALANCED_CA", new Parameter[]{new BooleanParameter("VIRTUAL", virtual), new BooleanParameter("SIMPLE_JOIN", simple_join),
				new IntParameter("C", c), new DoubleParameter("G", g)}
		        ,name, folder, s, random, minus, it, granularity, Test.seeds[Test.ind]);
		this.virtual = virtual;
		this.simple_join = simple_join;
		this.est = g;
		this.c = c;
	}
	
	public BalancedCA(String name, String folder, String s, boolean random,
			double minus, int it, int granularity, String m, boolean virtual, boolean simple_join, int c, double g) {
		super("BALANCED_CA", new Parameter[]{new BooleanParameter("VIRTUAL", virtual), new BooleanParameter("SIMPLE_JOIN", simple_join),
				new IntParameter("C", c), new DoubleParameter("G", g)}
		        ,name, folder, s, random, minus, it, granularity, (new Random()).nextInt(),m);
		this.virtual = virtual;
		this.simple_join = simple_join;
		this.est = g;
		this.c = c;
	}
	
	public BalancedCA(String name, String sess, String inter, String s,  int it, int granularity, boolean virtual, boolean simple_join, int c, double g) {
		super("BALANCED_CA", new Parameter[]{new BooleanParameter("VIRTUAL", virtual), new BooleanParameter("SIMPLE_JOIN", simple_join),
				new IntParameter("C", c), new DoubleParameter("G", g)}
		        ,name, sess, inter, s, it, granularity, Test.seeds[Test.ind]);
		this.virtual = virtual;
		this.simple_join = simple_join;
		this.est = g;
		this.c = c;
	}
	
	public BalancedCA(String name, String sess, String inter, String s,  int it, int granularity, int seed, boolean virtual, boolean simple_join, int c, double g) {
		super("BALANCED_CA", new Parameter[]{new BooleanParameter("VIRTUAL", virtual), new BooleanParameter("SIMPLE_JOIN", simple_join),
				new IntParameter("C", c), new DoubleParameter("G", g)}
		        ,name, sess, inter, s, it, granularity, seed);
		this.virtual = virtual;
		this.simple_join = simple_join;
		this.est = g;
		this.c = c;
	}
	
	
	
	@Override
	public void initGraph(Graph g, Network n, HashMap<String, Metric> m) {
		super.initGraph(g, n, m);
		this.initIDs=0;
		this.initMes=0; 
		nest = new HashMap<Integer,Integer>();
		this.frac_ids = new double[g.getNodeCount()][2];
		this.lastId = new double[g.getNodeCount()][];
		if (this.virtual){
			this.level_virtual = new int[g.getNodeCount()];
		}
		for (int j = 0; j < this.newRoots.size(); j++){
			lastId[this.newRoots.get(j)]=new double[0];
			int val = 0;
			if (this.virtual){
				val = this.embedVirtual(this.newRoots.get(j),1);
			} else {
			   val = this.embed(this.newRoots.get(j),1);
			}
			this.initIDs = this.initIDs+val;
			this.initMes = this.initMes+val-1;
			nest.put(this.newRoots.get(j), val);
		}
		messages = new Vector<Integer>();
		newIDs = new Vector<Integer>();
		messagesTot = new Vector<Integer>();
		newIDsTot = new Vector<Integer>();
		newIDs_frac = new Vector<Double>();
		mess_frac = new Vector<Double>();
		maxOverhead = new Vector<Double>();
		boundOver = new Vector<Double>();
		jain = new Vector<Double>();
		this.computeBalance();
	}
	
	private int embed(int subroot, double contT){
		Queue<Integer> q = new LinkedList<Integer>();
		q.add(subroot);
		this.frac_ids[subroot][1] = contT;
		int count = 0;
		while (!q.isEmpty()){
			count++;
			int cur = q.poll();
			double old = 0;
			for (int child: children[cur]){
				double next = old + this.subtree[child]/(double)this.subtree[cur];
				this.lastId[child]=new double[]{old,next};
				this.frac_ids[child][1]=this.frac_ids[cur][1]*(next-old);
				old = next;
				q.add(child);
			}
			this.frac_ids[cur][0] = this.frac_ids[cur][1]*(1-old);
		}
		return count;
	}
	
	private int embedVirtual(int subroot, double contT){
		this.frac_ids[subroot][0] = 1/(double)subtree[subroot]*contT;
		this.frac_ids[subroot][1] = contT;
		for (int i = 0; i < children[subroot].length; i++){
			int cur = this.children[subroot][i];
			this.frac_ids[cur][1] = (contT - this.frac_ids[subroot][0])/(double)this.children[subroot].length;
			this.lastId[children[subroot][i]] = new double[2];
		}
		return this.embedVirtual(subroot, contT, 0, this.children[subroot].length-1)+1;
	}
	
	private int embedVirtual(int subroot, double contT, int first, int last){
		//System.out.println("embedVirt " + subroot + " " + contT + " " + first + " " + last + " nodeC " + this.subtree[subroot]);
		if (this.subtree[subroot] == 1) return 0;
		Queue<Integer> q = new LinkedList<Integer>();
		//embed only subset of children
		int sumNodes = 0;
		double sumC = 0;
		for (int i = first; i <= last; i++){
			int cur = children[subroot][i];
			sumNodes = sumNodes + this.subtree[cur];
			sumC = sumC + this.frac_ids[cur][1];
			//System.out.println("old frac " + i + " " + this.frac_ids[cur][1]);
		}
		sumC = sumC/contT;
		//System.out.println("after children " + sumNodes + " " + sumC);
		double st = this.lastId[children[subroot][first]][0];
		for (int i = first; i <= last; i++){
			int cur = children[subroot][i];
			double up = st + this.subtree[cur]/(double)sumNodes*sumC;
			this.lastId[cur][0] = st;
			this.lastId[cur][1] = up;
			this.frac_ids[cur][1] = contT*(up-st);
			//System.out.println("frac " + i + " " + this.frac_ids[cur][1]);
			st = up;
			q.add(cur);
		}
		int count = 0;
		while (!q.isEmpty()){
			count++;
			int cur = q.poll();
			double old = 0;
			for (int child: children[cur]){
				double next = old + this.subtree[child]/(double)this.subtree[cur];
				this.lastId[child]=new double[]{old,next};
				this.frac_ids[child][1]=this.frac_ids[cur][1]*(next-old);
				old = next;
				q.add(child);
			}
			this.frac_ids[cur][0] = this.frac_ids[cur][1]*(1-old);
			//System.out.println("frac " + cur + " " + this.frac_ids[cur][0]*this.subtree[subroot] + " " + this.frac_ids[cur][1]);
			if (children[cur].length > 1){
			 int log = (int)Math.floor(Math.log(this.children[cur].length)/Math.log(2));
			 int twox = (int)Math.pow(2,log);
			 int rem = children[cur].length-twox;
			 for (int i = 0; i < 2*rem; i++){
				this.level_virtual[this.children[cur][i]] = this.level_virtual[cur] + log +1;
			 }
			 for (int i = 2*rem; i < this.children[cur].length; i++){
					this.level_virtual[this.children[cur][i]] = this.level_virtual[cur] + log;
				 }
			} else {
				if (children[cur].length == 1){
					this.level_virtual[this.children[cur][0]] = this.level_virtual[cur] +1;
				}	 
			}
		}
		return count;
	}
	
	@Override
	public void processJoin(Graph g, Network n, HashMap<String, Metric> m,
			int node) {
		super.processJoin(g, n, m, node);
		//case 1: new node is root -> re-embed
		//case 2: new estimate is required -> re-embed
		//case 3: !1&&!2 && simple_join=false -> re-embed at parent
		//case 4: !1&&!2 && simple_join=true -> assign node coordinate, potentially to merged-in children
		int r = this.tree[node];
		Integer cur_est = this.nest.get(r);
		int ids, mes, totIds,totMes;
		if (this.tree[node] == node || cur_est > subtree[r]*est || cur_est < subtree[r]/est){
			lastId[r]=new double[0];
			int val;
			if (this.virtual){
			    this.level_virtual[r] = 0;
				val = this.embedVirtual(r,1);
			} else {
				val = this.embed(r,1);
			}
			ids = val;
			mes = val-1+this.parent[node][1];
			nest.put(r, val);
		} else {
			int par = this.parent[node][0];
			int[] ph = this.embedJoinedNode(node, par);
			ids = ph[0];
			mes = ph[1];
		}
		totIds = this.subtree[r];
		totMes = totIds-1+this.parent[node][1];
		this.enterVals(ids, mes, totIds, totMes);
		this.computeBalance();
	}
	
	private int[] embedJoinedNode(int node, int par){
		if (this.simple_join){
			//TO DO: virtual
			double max=0;
			for (int c: children[par]){
				if (c != node && this.lastId[c][1]>max){
					max = this.lastId[c][1];
				}
			}
			this.lastId[node]=new double[]{max,(1+max)/2};
			int val = this.embed(node,this.frac_ids[par][0]/2);
			this.frac_ids[par][0] = this.frac_ids[par][0]/2;
			return new int[] {val, val + this.parent[node][1]};
		} else {
			int val = 0;
			if (this.virtual){
				val = this.embedVirtual(par,this.frac_ids[par][1])-1;
            } else {
			    val = this.embed(par,this.frac_ids[par][1])-1;
			} 
			return new int[] {val, val + this.parent[par][1]};
		}
	}
	
	private void enterVals(int ids, int mes, int totIds, int totMes){
		this.newIDs.add(ids);
		this.messages.add(mes);
		this.newIDsTot.add(totIds);
		this.messagesTot.add(totMes);
		double f_id = (totIds==0)?1:(double)ids/totIds;
		this.newIDs_frac.add(f_id);
		double f_mes = (totMes==0)?1:(double)mes/totMes;
		this.mess_frac.add((double)f_mes);
	}
	
	@Override
	public void processLeave(Graph g, Network n, HashMap<String, Metric> m,
			int node) {
		int r = tree[node];
		int pa = this.parent[node][0];
		super.processLeave(g, n, m, node);
		this.children[node] = new int[0];
		this.subtree[node] = 0;
		//step one: embed new trees (either result from node being a root or from disconnecting components) 
		int ids = 0;
		int mes = 0;
		int totIds = 0;
		int totMes = 0;
		for (int j = 0; j < this.newRoots.size(); j++){
			int i = this.newRoots.get(j);
			lastId[i]=new double[0];
			int val = 0;
			if (this.virtual){
				this.level_virtual[i] = 0;
				val = this.embedVirtual(i, 1);
			} else {
			   val = this.embed(i,1);
			}
            ids = ids+val;
			mes = mes+val-1;
			nest.put(i, val);
			totIds = totIds+val;
			totMes = totMes+val-1;
			//System.out.println("Root " + this.newRoots.get(j));
		}
		
		//step two
		//case 1: re-embed due to estimate change
		//case 2: local re-embeds
		if(r != node){
			if (!this.nest.containsKey(r)){
				System.out.println("no val for " + r);
			}
			int cur_est = nest.get(r);
			int extra = 0;
			if (cur_est > subtree[r]*est || cur_est < subtree[r]/est || (!this.simple_join && r == pa)){
				int val;
				if (this.virtual){
				    this.level_virtual[r] = 0;
					val = this.embedVirtual(r,1);
				} else {
					val = this.embed(r,1);
				}
				ids = ids + val;
				mes = mes+val-1+this.parent[node][1];
				nest.put(r, val);
			} else {
				//re-embed descendants if not done before
			  for (int i = 0; i < this.changedChild.size(); i++){
				int[] pc = this.changedChild.get(i);
				if (tree[pc[1]] == r){
					int[] ph = this.embedJoinedNode(pc[0], pc[1]);
					ids = ids+ph[0];
					mes = mes+ph[1];
					extra = extra + this.parent[pc[1]][1];
				}
			  }
			//re-embed ancestor of departed node
				int val = this.localReemb(pa, r);
				int cur = pa;
				int child = node;
                while ( val == -1){
                	child = cur;
                	cur = this.parent[cur][0];
                	if (cur != r){
                	   val = this.localReemb(cur, r, child);
                	} else{
                		if (this.virtual){
                	       val = this.embedVirtual(r, 1);	
                		} else {
                			val = this.embed(r, 1);
                		}
                	}
                }
                ids = ids + val-1;
  			    mes = mes + val -1 + this.parent[cur][1];  
			  
			  
			  
			  
			  
			  
			  
			  
			  
//			    int cur = pa;
//				int child = node;
//				boolean fonly = false;
//				while (!fonly && cur != r){
//					int v = this.localReemb(cur, r, child);
//					if ()
////					double left = nest.get(r)*this.frac_ids[cur][1]/(double)subtree[cur];
////					if (left <= 1 + c + this.parent[cur][1]){
////						fonly = true;
////						int val = this.embed(cur,this.frac_ids[cur][1]);
////						ids = ids + val-1;
////						mes = mes + val -1 + this.parent[cur][1]; 
////					} else {
////						cur = this.parent[cur][0];
////						
////					}
//				}
			}
			totIds = totIds + this.subtree[r];
			totMes = totMes + this.subtree[r]-1+extra+this.parent[node][1];
		}
		this.enterVals(ids, mes, totIds, totMes);
		this.computeBalance();
	}
	
	private int localReemb(int cur, int r){
		double left = nest.get(r)*this.frac_ids[cur][1]/(double)subtree[cur];
		int level=this.virtual? this.level_virtual[cur]:this.parent[cur][1];
		if (left <= 1 + c + level){
			if (this.virtual){
				return this.embedVirtual(cur,this.frac_ids[cur][1]);
			} else {
			   return this.embed(cur,this.frac_ids[cur][1]);
			}
		}
		return -1;
	}
	
	private int localReemb(int cur, int r, int child){
		if (this.virtual){
			if (children[cur].length > 2){
				boolean[][] bits = new boolean[this.children[cur].length][];
			   int log = (int)Math.floor(Math.log(this.children[cur].length)/Math.log(2));
				 int twox = (int)Math.pow(2,log);
				 int rem = children[cur].length-twox;
				 bits[0] = new boolean[log+1];
			   for (int i = 1; i < 2*rem; i++){
				   boolean add = false;
				   int index = log;
				   bits[i] = new boolean[log+1];
				   while (index >= 0 && !add){					   
					   if (bits[i-1][index]){
						   bits[i][index] = false;
					   } else {
						   bits[i][index] = true;
						   add = true;
					   }
					   index--;
				   }
			   }
			   if (rem == 0){
				   bits[0] = new boolean[log]; 
			   }
			   for (int i= Math.max(2*rem,1); i < children[cur].length; i++){
			       bits[i] = new boolean[log];
			       boolean add = false;
				   int index = log-1;
				   while (index >= 0 && !add){					   
					   if (bits[i-1][index]){
						   bits[i][index] = false;
					   } else {
						   bits[i][index] = true;
						   add = true;
					   }
					   index--;
				   }
			   }
			   
			   double fs = this.frac_ids[child][1];
			   int nodC = this.subtree[child];
			   int childindex = -1;
			   for (int j = 0; j < children[cur].length; j++){
				   if (children[cur][j] == child){
					   childindex = j;
					   break;
				   }
			   }
			   HashSet<Integer> done = new HashSet<Integer>();
			   done.add(childindex);
			   int index = bits[childindex].length-1;
			   int min = childindex;
			   int max = childindex;
			   while (done.size() != children[cur].length){
				   for (int j = 0; j < children[cur].length; j++){
					   if (done.contains(j)) continue;
					   boolean match = true;
					   for (int i = 0; i < index; i++){
						   if (bits[j][i] != bits[childindex][i]){
							   match = false;
							   break;
						   }
					   }
					   if (match){
						   done.add(j);
						   int sib = this.children[cur][j];
						   nodC = nodC + this.subtree[sib];
						   fs = fs + this.frac_ids[sib][1];
						   if (j < min){
							   min = j;
						   }
						   if (j > max){
							   max = j;
						   }
					   }
				   }
				   double left = nest.get(r)*fs/(double)nodC;
				   if (left <= 1 + c + this.level_virtual[cur] + index){
					   return this.embedVirtual(cur,this.frac_ids[cur][1], min, max);
				   }
				   index--;
			   }
			}  
				double left = nest.get(r)*this.frac_ids[cur][1]/(double)subtree[cur];
				if (left <= 1 + c + this.level_virtual[cur]){
					return this.embedVirtual(cur,this.frac_ids[cur][1]);
				
				}
		} else {
			double left = nest.get(r)*this.frac_ids[cur][1]/(double)subtree[cur];
			if (left <= 1 + c + this.parent[cur][1]){
				return this.embed(cur,this.frac_ids[cur][1]);
			} 
		}
		return -1;
	}
	
	
	
	private void computeBalance(){
		int depth = 0;
		double f = 0;
		HashMap<Integer, Double> sums = new HashMap<Integer, Double>();
		for (int i = 0; i < this.frac_ids.length; i++){
			if (step.isOn(i)){
				if (this.virtual){
					if (this.level_virtual[i] > depth){
						depth = this.level_virtual[i];
					}
				} else {
			            if (this.parent[i][1] > depth){
				             depth = this.parent[i][1];
			             }
				}
			if (tree[i] == -1){
				System.out.println(i);
			}
			double fi = this.frac_ids[i][0]*this.subtree[tree[i]];
			if (fi > f){
				f = fi;
			}
			double sum = 0;
			if (sums.containsKey(tree[i])){
				sum = sums.get(tree[i]);
			}
			sum = sum + this.frac_ids[i][0];
			sums.put(tree[i], sum);
			if (sum >1.0001){
				System.out.println("Uhm " + sum);
				System.exit(0);
			}
			}
		}
		
		this.maxOverhead.add(f);
		this.boundOver.add(this.est*(depth+1+c));
		
		//fairness index
		HashMap<Integer,double[]> jains = new HashMap<Integer,double[]>();
		for (int i = 0; i < this.frac_ids.length; i++){
			if (step.isOn(i)){
			double[] terms = jains.get(tree[i]);
			if (terms == null){
				terms = new double[2];
				jains.put(tree[i], terms);
			}
			terms[0] = terms[0]+this.frac_ids[i][0];
			terms[1] = terms[1] + this.frac_ids[i][0]*this.frac_ids[i][0];
			}
		}
		Iterator<Entry<Integer,double[]>> it = jains.entrySet().iterator();
		double lowest =1;
		while (it.hasNext()){
			Entry<Integer,double[]> e = it.next();
			double[] terms = e.getValue();
			double j = terms[0]*terms[0]/(terms[1]*this.subtree[e.getKey()]);
			if (j < lowest){
				lowest = j;
			}
		}
		this.jain.add(lowest);
	}
	
	@Override
	public Single[] getSingles() {
		Single[] old = super.getSingles();
		Single initids = new Single(this.key +"_INIT_IDS", this.initIDs);
		Single initmes = new Single(this.key +"_INIT_MES", this.initMes);
		
		double[] idsSt = this.getAvMax(this.newIDs);
		Single idsav = new Single(this.key +"_IDS_AV", idsSt[0]);
		Single idsmax = new Single(this.key +"_IDS_MAX", idsSt[1]);
		double[] mesSt = this.getAvMax(this.messages);
		Single mesav = new Single(this.key +"_MES_AV", mesSt[0]);
		Single mesmax = new Single(this.key +"_MES_MAX", mesSt[1]);
		
		double[] idsStT = this.getAvMax(this.newIDsTot);
		Single idsTav = new Single(this.key +"_IDS_TOT_AV", idsStT[0]);
		Single idsTmax = new Single(this.key +"_IDS_TOT_MAX", idsStT[1]);
		double[] mesStT = this.getAvMax(this.messagesTot);
		Single mesTav = new Single(this.key +"_MES_TOT_AV", mesStT[0]);
		Single mesTmax = new Single(this.key +"_MES_TOT_MAX", mesStT[1]);
		
		Single fid = new Single(this.key+"_IDS_OVERLL_FRAC", idsSt[0]/idsStT[0]);
		Single fmes = new Single(this.key+"_MES_OVERLL_FRAC", mesSt[0]/mesStT[0]);
		
		double[] idsfSt = this.getAvMaxD(this.newIDs_frac);
		Single idsfav = new Single(this.key +"_IDS_FRAC_AV", idsfSt[0]);
		Single idsfmax = new Single(this.key +"_IDS_FRAC_MAX", idsfSt[1]);
		double[] mesfSt = this.getAvMaxD(this.mess_frac);
		Single mesfav = new Single(this.key +"_MES_FRAC_AV", mesfSt[0]);
		Single mesfmax = new Single(this.key +"_MES_FRAC_MAX", mesfSt[1]);
		
		double[] maxF = this.getAvMaxD(this.maxOverhead);
		Single maxFav = new Single(this.key +"_MAX_F_AV", maxF[0]);
		Single maxFmax = new Single(this.key +"_MAX_F_MAX", maxF[1]);
		double[] boundF = this.getAvMaxD(this.boundOver);
		Single boundFav = new Single(this.key +"_BOUND_F_AV", boundF[0]);
		Single boundFmax = new Single(this.key +"_BOUND_F_MAX", boundF[1]);
		
		double[] jfair = this.getAvMinD(this.jain);
		Single jfairav = new Single(this.key +"_JAIN_AV", jfair[0]);
		Single jfairmax = new Single(this.key +"_JAIN_MIN", jfair[1]);
		
		Single[] singles = new Single[]{old[0], initids, initmes,idsav, idsmax, mesav, mesmax, idsTav, idsTmax, mesTav, mesTmax,
				idsfav, idsfmax, mesfav, fid, fmes,
				mesfmax, maxFav, maxFmax, boundFav, boundFmax, jfairav, jfairmax};
		return singles; 
	}
	
	private double[] getAvMaxD(Vector<Double> vec){
		double sum = 0;
		double max = 0;
		for (int i = 0; i < vec.size(); i++){
			double val = vec.get(i);
			sum = sum + val;
			if (val > max){
				max = val;
			}
		}
		return new double[]{sum/vec.size(),max};
	}
	
	private double[] getAvMinD(Vector<Double> vec){
		double sum = 0;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < vec.size(); i++){
			double val = vec.get(i);
			sum = sum + val;
			if (val < min){
				min = val;
			}
		}
		return new double[]{sum/vec.size(),min};
	}
	
	private double[] getAvMax(Vector<Integer> vec){
		double sum = 0;
		double max = 0;
		for (int i = 0; i < vec.size(); i++){
			double val = vec.get(i);
			sum = sum + val;
			if (val > max){
				max = val;
			}
		}
		return new double[]{sum/vec.size(),max};
	}
	
	@Override
	public boolean writeData(String folder) {
		boolean succ = super.writeData(folder);
		succ = succ && DataWriter.writeWithIndex(this.avVector(this.newIDs, this.gra), this.key + "_NEW_IDS", folder); 
		succ = succ && DataWriter.writeWithIndex(this.avVector(this.messages, this.gra), this.key + "_MESS", folder); 
		succ = succ && DataWriter.writeWithIndex(this.avVector(this.newIDsTot, this.gra), this.key + "_NEW_IDS_TOT", folder); 
		succ = succ && DataWriter.writeWithIndex(this.avVector(this.messagesTot, this.gra), this.key + "_MESS_TOT", folder);
		succ = succ && DataWriter.writeWithIndex(this.avVectorD(this.newIDs_frac, this.gra), this.key + "_NEW_IDS_FRAC", folder); 
		succ = succ && DataWriter.writeWithIndex(this.avVectorD(this.mess_frac, this.gra), this.key + "_MESS_FRAC", folder); 
		succ = succ && DataWriter.writeWithIndex(this.avVectorD(this.maxOverhead, 1), this.key + "_MAX_F", folder); 
		succ = succ && DataWriter.writeWithIndex(this.avVectorD(this.boundOver, 1), this.key + "_BOUND_F", folder); 
		succ = succ && DataWriter.writeWithIndex(this.avVectorD(this.jain, 1), this.key + "_JAIN", folder); 
		return succ;
	}
	

}
