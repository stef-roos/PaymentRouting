package paymentrouting.route.mimo;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import gtna.data.Single;
import gtna.graph.Edge;
import gtna.graph.Graph;
import paymentrouting.route.PartialPath;
import paymentrouting.route.PathSelection;
import paymentrouting.route.concurrency.ConcurrentTransaction;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;
import paymentrouting.route.concurrency.ScheduledUnlock;

public class RoutePaymentMimo extends RoutePaymentConcurrent {
	ConcurrentTransaction[][] succParts; //parts of orbit that have succeeded up to now 
	double[] failTime; //time atomic payment failed, -1 if successful, 0 if not known yet if successful
	AtomicMapping atomic; //map each mimo transaction to atomic payments 
	MimoMapping originalAtomic; //map original payments to atomic payments 
	boolean[] processed; 
	double coll = 0; //overall collaterral locked
	double collSuccess = 0; //locked for successful payments 
	double allSucc = 0; 
	double atomicSucc = 0; //fraction successful atomic segment payments
	double atomicSuccNonSingle = 0; //fraction successful atomic segment payments, at least 2 payments 
	int atomicNonSingle = 0; //number of segments with at least two segments 
	double originalSucc = 0; //fraction of payments for which all payment parts succeed
	double originalFracSucc = 0; //average fraction of payment that is successful 
	HashMap<Integer, Vector<PartialPath>> inwaiting; //vectors of partial payments terminated 

	public RoutePaymentMimo(PathSelection ps, int trials, double latency) {
		super(ps, trials, latency);
	}
	
	public void preprocess(Graph g) {
		super.preprocess(g);
		this.atomic = (AtomicMapping)g.getProperty("ATOMIC_MAPPING"); 
		this.succParts = new ConcurrentTransaction[atomic.atomicSet.size()][]; 
		this.failTime = new double[atomic.atomicSet.size()];
		this.processed = new boolean[this.transactions.length]; 
		this.originalAtomic = (MimoMapping)g.getProperty("MIMO_MAPPING"); 
		this.inwaiting = new HashMap<Integer, Vector<PartialPath>>(); 
	}
	
	public boolean[] checkFinal(Vector<PartialPath> vec, int dst, ConcurrentTransaction cur) {
		if (this.processed[this.curT]) {
			//already been processed 
			int set = this.atomic.atomicsetIndex.get(this.curT);
			boolean[] res = new boolean[2];
			res[0] = true;
			//all atomic payment successul 
			if (this.failTime[set] == -1) {
				res[1] = true;
			}
			return res; 
		}
		//call original checkFinal 
		boolean[] res = super.checkFinal(vec, dst,cur);
		if (res[0]) { //if we are done (otherwise just continue as without mimo) 
			//add new transaction to successful transactions for the orbit 
		  int set = this.atomic.atomicsetIndex.get(this.curT);
		  int[] allTx = this.atomic.atomicSet.get(set); 
		  if (this.succParts[set] == null) {
			  this.succParts[set] = new ConcurrentTransaction[allTx.length];
			  if (this.succParts[set].length > 1) {
				  this.atomicNonSingle++; 
			  }
		  }
		  if (!res[1]) {
			  //failed, others need to also be failed
			  this.failTime[set] = this.curTime; //set time at which failed, other wills only learn linklatency later
			  for (int i = 0; i < this.succParts[set].length; i++) {
				  if (this.succParts[set][i] != null) {
					  //transaction already succeeded, can now be failed due to atomicity; 
					  //add to queue to resolve timelocks after linklatency delay to inform nodes of failure 
					  this.succParts[set][i].setTime(curTime + this.linklatency); 
					  Vector<PartialPath> v = this.inwaiting.remove(this.succParts[set][i].getNr()); 
					   ongoingTr.put(this.succParts[set][i].getNr(),v); 
					   qTr.add(this.succParts[set][i]);
				  }
			  }   
		 } else {//payment successful
			 // option A: payment already failed 
			 if (this.failTime[set] > 0) {
				//will be failed in scheduling
				 return res; 
			 }
			 // option B: not failed yet
			 //add to successful segments 
			 int index = -1; 
			 for (int i = 0; i < allTx.length; i++) {
				 if (allTx[i] == cur.getNr()) {
					 index = i;
				 }
			 }
			 this.succParts[set][index] = cur;
			 //check if last payment to succeed
			 boolean done = true;
			 for (int i = 0; i < this.succParts[set].length; i++) {
				  if (this.succParts[set][i] == null) {
					   done = false;
				  }
			 }	  
			 //if done => all successful, atomic payment successful 
			 if (done) {
				 this.failTime[set] = -1; 
			    for (int i = 0; i < this.succParts[set].length; i++) { //queue for scheduling timelocks
			    	if (i == index) continue; 
				   this.succParts[set][i].setTime(curTime + this.linklatency); 
				   Vector<PartialPath> v = this.inwaiting.remove(this.succParts[set][i].getNr());
				   ongoingTr.put(this.succParts[set][i].getNr(),v); 
				   qTr.add(this.succParts[set][i]);
			    }
			    this.atomicSucc++;
			    this.allSucc = this.allSucc + allTx.length; 
			    if (allTx.length > 1) {
			    	this.atomicSuccNonSingle++; 
			    }
			 } else {
				 //add to waiting until decided whether orbit/segment fails/succeeeds  
				 this.inwaiting.put(this.curT, vec); 
			 }
			 
		 }
		 	  
		}	  
		return res;
	}
	
	public void transStats(boolean s, int h, int x, int i, int t) {
		if (this.processed[i]) {
			//already done 
			return;
		}
		super.transStats(s, h, x, i, t);
		this.processed[i] = true; 
	}
	
	public void scheduleSuccessful(Vector<PartialPath> paths, double curTime) {
		//only schedule once all partial payments done
		int set = this.atomic.atomicsetIndex.get(this.curT);
		if (this.failTime[set] == -1) { //successful 
			super.scheduleSuccessful(paths, curTime);
		} else {
			if (this.failTime[set] > 0) { //atomic payment failed even if this one succeeded 
				super.scheduleNotSuccessful(paths, curTime, paths.get(0).node, paths.get(0).pre.size());
			}
		}
	}
	
	public void schedulePath(Vector<Integer> vec, double curTime, double val, boolean succ) {
		//as for super class but for collateral stats 
		double step = curTime; 
		int t = vec.get(vec.size()-1); 
		for (int j = vec.size()-2; j>= 0; j--) {
			//schedule lock and increase timeout 
			int s = vec.get(j);
			if (s == t) continue; 
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
			if (log) System.out.println("Step " + step); 
			//retrieve lock 
			HashMap<Integer, ScheduledUnlock> map = this.preScheduled.get(e);
			ScheduledUnlock lock = null;
			if (map != null) {
				lock = map.get(this.curT);
			}
			if (lock != null) {
				lock.finalize(step, succ);
				
			} else {
			    lock = new ScheduledUnlock(this.curTime, e, step, succ, val, this.curT); 
			}    
			this.coll = this.coll + lock.getVal(); //payment val was locked 
			if (succ) {
				this.collSuccess = this.collSuccess  + lock.getVal(); //was locked & succeess  
			}
			this.qLocks.add(lock); 
			t = s; 
		}
	}
	
	public void postprocess() {
		super.postprocess();
		//divide success probs 
		this.allSucc = this.allSucc/(double)this.transactions.length;
		this.atomicSucc = this.atomicSucc/(double)this.failTime.length;
		this.atomicSuccNonSingle = this.atomicSuccNonSingle/(double)this.atomicNonSingle;
		//check original transactions
		Set<Integer> mapping = this.originalAtomic.segmentMapping.keySet();
		Iterator<Integer> it = mapping.iterator();
		while (it.hasNext()) {
			int tx = it.next();
			int[] ats = this.originalAtomic.getSegmentIds(tx);
			boolean done = true;
			double succVal = 0;
			double allVal = 0;
			for (int i = 0; i < ats.length; i++) {
				allVal = allVal + this.transactions[ats[i]].getVal();
				if (this.failTime[ats[i]] == -1) {
					succVal= succVal + this.transactions[ats[i]].getVal();
				} else {
					done = false;
				}
			}
			succVal = succVal/allVal; 
			if (done) {
				this.originalSucc++;
			}
			this.originalFracSucc = this.originalFracSucc + succVal; 
		}
		this.originalSucc = this.originalSucc/(double)mapping.size();
		this.originalFracSucc = this.originalFracSucc/(double)mapping.size();
	}

	public Single[] getSingles() {
		Single[] singles1 = super.getSingles();
		Single[] singleNew = new Single[singles1.length + 7];
		for (int i = 0; i < singles1.length; i++) {
			singleNew[i] = singles1[i];
		}
		int index = singles1.length;
		singleNew[index++] = new Single(this.key + "_COLLATERAL", this.coll); 
		singleNew[index++] = new Single(this.key + "_COLLATERAL_SUCC", this.collSuccess); 
		singleNew[index++] = new Single(this.key + "_SUCC_FINAL", this.allSucc); 
		singleNew[index++] = new Single(this.key + "_SUCC_ATOMIC", this.atomicSucc);
		singleNew[index++] = new Single(this.key + "_SUCC_ATOMIC_NONSINGLE", this.atomicSuccNonSingle);
		singleNew[index++] = new Single(this.key + "_SUCC_ORIGINAL", this.originalSucc);
		singleNew[index++] = new Single(this.key + "_SUCC_ORIGINAL_FRAC", this.originalFracSucc);
		
		return singleNew; 
	}
}
