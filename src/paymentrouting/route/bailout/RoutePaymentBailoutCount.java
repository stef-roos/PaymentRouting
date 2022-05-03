package paymentrouting.route.bailout;


import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import gtna.data.Single;
import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
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
	double successfulBail; 
	double failedBail;
	double failedBailSrc;
	int noBailNode;
	double noNeedtoBail;
	double successChannelBail;
	double failedChannelBail;
	int noChannelBailNode; 
	double noChannelNeedtoBail; 
	long[] bailoutNumber; 
	long[] bailoutNumberSucc; 
	double wait;
	double bailoutTime=-1; //time all bailout
	
	


	public RoutePaymentBailoutCount(PathSelection ps, int trials, double latency, String recordFile, PaymentReaction react, double wait) {
		super(ps, trials, latency, recordFile,
				new Parameter[] {new StringParameter("PAYMENT_REACTION", react.getName()), 
						 new DoubleParameter("WAIT", wait)});
		this.react = react; 
		this.wait = wait;
	}
	
	
	protected void bailout(Graph g) {
		successfulBail = 0; 
		failedBail = 0;
		failedBailSrc = 0; 
		noBailNode = 0;
		noNeedtoBail = 0;
		successChannelBail = 0; 
		failedChannelBail = 0;
		noChannelBailNode = 0;
		noChannelNeedtoBail = 0;
		this.bailoutNumberSucc = new long[0];
		this.bailoutNumber = new long[0];
		
		
		Node[] nodes = g.getNodes();
		int included = 0; 
		for (int i = 0; i < nodes.length; i++) {
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
					//if node is sender of lock -> exclude as impossible  
					if (l[0] == null) {
						exclude = true;
						failedBailSrc++;
						succ = false; 
						break; 
					} else {
						//if intermediary -> find loops with two edges 
						int length = 3;
						boolean foundLoop = false; 
						boolean solved = false;
						double val = l[0].getVal();
						while (length <= 6 && !solved) {
						       Vector<Vector<Edge>> loops = getLoops(l[0].getEdge(), l[1].getEdge(), length, g);
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
						    			   double x = reducedCap.get(em);
						    			   cap = Math.min(cap, this.computePotential(em.getSrc(), em.getDst())-x);
						    		   }
						    		   if (cap > 0) {
						    			   //assign cap of val to this loop 
						    			   double assign = Math.min(cap, val);
						    			   for (int m = 0; m < loop.size(); m++) {
							    			   Edge em = loop.get(m); 
							    			   double x = reducedCap.get(em);
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
						}
						if (!foundLoop) {
							exclude = true;
							succ = false; 
							this.noBailNode++; 
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
			}
			if (!exclude) {
				included++; 
				if (succ) {
					this.inc(this.bailoutNumberSucc, bails); 
					this.successfulBail++;
				} else {
					this.failedBail++;
				}
				this.inc(this.bailoutNumber, bails); 
			}
			

		}
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
		while (path.size() <= max) {
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
		if (limit <= this.bailoutTime) {
			super.unlockAllUntil(limit, g);
		} else {
			super.unlockAllUntil(this.bailoutTime+wait, g);
			this.bailout(g);
		}
		
	}
	
	public Vector<ScheduledUnlock[]> getLocks(Edge e){
		Vector<ScheduledUnlock[]> vec = new Vector<ScheduledUnlock[]>();
		HashMap<Integer, ScheduledUnlock> preLink = new HashMap<Integer, ScheduledUnlock>();
		Iterator<ScheduledUnlock> it = this.qLocks.iterator();
		if (log) System.out.println("Total available locks: " + this.qLocks.size()); 
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
		Single[] allSingle = new Single[singles.length+11];
		for (int i = 0; i < singles.length; i++) {
			allSingle[i] = singles[i]; 
		}
		
		

		return allSingle; 
	}
	
	@Override
	public boolean writeData(String folder) {
		boolean succ = super.writeData(folder);
		
		
		return succ; 
	}
	
	
	
	@Override
	public void postprocess() {
		//compute final stats
		super.postprocess();
		
		
		
	}
	
	
	


}
