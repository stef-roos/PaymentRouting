package paymentrouting.route.bailout;


import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import gtna.data.Single;
import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.DataWriter;
import gtna.util.Distribution;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;
import paymentrouting.datasets.LNParams;
import paymentrouting.route.PartialPath;
import paymentrouting.route.PathSelection;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;
import paymentrouting.route.concurrency.ScheduledUnlock;

public class RoutePaymentBailoutCount extends RoutePaymentConcurrent{
	PaymentReaction react;
	LNParams params; 
	int successfulBail; 
	int failedBail;
	int failedBailSrc;
	int noBailNode;
	int noNeedtoBail;
	Distribution bailoutNumber; 
	Distribution bailoutNumberSucc;
	
	double wait;
	double bailoutTime=-1; //time all bailout
	boolean done = false; 
	


	public RoutePaymentBailoutCount(PathSelection ps, int trials, double latency, String recordFile, PaymentReaction react, double wait) {
		super(ps, trials, latency, recordFile,
				new Parameter[] {new StringParameter("PAYMENT_REACTION", react.getName()), 
						 new DoubleParameter("WAITING", wait)});
		this.react = react; 
		this.wait = wait;
	}
	
	public RoutePaymentBailoutCount(PathSelection ps, int trials, double latency, PaymentReaction react, double wait) {
		super(ps, trials, latency, 
				new Parameter[] {new StringParameter("PAYMENT_REACTION", react.getName()), 
						 new DoubleParameter("WAITING", wait)});
		this.react = react; 
		this.wait = wait;
	}
	
	
	protected void bailout(Graph g) {
		successfulBail = 0; 
		failedBail = 0;
		failedBailSrc = 0; 
		noBailNode = 0;
		noNeedtoBail = 0;
		long[] bailoutNS = new long[0];
		long[] bailoutN = new long[0];
		
		
		Node[] nodes = g.getNodes();
		int included = 0; 
		Iterator<ScheduledUnlock> it = this.qLocks.iterator();
		if (log) {
		while (it.hasNext()) {
			ScheduledUnlock lock = it.next();
			System.out.println(lock.getEdge().getSrc() + " " + lock.getEdge().getDst() + " " + lock.getStartTime() + " " + 
			lock.getTime() + " " + lock.getMaxTime()); 
		}
		}
		for (int i = 0; i < nodes.length; i++) {
			if (log) System.out.println("Node " + i);
			HashMap<Edge,Double> reducedCap = new HashMap<Edge,Double>(); 
			int bails = 0; 
			boolean succ = true;
			boolean exclude = false; 
			//iterate over all locks node is part of 
			int[] out = nodes[i].getOutgoingEdges();
			for (int neigh: out) {
				Vector<ScheduledUnlock[]> edgeLocks = this.getLocks(new Edge(i, neigh)); 
				bails = bails + edgeLocks.size();
				for (int j = 0; j < edgeLocks.size(); j++) {
					ScheduledUnlock[] l = edgeLocks.get(j);
					//if node is sender of lock -> no need to bil out of this one 
					if (l[0] == null) {
						continue; 
					} else {
						//if intermediary -> find loops with two edges 
						int length = 3;
						boolean foundLoop = false; 
						boolean solved = false;
						double val = l[0].getVal();
						while (length <= 5 && !solved) {
						       Vector<Vector<Edge>> loops = getLoops(l[0].getEdge(), l[1].getEdge(), length-2, g);
						       if (loops.size() == 0) {
						    	   length++;
						    	   continue;
						       } else {
						    	   foundLoop = true; 
						    	   for (int k = 0; k < loops.size(); k++) {
						    	   //determine minimal capacity of loop
						    		   Vector<Edge> loop = loops.get(k);
						    		   double cap = Double.MAX_VALUE; 
						    		   for (int m = 0; m < loop.size(); m++) {
						    			   Edge em = loop.get(m); 
						    			   Double x = reducedCap.get(em);
						    			   if (x == null) {
						    				   x = 0.0;  
						    			   }
						    			   cap = Math.min(cap, this.computePotential(em.getSrc(), em.getDst())-x);
						    		   }
						    		   if (cap > 0) {
						    			   //assign cap of val to this loop 
						    			   double assign = Math.min(cap, val);
						    			   for (int m = 0; m < loop.size(); m++) {
							    			   Edge em = loop.get(m); 
							    			   Double x = reducedCap.get(em);
							    			   if (x == null) {
							    				   x = 0.0;  
							    			   }
							    			   x = x + assign;
							    			   reducedCap.put(em,x);
							    		   }
						    			   val = val - assign;
						    			   if (val <= 0) {
						    				   solved = true;
						    				   break; 
						    			   }
						    		   }
						    	   }
						       }
						       length++; 
						}
						if (!foundLoop) {
							exclude = true;
							succ = false; 
							this.noBailNode++; 
							if (log) System.out.println("no node");
							break; 
						}
						if (!solved) {
							succ = false; 
						}
					}
				}
			}
			
			if (bails == 0) {
				//exclude nodes that did not need bailouts
				exclude = true;
				this.noNeedtoBail++; 
				if (log) System.out.println("no need");
			}
			if (!exclude) {
				included++; 
				if (succ) {
					bailoutNS = this.inc(bailoutNS, bails); 
					this.successfulBail++;
					if (log) System.out.println("succ");
				} else {
					this.failedBail++;
					if (log) System.out.println("fail");
				}
				bailoutN = this.inc(bailoutN, bails); 
			}
			

		}
		
		this.bailoutNumber = new Distribution(bailoutN,included); 
		this.bailoutNumberSucc = new Distribution(bailoutNS,included); 
	}
	

	private Vector<Vector<Edge>> getLoops(Edge e1, Edge e2, int max, Graph g){
		int a = e1.getSrc();
		int b = e1.getDst();
		int c = e2.getDst();
		Vector<Vector<Edge>> res = new Vector<Vector<Edge>>();
		//get loops 
		LinkedList<Vector<Integer>> list = new LinkedList<Vector<Integer>>();
		Vector<Integer> path = new Vector<Integer>();
		path.add(a);
		list.add(path);
		Node[] nodes = g.getNodes();
		while (path != null && path.size() <= max) {
			int[] out = nodes[path.lastElement()].getOutgoingEdges();
			for (int i: out) {
				if (i == b) continue; //path can't go via b 
				if (i == c) {
					//found path from a to c that can act as alternative -> add to set of alternative paths  
					Vector<Edge> vec = new Vector<Edge>();
					for (int j = 0; j < path.size()-1; j++) {
						Edge ej = new Edge(path.get(j), path.get(j+1));
						vec.add(ej);
					}
					vec.add(new Edge(path.lastElement(),c)); 
					res.add(vec); 
				} else {
					if (list.size() < 1000) {
					//check that i does not create loop 
					boolean loop = false;
					for (int j = 0; j < path.size(); j++) {
						if (path.get(j) == i) {
							loop = true;
							break;
						}
					}
					if (loop) continue; 
					
					//add new path to list
					Vector<Integer> pathc = (Vector<Integer>) path.clone(); 
					pathc.add(i);
					list.add(pathc);
					}
				}
			}
			path = list.poll(); 
		}
		return res; 
	}
	

	
	@Override 
	protected boolean agree(int curN, double val, double curTime) {
		return this.react.acceptLock(this.graph, curN, curTime, val, rand);
	}
	
	@Override
	public double getTimeToUnlock(double timeBefore, Edge e, boolean succ, double val) {
		if (succ) {
		   return this.linklatency + this.react.forwardHash(this.graph, e, timeBefore, rand); 
		} else {
		   return this.linklatency + this.react.resolve(this.graph, e, timeBefore, rand); 
		}
	}
	
	public boolean[] checkFinal(Vector<PartialPath> vec, int dst) {
		boolean[] res = super.checkFinal(vec, dst);
		if (res[0] && res[1]) {
			res[1] = this.react.receiverReaction(dst); //receiver can decide to fail payment 
		}
		return res; 
	}
	
	@Override 
	public void unlockAllUntil(double limit, Graph g) {
		if (this.bailoutTime == -1) {
			this.bailoutTime = this.transactions[this.transactions.length-1].getTime(); 
		}
		if (limit <= this.bailoutTime || done) {
			super.unlockAllUntil(limit, g);
		} else {
			super.unlockAllUntil(this.bailoutTime+wait, g);
			this.bailout(g);
			done = true; 
		}
		
	}
	
	public Vector<ScheduledUnlock[]> getLocks(Edge e){
		Vector<ScheduledUnlock[]> vec = new Vector<ScheduledUnlock[]>();
		HashMap<Integer, ScheduledUnlock> preLink = new HashMap<Integer, ScheduledUnlock>();
		Iterator<ScheduledUnlock> it = this.qLocks.iterator();
		//if (log) System.out.println("Total available locks: " + this.qLocks.size()); 
		while (it.hasNext()) {
			ScheduledUnlock lock = it.next();
			if (lock.getEdge().equals(e)) {
				ScheduledUnlock[] locks = new ScheduledUnlock[2];
				locks[1] = lock;
				vec.add(locks); 
			} else {
				int s = lock.getEdge().getDst();
				if (s == e.getSrc()) {
					preLink.put(lock.getNr(), lock); 
				}
			}
		}
		for (int i = 0; i < vec.size(); i++) {
			ScheduledUnlock[] locks = vec.get(i);
			ScheduledUnlock preLock = preLink.get(locks[1].getNr());
			locks[0] = preLock; 
		}
		return vec;	
	}
	

	

	
	@Override
	public Single[] getSingles() {
		Single[] singles = super.getSingles();
		Single[] allSingle = new Single[singles.length+7];
		for (int i = 0; i < singles.length; i++) {
			allSingle[i] = singles[i]; 
		}
		int n = this.noBailNode + this.failedBailSrc + this.noNeedtoBail + this.successfulBail + this.failedBail; 
		int index = singles.length;
		allSingle[index++] = new Single("NO_BAIL_NODE", (double)this.noBailNode/(double)n); 
		allSingle[index++] = new Single("SRC_BAIL_FAIL", (double)this.failedBailSrc/(double)n); 
		allSingle[index++] = new Single("NO_NEED_BAIL", (double)this.noNeedtoBail/(double)n); 
		allSingle[index++] = new Single("SUCC_BAIL_ALL", (double)this.successfulBail/(double)n); 
		allSingle[index++] = new Single("FAIL_BAIL_ALL", (double)this.failedBail/(double)n); 
		allSingle[index++] = new Single("SUCC_BAIL_ONLY", (double)this.successfulBail/(double)(this.successfulBail+this.failedBail)); 
		allSingle[index++] = new Single("FAIL_BAIL_ONLY", (double)this.failedBail/(double)(this.successfulBail+this.failedBail));

		return allSingle; 
	}
	
	@Override
	public boolean writeData(String folder) {
		boolean succ = super.writeData(folder);
		succ = succ & DataWriter.writeWithIndex(this.bailoutNumber.getDistribution(), this.key + "_BAILOUT_NUMBER" ,folder);
		succ = succ & DataWriter.writeWithIndex(this.bailoutNumberSucc.getDistribution(), this.key + "_BAILOUT_NUMBER_SUCC" ,folder);
		return succ; 
	}
	
	
	
	@Override
	public void postprocess() {
		//compute final stats
		super.postprocess();
		
		
		
	}
	
	
	


}
