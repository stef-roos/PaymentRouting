package paymentrouting.route.concurrency;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Vector;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.Filewriter;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.Parameter;
import paymentrouting.datasets.TransactionRecord;
import paymentrouting.route.PartialPath;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;
import treeembedding.credit.Transaction;

public class RoutePaymentConcurrent extends RoutePayment {
	protected double linklatency; //link latency in ms
	//double now = 0; 
	int curT; 
    protected PriorityQueue<ConcurrentTransaction> qTr; 
	protected HashMap<Integer, Vector<PartialPath>> ongoingTr; 
	protected HashMap<Edge, Double> locked; 
	protected PriorityQueue<ScheduledUnlock> qLocks;
	protected HashMap<Integer,HashMap<Integer,ScheduledUnlock>> preScheduled;
	protected double curTime; 
	protected double timeAdded=0; 
	String rec; 
	protected HashMap<Integer, HashMap<Integer,TransactionRecord>> records;
	
	
	
	
	public RoutePaymentConcurrent(PathSelection ps, int trials, double latency, String recordFile) {
		this(ps,trials,Integer.MAX_VALUE, latency, recordFile); 
	}
	

	public RoutePaymentConcurrent(PathSelection ps, int trials, int epoch, double latency, String recordFile) {
		super(ps, trials, true, epoch, new Parameter[]{new DoubleParameter("LINK_LATENCY", latency)});
		this.linklatency = latency; 
		this.rec = recordFile; 
	}
	
	public RoutePaymentConcurrent(PathSelection ps, int trials, double latency) {
		this(ps,trials,Integer.MAX_VALUE, latency, null); 
	}
	

	public RoutePaymentConcurrent(PathSelection ps, int trials, int epoch, double latency) {
		this(ps,trials, epoch, latency, null); 
	}
	
	 public void run(Graph g) {
		 Node[] nodes = g.getNodes();
		 //Add transactions into queue
		 for (int i = 0; i < this.transactions.length; i++) {
			 Transaction t = this.transactions[i]; 
			 ConcurrentTransaction ct = new ConcurrentTransaction(i,t); 
			 qTr.add(ct); 
		 }
		 this.curTime = 0; 
		 boolean[] excluded = new boolean[nodes.length]; 
		 if (this.rec != null) {
			 this.initRecording(nodes.length);
		 }
		 
		 //loop until all transactions are processed 
		 while (!qTr.isEmpty()) {
		   //take a transaction, process all unlocks before, check if final or new (final for all paths = last node receiver or -1 indicating failure)
		   ConcurrentTransaction cur = qTr.poll();
		   this.curT = cur.getNr(); 
		   curTime = cur.getTime(); 
		   this.unlockAllUntil(curTime);
		   Vector<PartialPath> vec = this.ongoingTr.get(cur.getNr()); 
		   if (vec != null) {
		      boolean[] state = this.checkFinal(vec, cur.getDst());
		      if (state[0]) {
		    	  //done 
		    	  //if final: schedule + compute stats 
		    	  int[] stats = this.getStats(vec); 
		    	  transStats(state[1],stats[0],stats[1],cur.getNr(),0); 
		    	  //schedule unlocks
		    	  if (state[1]) {
		    		  this.scheduleSuccessful(vec, curTime);
		    	  } else {
		    		  this.scheduleNotSuccessful(vec, curTime, cur.getDst(),stats[0]);
		    	  }
		    	  continue; //go to next transaction 
		      } else {
		    	  //non-final: main logic done below  
		      }
		   } else {
			   //add new transaction to ongoing 
			   vec = new Vector<PartialPath>();
			   double[] splitVal = this.splitRealities(cur.getVal(), select.getDist().getStartR(), rand);
		    	for (int a = 0; a < select.getDist().getStartR(); a++) {
		    	     vec.add(new PartialPath(cur.getSrc(), splitVal[a],new Vector<Integer>(),a));
		        }
			   ongoingTr.put(cur.getNr(),vec); 
		   }
		    //if not final:  iterate over all non-final paths 
		   Vector<PartialPath> next = new  Vector<PartialPath>();
		   for (int i = 0; i < vec.size(); i++) {
			   //retrieve partial path and get next hops 
			   PartialPath pp = vec.get(i);
               int curN = pp.node;
               //finished path -> no more steps
               if (curN == -1 || curN == cur.getDst()) {
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
           	
           	   if (log) System.out.println("Routing at cur " + curN); 
               double[] partVals;
               if (this.agree(curN, pp.val, curTime)) {
                partVals = this.select.getNextsVals(g, curN, cur.getDst(), 
               		pre, excluded, this, pp.val, rand, pp.reality); 
               } else {
            	   partVals = new double[nodes[curN].getOutDegree()]; 
               }
               for (int l = 0; l < past.size(); l++) {
           		excluded[past.get(l)] = false;
           	   }
               past.add(curN);
               
               //recording
               if (this.rec != null && partVals != null) {
            	   this.addInitRecord(nodes, curN, partVals, pp.val, pre, curTime);
               }
		       //if next hops found: get next hops and add to locked; add to originalAll if not in yet 
               if (partVals != null) {
            	   if (this.timeAdded != 0) {
            		   //we need to delay
            		   partVals = new double[partVals.length];
            	   }
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
   						this.lock(curN, out[k], partVals[k], cur.nr); 
               			if (out[k] != cur.getDst()) {
               				//more hops
               				next.add(new PartialPath(out[k], partVals[k], 
               						(Vector<Integer>)past.clone(),pp.reality));
               			} else {
               				//destination reached 
               				Vector<Integer> res = (Vector<Integer>)past.clone();
               				res.add(cur.getDst()); 
               				next.add(new PartialPath(out[k], partVals[k], 
               						res,pp.reality));
               			}
               			
               			if (log) {
       		    			System.out.println("add link (" + cur + "," + out[k] + ") with val "+partVals[k]);
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
		   cur.setTime(curTime + this.linklatency+this.timeAdded);
		   this.timeAdded = 0; 
		   ongoingTr.put(cur.getNr(),next);
		   qTr.add(cur); 
		 }     
	 }
	 
	 /**
	  * allows curN to refuse to forward tx of value val at time curTime;
	  * true for this class, can be implemented differently in subclass 
	  * @param curN
	  * @param val
	  * @param curTime
	  * @return
	  */
	 protected boolean agree(int curN, double val, double curTime) {
		return true;
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
		 preScheduled = new HashMap<Integer,HashMap<Integer,ScheduledUnlock>>();
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
	public boolean lock(int s, int t, double v, int nr) {
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
		
		//add to hashmap 
		HashMap<Integer, ScheduledUnlock> map = this.preScheduled.get(s);
		if (map == null) {
			map = new HashMap<Integer, ScheduledUnlock>();
			this.preScheduled.put(s, map); 
		}
		map.put(nr, new ScheduledUnlock(new Edge(s,t), v, nr)); 
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
	    
	    //add record if file provided 
	    if (this.records != null) {
	    	this.finishRecord(lock, lock.time);
	    }
	}
	
	/**
	 * unlocking all scheduled locks until time limit
	 * @param limit
	 */
	public void unlockAllUntil(double limit) {
		ScheduledUnlock lock = this.qLocks.peek();
		while (lock != null && lock.time <= limit) {
			HashMap<Integer, ScheduledUnlock> map = this.preScheduled.get(lock.edge.getSrc());
			if (map != null) {
				map.remove(lock.nr);
			}
			unlock(lock);
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
		double step = curTime; 
		int t = vec.get(vec.size()-1); 
		for (int j = vec.size()-2; j>= 0; j--) {
			//schedule lock and increase timeout 
			int s = vec.get(j);
			if (!this.locked.containsKey(new Edge(s,t))) {
				System.out.println("Not contained in locks "+s + "," + t);
				String path = "";
				for (int i = 0; i < vec.size(); i++) {
					path = path + vec.get(i) + " "; 
				}
				System.out.println("Path "+path);
			}
			Edge e = new Edge(s,t); 
			step = step + this.getTimeToUnlock(step, e, succ, val); 
			//retrieve lock 
			HashMap<Integer, ScheduledUnlock> map = this.preScheduled.get(s);
			ScheduledUnlock lock = null;
			if (map != null) {
				lock = map.get(this.curT);
			}
			if (lock != null) {
				lock.finalize(step, succ);
			} else {
			    lock = new ScheduledUnlock(e, step, succ, val, this.curT); 
			}    
			this.qLocks.add(lock); 
			t = s; 
		}
	}
	
	public double getTimeToUnlock(double timeBefore, Edge e, boolean succ, double val) {
		return this.linklatency; 
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
	

	public int getCurT() {
		return curT;
	}


	public void setCurT(int curT) {
		this.curT = curT;
	}

	protected void initRecording(int nodes) {
		this.records = new HashMap<Integer,HashMap<Integer,TransactionRecord>>();
		for (int i = 0; i < nodes; i++) {
			this.records.put(i, new HashMap<Integer,TransactionRecord>()); 
		}
	}
	
	protected void addInitRecord(Node[] nodes, int node, double[] vals, double val, int pre, double time) {
		HashMap<Integer,TransactionRecord> vec = this.records.get(node);
		int[] out = nodes[node].getOutgoingEdges();
		//find succ(s)
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] > 0) {
				int succ = out[i];
				TransactionRecord r = new TransactionRecord(time, val, pre, succ);
				vec.put(this.curT, r);
			}
		}
	}
	
	protected void finishRecord(ScheduledUnlock lock, double time) {
		int node = lock.edge.getSrc();
		HashMap<Integer,TransactionRecord> record = this.records.get(node);
		TransactionRecord tr = record.get(lock.nr);
		tr.setEndT(time);
		tr.setSuuccess(lock.success);
		 
		
	}
	
	@Override
	public void postprocess() {
		super.postprocess();
		if (this.rec != null) {
			this.writeRecords();
		}
	}
	
	protected void writeRecords() {
		
			Filewriter fw = new Filewriter(this.rec);
			int i = 0;
			while (this.records.containsKey(i)) {
				HashMap<Integer,TransactionRecord> record = this.records.get(i);
				fw.writeln("node " + i);
				Iterator<TransactionRecord> it = record.values().iterator();
				while (it.hasNext()) {
					TransactionRecord tr = it.next();
					fw.writeln(tr.toString()); 
				}
				i++;
			}
			fw.close();
		
	}
	
}

