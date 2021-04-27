package paymentrouting.route.concurrency;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.Parameter;
import paymentrouting.route.PartialPath;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;
import treeembedding.credit.Transaction;

public class RoutePaymentConcurrent extends RoutePayment {
	double linklatency; //link latency in ms
	double now = 0; 
	PriorityQueue<ConcurrentTransaction> qTr; 
	HashMap<Integer, Vector<PartialPath>> ongoingTr; 
	HashMap<Edge, Double> locked; 
	PriorityQueue<ScheduledUnlock> qLocks;
	public ConcurrentTransaction curT;
	
	
	
	
	public RoutePaymentConcurrent(PathSelection ps, int trials, double latency) {
		this(ps,trials,Integer.MAX_VALUE, latency); 
	}
	

	public RoutePaymentConcurrent(PathSelection ps, int trials, int epoch, double latency) {
		super(ps, trials, true, epoch, new Parameter[]{new DoubleParameter("LINK_LATENCY", latency)});
		this.linklatency = latency; 
	}
	
	 public void run(Graph g) {
		 Node[] nodes = g.getNodes();
		 //Add transactions into queue
		 for (int i = 0; i < this.transactions.length; i++) {
			 Transaction t = this.transactions[i]; 
			 ConcurrentTransaction ct = new ConcurrentTransaction(i,t); 
			 qTr.add(ct); 
		 }
		 double curTime = 0; 
		 boolean[] excluded = new boolean[nodes.length]; 
		 
		 //loop until all transactions are processed 
		 while (!qTr.isEmpty()) {
		   //take a transaction, process all unlocks before, check if final or new (final for all paths = last node receiver or -1 indicating failure)
		   curT = qTr.poll();
		   curTime = curT.getTime(); 
		   if (log) {
			   System.out.println("Processing tx " + curT.getNr() + " at time " + curTime); 
		   }
		   this.unlockAllUntil(curTime);
		   Vector<PartialPath> vec = this.ongoingTr.get(curT.getNr()); 
		   if (vec != null) {
		      boolean[] state = this.checkFinal(vec, curT.getDst());
		      if (state[0]) {
		    	  //done 
		    	  //if final: schedule + compute stats 
		    	  int[] stats = this.getStats(vec); 
		    	  transStats(state[1],stats[0],stats[1],curT.getNr(),0); 
		    	  //schedule unlocks
		    	  if (state[1]) {
		    		  this.scheduleSuccessful(vec, curTime);
		    	  } else {
		    		  this.scheduleNotSuccessful(vec, curTime, curT.getDst(),stats[0]);
		    	  }
		    	  continue; //go to next transaction 
		      } else {
		    	  //non-final: main logic done below  
		      }
		   } else {
			   //add new transaction to ongoing 
			   vec = new Vector<PartialPath>();
			   double[] splitVal = this.splitRealities(curT.getVal(), select.getDist().getStartR(), rand);
		    	for (int a = 0; a < select.getDist().getStartR(); a++) {
		    	     vec.add(new PartialPath(curT.getSrc(), splitVal[a],new Vector<Integer>(),a));
		        }
			   ongoingTr.put(curT.getNr(),vec); 
		   }
		    //if not final:  iterate over all non-final paths 
		   Vector<PartialPath> next = new  Vector<PartialPath>();
		   for (int i = 0; i < vec.size(); i++) {
			   //retrieve partial path and get next hops 
			   PartialPath pp = vec.get(i);
               int curN = pp.node;
               //finished path -> no more steps
               if (curN == -1 || curN == curT.getDst()) {
            	   next.add(pp);
            	   continue; 
               }
               int pre = -1;
               Vector<Integer> past = pp.pre;
           	   if (past.size() > 0) {
           		   pre = past.get(past.size()-1);
           	   }
           	   for (int l = 0; l < past.size(); l++) {
           		    excluded[past.get(l)] = true;
           	   }
           	
           	   //if (log) System.out.println("Routing at cur " + curN); 
               double[] partVals = this.select.getNextsVals(g, curN, curT.getDst(), 
               		pre, excluded, this, pp.val, rand, pp.reality); 
               for (int l = 0; l < past.size(); l++) {
           		excluded[past.get(l)] = false;
           	   }
               past.add(curN);
		       //if next hops found: get next hops and add to locked; add to originalAll if not in yet 
               if (partVals != null) {
               	int[] out = nodes[curN].getOutgoingEdges();
               	int zeros = 0; //delay -> stay at same node 
               	for (int k = 0; k < partVals.length; k++) {
               		if (partVals[k] > 0) {
               			//update vals 
               			Edge e = edgeweights.makeEdge(curN, out[k]);
   						double w = edgeweights.getWeight(e);
   						if (this.update && !originalAll.containsKey(e)) {
   							originalAll.put(e, w); 
   						}
   						this.lock(curN, out[k], partVals[k]); 
               			if (out[k] != curT.getDst()) {
               				//more hops
               				next.add(new PartialPath(out[k], partVals[k], 
               						(Vector<Integer>)past.clone(),pp.reality));
               			} else {
               				//destination reached 
               				Vector<Integer> res = (Vector<Integer>)past.clone();
               				res.add(curT.getDst()); 
               				next.add(new PartialPath(out[k], partVals[k], 
               						res,pp.reality));
               			}
               			
               			if (log) {
       		    			System.out.println("add link (" + curT + "," + out[k] + ") with val "+partVals[k]);
       		    		}
               		} else {
               			zeros++;
               		}
               	}
               	if (zeros == partVals.length) {
               		//stay at node itself 
               		next.add(new PartialPath(curN, pp.val, 
       						(Vector<Integer>)past.clone(),pp.reality));
               	}
               } else {
		            //if not next hops found: schedule unlock for failed partial path
            	   this.schedulePath(past, curTime, pp.val, false);
            	   Vector<Integer> res = (Vector<Integer>)past.clone();
      				res.add(-1);
            	   next.add(new PartialPath(-1, pp.val, 
      						res,pp.reality));
               }
           } 
		   //update entry in ongoingTr, add new next event in qTr
		   curT.setTime(curTime + this.linklatency);
		   ongoingTr.put(curT.getNr(),next);
		   qTr.add(curT); 
		 }     
	 }
	 
	 /**
	  * add specific variables 
	  */
	 public void preprocess(Graph g) {
		 super.preprocess(g);
		 this.qTr = new PriorityQueue<ConcurrentTransaction>(); 
		 this.ongoingTr = new HashMap<Integer, Vector<PartialPath>>(); 
		 this.locked = new HashMap<Edge, Double>(); 
		 this.qLocks = new PriorityQueue<ScheduledUnlock>();
	 }
	 
	

	/**
	 * potential minus all locked collateral 
	 */
	public double computePotential(int s, int t) {
		Edge e = new Edge(s,t);
		double v; 
		if (locked.containsKey(e)) {
	        v = locked.get(e);   
		} else {
			v = 0;
		}
		return this.edgeweights.getPot(s, t)-v;
	}
	
	/**
	 * lock v on (s,t)
	 * @param s
	 * @param t
	 * @param v
	 * @return
	 */
	public boolean lock(int s, int t, double v) {
		double max = this.computePotential(s, t);
		if (max < v) {
			System.out.println("s=" + s + " t="+t + " max="+max + " v="+v); 
			return false; 
		}
		//add to already locked collateral
		Edge e = new Edge(s,t); 
		Double locked = this.locked.get(e);
		if (locked == null) {
			locked = 0.0; 
		}
		locked = locked + v;
		if (log) System.out.println("Locked value " + v + "for s=" + s + " t=" + t); 
		this.locked.put(e, locked);
		return true; 
	}
	
	/**
	 * unlock value on edge
	 * @param lock: scheduledlock, SHOULD already have been removed from priority queue before calling this function  
	 */
	public void unlock(ScheduledUnlock lock) {
		//remove from locked 
		if (!locked.containsKey(lock.edge)) {
			System.out.println("WTF"); 
		}
		double locked = this.locked.get(lock.edge);
		locked = locked - lock.val; 
		if (locked < -0.0000001) {
			throw new IllegalArgumentException("less than zero logged collateral " + locked); 
		}
		this.locked.put(lock.edge, locked);
		//update funds 
	    if (lock.success) {	
	       //successful payment -> permanently change potential in both directions 
	    	edgeweights.setWeight(lock.edge.getSrc(),lock.edge.getDst(),lock.val);
	    } else {
	    	//failed payment -> no change 
	    }
	}
	
	/**
	 * unlocking all scheduled locks until time limit
	 * @param limit
	 */
	public void unlockAllUntil(double limit) {
		ScheduledUnlock lock = this.qLocks.peek();
		while (lock != null && lock.time <= limit) {
			if (lock.edge.getSrc() != lock.edge.getDst()) {
			    unlock(lock);
			}
			this.qLocks.poll();
			lock = this.qLocks.peek(); 
		}
	}
	
	/**
	 * schedule locks for all partial paths
	 * @param paths
	 * @param curTime: time at which routing ended  
	 */
	public void scheduleSuccessful(Vector<PartialPath> paths, double curTime) {
		for (int i = 0; i < paths.size(); i++) {
			//get path info
			PartialPath p = paths.get(i); 
			Vector<Integer> vec = p.pre; 
			this.schedulePath(vec, curTime, p.val, true);
		}
	}

    /**
     * 	schedule locks for a not successful routing attempt for paths that *ARE* successful (others done on failure)
     * 
     * assumption: nodes resolve locks immediately after they detect failure
     * sender informs receiver when they receive failure notice 
     * @param vec2
     * @param curTime
     * @param dst
     */
	public void scheduleNotSuccessful(Vector<PartialPath> vec2, double curTime, int dst, int h) {
		//get time by which sender informs receiver 
		int maxFailed = Integer.MAX_VALUE; //maximal length of failed path 
		for (int i = 0; i < vec2.size(); i++) {
			PartialPath p = vec2.get(i); 
			Vector<Integer> vec = p.pre; 
			if (vec.get(vec.size()-1) != dst){
				int l = vec.size()-1; 
				if (l < maxFailed) {
					maxFailed = l;
				}
			}
		}	
		int dstKnows = 2*maxFailed + 1-h; //go along path twice plus sender informs receiver (first h steps already over at curtime)
		curTime = Math.max(curTime, dstKnows); //destination starts resolving locks when it is a) informed and b) paths have reached it
		//schedule locks for all successful partial paths as failures already been done 
		for (int i = 0; i < vec2.size(); i++) {
			PartialPath p = vec2.get(i); 
			Vector<Integer> vec = p.pre; 
			if (vec.get(vec.size()-1) == dst) {
			   this.schedulePath(vec, curTime, p.val, false);
			}   
		}
	}	
	
	
	/**
	 * schedule lock release on partial path 
	 * @param vec
	 * @param curTime
	 */
	public void schedulePath(Vector<Integer> vec, double curTime, double val, boolean succ) {
		double step = curTime + this.linklatency; 
		int t = vec.get(vec.size()-1); 
		for (int j = vec.size()-2; j>= 0; j--) {
			//schedule lock and increase timeout 
			int s = vec.get(j);
			if (s != t && !this.locked.containsKey(new Edge(s,t))) {
				System.out.println("Not contained in locks "+s + "," + t);
				String path = "";
				for (int i = 0; i < vec.size(); i++) {
					path = path + vec.get(i) + " "; 
				}
				System.out.println("Path "+path);
			}
			ScheduledUnlock lock = new ScheduledUnlock(new Edge(s,t), step, succ, val); 
			this.qLocks.add(lock); 
			step = step + this.linklatency; 
			t = s; 
		}
	}
	
	/**
	 * check whether a routing is finished, if so check if it was successful
	 * @param vec
	 * @param dst
	 * @return
	 */
	public boolean[] checkFinal(Vector<PartialPath> vec, int dst) {
		boolean f = true; //final?
		boolean s = true; //successful?
		for (int i = 0; i < vec.size(); i++) {
			Integer last = vec.get(i).node; 
			if (last != dst && last != -1) {
				f = false;
				return new boolean[] {f,s}; 
			} else {
				if (last == -1) {
					s = false; 
				}
			}
		}
		return new boolean[] {f,s};  
	}
	
	/**
	 * return number of hops and messages 
	 * @param vec
	 * @return
	 */
	public int[] getStats(Vector<PartialPath> vec) {
		int h = 0;  //hops
		int x = 0; //messages
		for (int i = 0; i < vec.size(); i++) {
			Vector<Integer> p = vec.get(i).pre;
//			Integer last = p.get(p.size()-1);
			int l = p.size()-1; 
//			if (last == -1) {
//				l--; //not needed as  RoutePayment stats method does it 
//			}
			if (l > h) {
				h = l;
			}
			x = x + l;
		}
		return new int[] {h,x}; 
	}
	

}

