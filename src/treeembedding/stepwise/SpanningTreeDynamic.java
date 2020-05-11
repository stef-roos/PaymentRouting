package treeembedding.stepwise;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.transformation.churn.DynamicMetric;
import gtna.util.parameter.Parameter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;

public class SpanningTreeDynamic extends DynamicMetric {
	int[] rootIds;
	int[][] parent;
	int[][] children;
	int[] tree;
	int[] subtree;
	Random rand;
	Vector<Integer> changed;
	int initCost; 
	int gra;
	int components;
	boolean debug = false;
	Vector<Integer> newRoots;
	Vector<int[]> changedChild;
	int maxdepth = 0;
	

	public SpanningTreeDynamic(String key, Parameter[] params ,String name,
			String folder,  String s, boolean random, double minus, int it, int granularity, int seed, String m) {
		super(key, DynamicMetric.extendParams(params, new Parameter[]{}), name, folder, m, s, random, minus, it,seed);
		this.gra = granularity;
		rand = new Random(seed);
	}
	
	public SpanningTreeDynamic(String key, Parameter[] params, String name,
			String folder, String s, boolean random, double minus, int it, int granularity, int seed) {
		super(key, DynamicMetric.extendParams(params, new Parameter[]{}), name, folder, s, random, minus, it,seed);
		this.gra = granularity;
		rand = new Random(seed);
	}
	
	public SpanningTreeDynamic(String name,
			String folder,  String s, boolean random, double minus, int it, int granularity, int seed, String m) {
		this("ST_DYM",new Parameter[0],name,folder,s,random,minus,it, granularity,seed,m); 
	}
	
	public SpanningTreeDynamic(String name,
			String folder, String s, boolean random, double minus, int it, int granularity, int seed) {
		this("ST_DYM",new Parameter[0], name,folder,s,random,minus,it, granularity,seed); 
	}
	
	public SpanningTreeDynamic(String name,
			String sess, String inter, String s, int it, int granularity, int seed) {
		super("ST_DYM",new Parameter[0], name,sess, inter,s,it, seed); 
		this.gra = granularity;
		rand = new Random(seed);
	}
	
	public SpanningTreeDynamic(String key, Parameter[] params,String name,
			String sess, String inter, String s, int it, int granularity, int seed) {
		super(key,params, name,sess, inter,s,it, seed); 
		this.gra = granularity;
		rand = new Random(seed);
	}
	

	@Override
	public void initGraph(Graph g, Network n, HashMap<String, Metric> m) {
		//initialize sets
		Node[] nodes = g.getNodes();
		changed = new Vector<Integer>();
		this.parent = new int[nodes.length][2];
		this.children = new int[nodes.length][];
		this.tree = new int[nodes.length];
		this.subtree = new int[nodes.length];
		//declare IDs
		this.rootIds = new int[nodes.length];
		for (int j = 0; j < nodes.length; j++){
			rootIds[j] = Math.abs(rand.nextInt());
			this.tree[j] = -1;
		}
		//establish a spanning tree with certain root node
		boolean done = false;
		this.initCost = 0;
		this.reinit();
		while (!done){
			int max = 0;
			int maxInd = -1;
		    for (int j = 0; j < nodes.length; j++){
			   if (this.step.isOn(j) && tree[j] == -1 && this.rootIds[j] > max){
				   max = this.rootIds[j];
				   maxInd = j;
			   }
		    }
		    if (maxInd != -1){
		    	this.initCost = this.initCost+this.constructNewTree(maxInd, maxInd, nodes, -1,0, 0);
		    	this.newRoots.add(maxInd);
		    } else{
		    	done = true;
		    }
		}
       this.printTree();
	}
	
	private int constructNewTree(int root, int start,Node[] nodes,int startP, int startD, int sizeBefore){
		return this.constructNewTree(root, start, nodes, startP, startD, sizeBefore,null);
	}
	
	private int constructNewTree(int root, int start,Node[] nodes,int startP, int startD, int sizeBefore, HashSet<Integer> toadd){
		Queue<Integer> q = new LinkedList<Integer>();
		HashSet<Integer> leaves = new HashSet<Integer>();
		q.add(start);
		this.parent[start][0] = startP;
		this.parent[start][1] = startD;
		this.tree[start] = root;
		this.subtree[start] = 1;
		int cost = 0;
		if(debug) {
			System.out.println("Got construct with root=" + root + " at node " + start);
		}
		while (!q.isEmpty()){
			int cur = q.poll();
			if(debug) {
				System.out.println("Got queue with cur=" + cur);
			}
			sizeBefore++;
			Vector<Integer> ch = new Vector<Integer>();
			int[] out = nodes[cur].getIncomingEdges();
			for (int k: out){
				if (this.step.isOn(k) && (tree[k]==-1 || this.parent[cur][0] != k)){
					cost++;
					if (tree[k] == -1){
						q.add(k);
						tree[k] = root;
						this.parent[k][0]=cur;
						this.parent[k][1]=this.parent[cur][1]+1;
						ch.add(k);
					} else {
						if (toadd != null && !toadd.contains(k)) continue;
						if (parent[k][0] == cur) continue;
						int old = parent[k][0];
						if (parent[k][1]>=this.parent[cur][1]+1 && this.rootIds[old]<rootIds[cur]){
							this.parent[k][0]=cur;
							this.parent[k][1]=this.parent[cur][1]+1;
							ch.add(k);
							int[] childO = this.children[old];
							this.children[old] = new int[childO.length-1];
							int j = 0;
							for (int l = 0; l < this.children[old].length; l++){
								if (childO[j]==k){
									j++;
								}
								this.children[old][l]=childO[j];
								j++;
							}
							if (this.children[old].length == 0 && old != start){								
								leaves.add(old);
								this.subtree[old] = 1;
							}
							
							//reset depth of desc
							if (this.children[k] != null && this.children[k].length > 0){
							Queue<Integer> re = new LinkedList<Integer>();
							re.add(k);
							if(debug) {
								System.out.println("Got before re-queue with k=" + k + " children " + children[k].length + " " + children[k][0] + " " + tree[k]);
							}
							while (!re.isEmpty()){
								int par = re.poll();
								if(debug) {
									System.out.println("Got re-queue with par=" + par);
								}
								int[] kids = this.children[par];
								for (int l = 0; l < kids.length; l++){
									this.parent[kids[l]][1] = this.parent[par][1]+1;
									re.add(kids[l]);
								}
							}
							}
						}
					}
				}
				
			}
			children[cur] = new int[ch.size()];
			for (int l = 0; l < children[cur].length; l++){
				children[cur][l]=ch.get(l);
			}
			if (children[cur].length == 0 && cur != start){
				leaves.add(cur);
				this.subtree[cur] = 1;
			} else {
				leaves.remove(cur);
			}
		}
		if(debug) {
			System.out.println("Got after tree build with root=" + root + " at node " + start);
		}
		//set subtree sizes for new tree
		HashSet<Integer> next = new HashSet<Integer>(); 
		while (!leaves.isEmpty()){
		Iterator<Integer> it = leaves.iterator();
		while (it.hasNext()){
			int l = it.next();
			int par = this.parent[l][0];
			if (par != -1 && !next.contains(par)){
				int[] chi = this.children[par];
				boolean add = true;
				for (int c: chi){
					if (!leaves.contains(c)){
						add = false;
						break;
					}
				}
				if (add){
					this.subtree[par] =1;
					for (int c: chi){
						this.subtree[par] = this.subtree[par]+this.subtree[c];
					}
					if (par != start)next.add(par);
				} else {
					for (int c: chi){
						if (leaves.contains(c)){
							next.add(c);
						}
					}
				}
			}
		}
		leaves = next;
		next = new HashSet<Integer>();
		//System.out.println("terminate round " + k);
		}
		if(debug) {
			System.out.println("Got after leaves with root=" + root + " at node " + start);
		}
		
		//update subtree sizes towards root
		int cur = start;
		while (this.parent[cur][0] !=-1){
			cur = this.parent[cur][0];
			int[] chi = this.children[cur];
			this.subtree[cur] = 1;
			for (int c: chi){
				this.subtree[cur] = this.subtree[cur] + this.subtree[c];
			}
		}
		
		return 2*cost;
	}
	

	@Override
	public void processJoin(Graph g, Network n, HashMap<String, Metric> m,
			int node) {
		reinit();
		Node[] nodes = g.getNodes();
		changed.add(this.joinTree(nodes, node));
		this.printTree();
	}
	
	private void reinit(){
		this.changedChild = new Vector<int[]>();
		this.newRoots = new Vector<Integer>();
	}
	
	private int joinTree(Node[] nodes, int node){
		//three cases: i) node is new root, ii) simple join, iii) merge components 
		
		//preparation to check which case
		//get roots in neighborhood
		int cost = 0; //send message to all ponline neighbors 
		int[] out = nodes[node].getOutgoingEdges();
		Vector<Integer> a = new Vector<Integer>();
		for (int i: out){
			if(this.step.isOn(i)){
				cost = cost +2;
				if (!a.contains(tree[i]) && tree[i] != -1){
					a.add(tree[i]);
				}
			}
				
			
		}
		//special case: no online neighbors
		if (a.size() == 0){
			tree[node] = node;
			this.parent[node][0]=-1;
			this.children[node]=new int[0];
			this.subtree[node] = 1;
			this.newRoots.add(node);
			return cost;
		}
		//get maximal rootId in neighborhood
		int max = this.rootIds[node];
		int maxInd = node;
		for (int j = 0; j < a.size(); j++){
			int b = this.rootIds[a.get(j)];
			if(b > max){
				max = b;
				maxInd = a.get(j);
			}
		}
		
		//case: new root
		if (max == this.rootIds[node]){
			if (debug) System.out.println(" new root");
			for (int j = 0; j < this.tree.length; j++){
				if (a.contains(this.tree[j])){
					this.newRoots.removeElement(this.tree[j]);
					this.tree[j] = -1;
					this.children[j] =null;
				}
			}
			this.newRoots.add(node);
			return this.constructNewTree(node, node, nodes, -1, 0,0);
			
		}
		int size = this.subtree[maxInd];
		//case: simple join & merge
		 //get parent closest to root (least rootId to break ties) with lowest rootID
			int depth = Integer.MAX_VALUE;
			int pp = -1;
			for (int i:out){
				if (this.step.isOn(i) &&  this.tree[i] == maxInd){
					if (this.parent[i][1] < depth){
						depth = this.parent[i][1];
						pp = i;
					} else {
						if (this.parent[i][1] == depth){
							if (this.rootIds[i] > this.rootIds[pp]){
								pp=i;
							}
						}
					}
				}
				
			}
			this.parent[node][0]=pp;
			this.parent[node][1]=depth+1;
			this.changedChild.add(new int[]{node,pp});
			
			this.children[node] = new int[0];
			int[] oldCh = this.children[pp];
			this.children[pp] = new int[oldCh.length+1];
			for (int j = 0; j < oldCh.length; j++){
				if (oldCh[j] == node) System.out.println("uhn " + node + " " + pp);
				this.children[pp][j] = oldCh[j];
			}
			this.children[pp][oldCh.length]=node;
			this.tree[node] = maxInd;
			cost = cost +2;
			//additional merge part -> re-embed all different neighbors
			HashSet<Integer> toadd = new HashSet<Integer>();
			if (a.size() > 1){
				for (int j = 0; j < this.tree.length; j++){
				 if (this.tree[j] != maxInd && a.contains(this.tree[j])){
					this.tree[j] = -1;
					this.children[j] = null;
					toadd.add(j);
				 }
			    }
				if (debug)System.out.println("....... a > 0 " + toadd.size());
			} 
			int extra = this.constructNewTree(maxInd, node, nodes, pp, depth+1,size,toadd);
			cost = cost + extra;
			return cost;
	}

	@Override
	public void processLeave(Graph g, Network n, HashMap<String, Metric> m,
			int node) {
		reinit();
		Node[] nodes = g.getNodes();
		//two cases: root leaves and normal node
		//case root => re-init component
		if (this.tree[node] == node){
			if (debug)System.out.println("---root leaves");
			this.tree[node] = -1;
			for (int j = 0; j < tree.length; j++){
				if (this.step.isOn(j) && tree[j] == node){
					tree[j] = -1;
					this.children[j] = null;
				}
			}
			boolean done = false; 
			int cost = 0;
			while (!done){
				int max = 0;
				int maxInd = -1;
			    for (int j = 0; j < nodes.length; j++){
				   if (this.step.isOn(j) && tree[j] == -1 && this.rootIds[j] > max){
					   max = this.rootIds[j];
					   maxInd = j;
				   }
			    }
			    if (maxInd != -1){
			    	cost = cost+this.constructNewTree(maxInd, maxInd, nodes, -1,0,0);
			    	this.newRoots.add(maxInd);
			    } else{
			    	done = true;
			    }
			}
			changed.add(cost);
		} else {
			//case: simple leave => remove child from parent and rejoin descendants
			  //remove child from parent and reduce size
			int rem = this.subtree[node];
			int c = node;
			while (this.parent[c][0] !=-1){
				c= this.parent[c][0];
				this.subtree[c] = this.subtree[c] -rem;
			}
					
			int old = this.parent[node][0];
			int[] childO = this.children[old];
			this.children[old] = new int[childO.length-1];
			int j = 0;
			for (int l = 0; l < this.children[old].length; l++){
				if (j == childO.length){
					String wtf = "kids of " +old;
					for (int w = 0; w < childO.length; w++){
						wtf = wtf + " " + childO[w] + " " + parent[childO[w]][0];
					}
					System.out.println(wtf);
				}
				if (childO[j]==node){
					j++;
				}
				this.children[old][l]=childO[j];
				j++;
			}
			
			//disconnect desc
			Queue<Integer> q = new LinkedList<Integer>();
			Queue<Integer> re = new LinkedList<Integer>();
			q.add(node);
			while (!q.isEmpty()){
				int cur = q.poll();
				tree[cur] = -1;
				for (int i: children[cur]){
					q.add(i);
					re.add(i);
				}
				this.children[cur] = null;
				
			}
			//rejoin desc
			
			int cost=0;
			int reS = re.size();
			while (!re.isEmpty()){
				Queue<Integer> h = new LinkedList<Integer>();
				while (!re.isEmpty()){
				   int k = re.poll();
					int[] out = nodes[k].getOutgoingEdges();
				   int potPar = -1;
				   int minDepth = Integer.MAX_VALUE;
				   for (int i:  out){
					   if (this.step.isOn(i) && tree[i] != -1){
						   if (this.parent[i][1] < minDepth){
							   minDepth = this.parent[i][1];
							   potPar = i;
						   } else {
						     if (this.parent[i][1] == minDepth){
							   if (this.rootIds[i] > this.rootIds[potPar]){
								   potPar = i;
							   }
						     }
						   }
					   }
				   }
				   if (potPar !=  -1){
					   cost = cost + 2;
					   this.parent[k][0] = potPar;
					   this.parent[k][1] = minDepth+1;
					   int[] ol = this.children[potPar];
					   this.children[potPar] = new int[ol.length+1];
					   for (int x = 0; x < ol.length; x++){
						   this.children[potPar][x] = ol[x];
					   }
					   this.children[potPar][ol.length] = k;
					   this.changedChild.add(new int[]{k,potPar});
					   this.children[k] = new int[0];
					   this.tree[k] = this.tree[potPar];
					   this.subtree[k] = 1;
					   while (potPar != -1){
						   this.subtree[potPar]++;
						   potPar = this.parent[potPar][0];
					   }
				   } else {
					   h.add(k);
				   }
				}
				if (h.size() == reS){
					  while(!h.isEmpty()){ 
					   int nr = -1;
					   int nrInd = -1;
					   Iterator<Integer> it = h.iterator();
					   while (it.hasNext()){
						   int k = it.next();
						   if (this.rootIds[k] > nrInd){
							   nrInd = this.rootIds[k];
							   nr = k;
						   }
					   }
					   cost = cost + this.constructNewTree(nr, nr, nodes, -1, 0, 0);
					   this.newRoots.add(nr);
					   it = h.iterator();
					   while (it.hasNext()){
						   int k = it.next();
						   if (tree[k] == -1){
							   re.add(k);
						   }
					   }
					   h = re;
					   re = new LinkedList<Integer>();
					  }
					   break;
				   } else {
					   re = h;
					   reS = re.size();
				   }
			}
			changed.add(cost);
		}
		
		this.printTree();
	}
	
	private void printTree(){
		if (debug){
		HashSet<Integer> roots = new HashSet<Integer>();
		for (int i = 0; i < this.tree.length; i++){
			if (step.isOn(i) && !roots.contains(this.tree[i])){
				if (tree[i] == -1) continue;
				roots.add(tree[i]);
				Queue<Integer> q = new LinkedList<Integer>();
				q.add(tree[i]);
				System.out.println("--------Tree " + rootIds[tree[i]] + " " + tree[i]);
				while (!q.isEmpty()){
					int cur = q.poll();
					System.out.println(cur + ", " + this.parent[cur][0] + ", " + this.parent[cur][1] + " " + this.subtree[cur]);
					for (int k = 0; k < this.children[cur].length; k++){
						q.add(children[cur][k]);
					}
				}
			}
		}
		
		}
	}
	

	@Override
	public boolean writeData(String folder) {
		boolean succ = super.writeData(folder);
		return succ && DataWriter.writeWithIndex(this.avVector(this.changed, this.gra), this.key + "_TREE_STAB", folder);
	}
	
	public double[] avVector(Vector<Integer> vec, int granu){
		double[] cost = new double[(int)Math.ceil(vec.size()/granu)];
		int k = 0;
		for (int j = 0; j < cost.length-1; j++){
			for (int i = k; i < k+granu; i++){
				cost[j] = cost[j]+vec.get(i);
			}
			cost[j] = cost[j]/(double)granu;
			k = k + granu;
		}
		for (int i = k; i < vec.size(); i++){
			cost[cost.length-1] = cost[cost.length-1]+vec.get(i);
		}
		cost[cost.length-1]=cost[cost.length-1]/(double)(vec.size()-k);
		return cost;
	}
	
	public double[] avVectorD(Vector<Double> vec, int granu){
		double[] cost = new double[(int)Math.ceil(vec.size()/granu)];
		int k = 0;
		for (int j = 0; j < cost.length-1; j++){
			for (int i = k; i < k+granu; i++){
				cost[j] = cost[j]+vec.get(i);
			}
			cost[j] = cost[j]/(double)granu;
			k = k + granu;
		}
		for (int i = k; i < vec.size(); i++){
			cost[cost.length-1] = cost[cost.length-1]+vec.get(i);
		}
		cost[cost.length-1]=cost[cost.length-1]/(double)(vec.size()-k);
		return cost;
	}

	@Override
	public Single[] getSingles() {
		// TODO Auto-generated method stub
		return new Single[]{new Single(this.key +"_INIT", this.initCost)};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return true;
	}

	
	

}
