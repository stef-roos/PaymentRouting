package paymentrouting.route.bailout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;
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
import paymentrouting.datasets.TransactionRecord;
import paymentrouting.route.PartialPath;
import paymentrouting.route.PathSelection;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;
import paymentrouting.route.concurrency.ScheduledUnlock;

public class RoutePaymentBailout extends RoutePaymentConcurrent{
	PaymentReaction react;
	LNParams params;
	BailoutFee feeStrategy; 
	double feeFactor; 
	AcceptFee aFeeStrategy; 
	double threshold; 
	double waitingTime; //time after locking when node considers bailout 
	HashMap<Edge, Double> inBailout; //maps a edge to time until bailout completed; delay all other operation on edge  
	int itMC = 10;  
	int bailouts; 
	int bailnot;
	int foundnot; 
	double[] feeGainedBailout; 
	double feeMeanBail;
	double feeMedianBail;
	double feeMinBail;
	double feeMaxBail; 
	double feeQ1Bail;
	double feeQ3Bail;
	double[] bailoutTime;
	double[] bailoutAttTime;
	
	public enum BailoutFee{
		NORMAL, FACTOR, EXPECTED, NEVER  
	}
	public enum AcceptFee{
		ALWAYS, THRESHOLD, EXPECTED  
	}

	public RoutePaymentBailout(PathSelection ps, int trials, double latency, String recordFile, PaymentReaction react, BailoutFee feeS, double fac, 
			AcceptFee a, double thres, double wait) {
		super(ps, trials, latency, recordFile,
				new Parameter[] {new StringParameter("PAYMENT_REACTION", react.getName()), new StringParameter("FEE_STRATEGY_BAILOUT", feeS.name()
						+"_" + fac), new StringParameter("FEE_STRATEGY_ACCEPT", a.name()+(a.equals(AcceptFee.THRESHOLD)?thres:"")),
						new DoubleParameter("WAITING", wait)});
		this.react = react; 
		this.feeStrategy = feeS; 
		this.feeFactor = fac; 
		this.waitingTime = wait; 
		this.aFeeStrategy = a; 
		this.threshold = thres; 
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
	
	private boolean bailout(int node, int pre, int succ, ScheduledUnlock lockPN, ScheduledUnlock lockNS, double curVal) {
		//add bailout attempt
		this.bailoutAttTime[this.getCurT()/1000]++;
		//compute fees
		double valOut = lockNS.getVal(); 
		double fB = this.params.computeFee(new Edge(node, succ), valOut);
		double fA = this.params.computeFee(new Edge(pre, node), valOut+fB);
		double val = this.params.getFeePart(new Edge(pre, node), valOut);
		double fC = valOut-val; 
		if (log) System.out.println("fee A " + fA + " fee B " + fB + " fee C " + fC);
		
		//find potential bailout node
		double minFee = valOut;
		int bailout = -1;
		Node[] nodes = this.graph.getNodes();
		int[] neighPre = nodes[pre].getIncomingEdges();
		boolean found = false; 
		for (int i: neighPre) {
			if (nodes[succ].hasNeighbor(i) && i != node) { //shared neighbor 
				if (log) System.out.println("Found shared neighbor " + i); 
				//check if possible without extra fees 
				if (!this.checkPossible(pre, succ, i, valOut+fB+fC, valOut+fB+fC, node, fB+fA+fC, fB)) {
					continue; 
				}
				if (log) System.out.println("First poss check passed"); 
				double f = this.getFeeD(pre, succ, i, valOut+fB, lockNS.getMaxTime(), curVal); //fee that neighbor charges 
				if (this.checkPossible(pre, succ, i, valOut+fB+f+fC, valOut+fB+fC, node, fB+fA+fC+f, fB)) { //check if possible with fee
					if (log) System.out.println("Second poss check passed");
					found = true; 
					if (f < minFee && this.acceptFee(f, pre, succ, node, val, lockNS.getMaxTime(), curVal)) {
						minFee = f; 
						bailout = i; 
					}
				}
			}
		}
		
		 
		if (bailout == -1) {
			if (found) {
				this.bailnot++;
			} else {
				this.foundnot++; 
			}
			return false;
			
		} else {
			//update metrics for bailout 
			bailouts++; 
			this.feeGainedBailout[bailout] = this.feeGainedBailout[bailout] + minFee; 
			this.bailoutTime[this.getCurT()/1000]++;
			//change locks
			//remove old ones 
			lockPN.setSuccess(false);
			lockNS.setSuccess(false);
			this.qLocks.remove(lockPN); 
			this.qLocks.remove(lockNS); 
			this.unlock(lockPN); 
			this.unlock(lockNS); 
			//locks for new path
			ScheduledUnlock lockPD = new ScheduledUnlock(this.curTime+4*this.linklatency, new Edge(pre,bailout), lockPN.getTime(), lockPN.isSuccess(), val, lockPN.getNr()); 
			ScheduledUnlock lockDS = new ScheduledUnlock(this.curTime+5*this.linklatency, new Edge(bailout,succ), lockNS.getTime(), lockNS.isSuccess(), val, lockNS.getNr());
			lockBailout(lockPD);
			lockBailout(lockDS);
			//locks for fees 
			this.timeAdded = this.timeAdded + 6*this.linklatency; //6 for setting up (2 communication with D, 4 links to setup)
			double step = this.curTime + this.timeAdded + this.linklatency; //immediately resolved 
			ScheduledUnlock feeBA = new ScheduledUnlock(this.curTime+3*this.linklatency, new Edge(node,pre), step, true, fB+fA+fC+minFee, lockPN.getNr());
			step = step + this.linklatency;
			ScheduledUnlock feeAD = new ScheduledUnlock(this.curTime+4*this.linklatency, new Edge(pre, bailout), step, true, fB+fC+minFee, lockPN.getNr());
			step = step + this.linklatency;
			ScheduledUnlock feeDC = new ScheduledUnlock(this.curTime+5*this.linklatency, new Edge(bailout, succ), step, true, fB+fC, lockPN.getNr());
			step = step + this.linklatency;
			ScheduledUnlock feeCB = new ScheduledUnlock(this.curTime+6*this.linklatency, new Edge(succ, node), step, true, fB, lockPN.getNr());
			lockBailout(feeBA); 
			lockBailout(feeAD); 
			lockBailout(feeDC); 
			lockBailout(feeCB); 
			this.timeAdded = this.timeAdded + 4*this.linklatency; //until resolved 
			this.inBailout.put(new Edge(pre,node), this.curTime + this.timeAdded); 
			this.inBailout.put(new Edge(node,succ), this.curTime + this.timeAdded); 
			this.inBailout.put(new Edge(node, pre), this.curTime + this.timeAdded); 
			this.inBailout.put(new Edge(succ,node), this.curTime + this.timeAdded); 
			return true; 
		}
	}
	
	private void lockBailout(ScheduledUnlock lock) {
		this.qLocks.add(lock);
		//add to already locked collateral
		Double locked = this.locked.get(lock.getEdge());
		if (locked == null) {
			locked = 0.0; 
		}
		locked = locked + lock.getVal();
		if (log) System.out.println("Locked value " + lock.getVal() + "for s=" + lock.getEdge().getSrc() + " t=" + lock.getEdge().getDst()); 
		this.locked.put(lock.getEdge(), locked);
	}
	
	@Override 
	public int isSufficientPot(int s, int t, double val, int pre) {
		if (this.feeStrategy == BailoutFee.NEVER) {
			return super.isSufficientPot(s, t, val, pre); 
		}
		//delay if bailout on-going 
		if (log) System.out.println("entering is sufficientpot "); 
		double limit1 = 0; double limit2 = 0; 
		Edge e = new Edge(s,t); 
		if (this.inBailout.containsKey(e)) {
			limit1 = this.inBailout.get(e);
		}	
		Edge eO = new Edge(t,s); 
		if (this.inBailout.containsKey(eO)) {
			limit2 = this.inBailout.get(eO);
		}	
		if (limit1 > this.curTime || limit2 > this.curTime) {
			this.timeAdded = Math.max(limit1, limit2);
			return 0; 
		}
		int a = super.isSufficientPot(s, t, val, pre);
		if (pre == -1) return a; //not a bailout try 
		boolean tried = false;
		if (a == -1) { //check if bailout an option 
			//step 1: check if there are locks on this edge
			if (log) System.out.println("trying bailout at edge " + e.toString()); 
			Double l = this.locked.get(e);
			if (l == null) {
				l = 0.0; 
			}
			if (l + this.computePotential(s, t) >= val) { //collateral locked is sufficient to forward payment 
				//retrieve all locks 
				if (log) System.out.println("enough capacity to try "); 
				Vector<ScheduledUnlock[]> locksEdge = this.getLocks(new Edge(s,t));
				if (log) System.out.println("locks: " + locksEdge.size()); 
				for (int j = 0; j < locksEdge.size(); j++) {
					ScheduledUnlock[] locks = locksEdge.get(j); 
					if (locks[0] != null) { ///need way to determine
						if (log) System.out.println("found match "); 
						tried = tried || this.bailout(s, pre, t, locks[0], locks[1], val); 
					}
				}
			}
			
			
		}
		if (tried) {
			a = 0; 
		}
		return a; 
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
	
	private boolean checkPossible(int pre, int succ, int bailout, double valPBail, double valBailS, int node, double valNP, double valSN) {
		boolean works = (super.isSufficientPot(pre, bailout, valPBail, -1)==1);
		works = works & (super.isSufficientPot(bailout, succ, valBailS, -1)==1); 
		works = works & (super.isSufficientPot(node, pre, valNP, -1)==1); 
		works = works & (super.isSufficientPot(succ, node, valSN, -1)==1); 
		return works; 
	}
	
	private double getFeeD(int pre, int succ, int bailout, double val, double timeout, double curVal) {
		switch (this.feeStrategy) {
		case NORMAL: return this.params.computeFee(new Edge(bailout, succ), val); 
		case FACTOR: return this.feeFactor*this.params.computeFee(new Edge(bailout, succ), val);
		case EXPECTED: return Math.max(this.feeFactor*this.estimateMCFee(pre, succ, bailout, val, true, timeout, curVal), this.params.computeFee(new Edge(bailout, succ), val));
		default: return Double.MAX_VALUE;
		}
	}
	
	private double estimateMCFee(int pre, int succ, int node, double val, boolean add, double timeout, double curVal) {
		Vector<TransactionRecord> recPN = this.getRecordsEdge(pre, node);
		Vector<TransactionRecord> recNS = this.getRecordsEdge(node, succ);
		Vector<TransactionRecord> recNP = this.getRecordsEdge(node, pre);
		Vector<TransactionRecord> recSN = this.getRecordsEdge(succ, node);
		
		//get funds 
		double potPN = this.computePotential(pre, node); 
		double potNS = this.computePotential(node, succ);
		double potNP = this.computePotential(node, pre);
		double potSN = this.computePotential(succ, node);
//		if (!add) {
//			//pre has already accepted current transaction, will be reset if not forwarded 
//			potPN = potPN + curVal;
//			potNP = potNP - curVal; 
//		}
		//adjust funds to different cases
		//first value: less locked collateral
		double feeNoLock; 
		if (add) {
			//case: D is asked to lock but has not, use current state  
			feeNoLock = this.doMCSimulationFee(pre, succ, node, timeout, recPN, recNS, recNP, recSN, potPN, potNS, potNP, potSN, rand);
		} else {
			//case: B unlocks the value 
			if (curVal <= potNS+val) {
				//current payment will be forwarded
				HashMap<Integer, ScheduledUnlock> locks = this.preScheduled.get(new Edge(node, succ)); 
				if (locks == null) {
					locks = new HashMap<Integer, ScheduledUnlock>();
					this.preScheduled.put(new Edge(node, succ), locks);
				}
				ScheduledUnlock newLock = new ScheduledUnlock(this.curTime, new Edge(node, succ), val, this.getCurT(), timeout); 
				locks.put(this.getCurT(), newLock); 
			    feeNoLock = this.doMCSimulationFee(pre, succ, node, timeout, recPN, recNS, recNP, recSN, 
					potPN+val+this.params.computeFee(new Edge(node,succ), val), potNS+val-curVal, potNP, potSN, rand);
			    locks.remove(this.getCurT());			   
			} else {
				//current payment will be canceled 
				double curValFee = this.params.computeFee(new Edge(node,succ), curVal);
				feeNoLock = this.doMCSimulationFee(pre, succ, node, timeout, recPN, recNS, recNP, recSN, 
						potPN+curVal-curValFee, potNS+val, potNP-curVal+
						curValFee, potSN, rand);
			}
		}
		//second value: include payment 
		double feeLock;
		if (add) {
			//D locks the value; assumption: locked until max  
			feeLock = this.doMCSimulationFee(pre, succ, node, timeout, recPN, recNS, recNP, recSN, potPN-val, potNS-val, potNP, potSN, rand);
		} else {
			//B keeps lock, current payment will be canceled 
			double curValFee = this.params.computeFee(new Edge(node,succ), curVal);
			feeLock = this.doMCSimulationFee(pre, succ, node, timeout, recPN, recNS, recNP, recSN, 
					potPN+curVal-curValFee, potNS, potNP-curVal+
					curValFee, potSN, rand);
		}
		return feeNoLock-feeLock; 
	}
	
	private Vector<TransactionRecord> getRecordsEdge(int s, int t){
		Vector<TransactionRecord> vec = new Vector<TransactionRecord>();
		Iterator<TransactionRecord> it = this.records.get(s).values().iterator();
		//check if transaction forwarded to t and if finished 
		while (it.hasNext()) {
			TransactionRecord record = it.next();
			if (record.getSucc() == t && record.getEndT() != Double.MAX_VALUE && record.getInterval() != -1 ) {
				vec.add(record);
			}
		}
		return vec; 
	}
	
	private HashMap<Integer, double[]> getAllSimScores(Edge e, Vector<TransactionRecord> vec){
		HashMap<Integer, double[]> map = new HashMap<Integer, double[]>(); 
		if (!this.preScheduled.containsKey(e)) {
			return map; 
		}
		Iterator<ScheduledUnlock> preSL = this.preScheduled.get(e).values().iterator();
		while (preSL.hasNext()) {
			ScheduledUnlock lock = preSL.next(); 
			double[] scores = this.simScore(vec, lock.getVal(), this.curTime-lock.getStartTime()); 
			map.put(lock.getNr(), scores);
		}
		return map; 
	}
	
	private double[] simScore(Vector<TransactionRecord> vec, double val, double minDur) {
		double[] score = new double[vec.size()];
		double sum = 0;
		for (int i = 0; i < vec.size(); i++) {
			TransactionRecord tr = vec.get(i); 
			if (tr.getDuration() < minDur) continue; 
			double valFac = Math.min(tr.getVal(), val)/Math.max(tr.getVal(), val); 
			score[i] = valFac;
			sum = sum + score[i];
		}
		
		if (sum == 0) {
			return null; 
		}
		for (int i = 0; i < score.length; i++) {
			score[i] = score[i]/sum;
		}
		
		return score; 
	}
	
	private int selectProp(double[] probs, Random rand) {
		double r = rand.nextDouble();
		int i = 0;
		double s = probs[0]; 
		while (i < probs.length-1 && s < r) {
			i++;
			s = s + probs[i];
		}
		return i; 
	}
	
	/**
	 * estimate expected fees based on x runs 
	 * @param pre
	 * @param succ
	 * @param node
	 * @param val
	 * @param lockP
	 * @param lockS
	 * @param remove
	 * @param maxTime
	 * @param recPre
	 * @param recSucc
	 * @return
	 */
    private double doMCSimulationFee(int pre, int succ, int node, double maxTime, 
    		Vector<TransactionRecord> recPN, Vector<TransactionRecord> recNS, Vector<TransactionRecord> recNP, Vector<TransactionRecord> recSN,
    		double potPN, double potNS, double potNP, double potSN, Random rand) {
    	Edge ePN = new Edge(pre, node); 
    	Edge eNS = new Edge(node, succ); 
    	Edge eNP = new Edge(node, pre); 
    	Edge eSN = new Edge(node, succ);
    	Edge[] edges = {ePN, eNS, eNP, eSN}; 
    	//pre-compute scores for selecting a transaction in simulation
        HashMap<Integer, double[]> mapPN = this.getAllSimScores(ePN, recPN);
        HashMap<Integer, double[]> mapNS = this.getAllSimScores(ePN, recNS);
        HashMap<Integer, double[]> mapNP = this.getAllSimScores(ePN, recNP);
        HashMap<Integer, double[]> mapSN = this.getAllSimScores(ePN, recSN);
		double feeAv = 0;
		for (int i = 0; i < this.itMC; i++) {
			double fRun = 0;
			double potPNRun = potPN; 
			double potNSRun = potNS;
			double potNPRun = potNP; 
			double potSNRun = potSN; 
			double[] pots = {potPNRun, potNSRun, potNPRun, potSNRun}; 
			//generate queue 
			PriorityQueue<LockChange> changes = new PriorityQueue<LockChange>();
			//add end times for locked tx 
			this.addOngoingToPQ(changes, ePN, recPN, mapPN);
			this.addOngoingToPQ(changes, eNS, recNS, mapNS);
			this.addOngoingToPQ(changes, eNP, recNP, mapNP);
			this.addOngoingToPQ(changes, eSN, recSN, mapSN);
			this.addNewtoPQ(changes, ePN, recPN, maxTime, rand);
			this.addNewtoPQ(changes, eNS, recNS, maxTime, rand);
			this.addNewtoPQ(changes, eNP, recNP, maxTime, rand);
			this.addNewtoPQ(changes, eSN, recSN, maxTime, rand);
			//execute changes
			double t = this.curTime; 
			while (t < maxTime && !changes.isEmpty()) {
				LockChange lc = changes.poll();
				t = lc.time;
				if (lc.lock) {
					for (int j = 0; j < 4; j++) {
					if (lc.edge.equals(edges[j])) {
						if (lc.val <= pots[j]) {
							//payment can be executed, hence value is locked  
							pots[j] = pots[j] - lc.val;
							//re-enter for unlock
							lc.changetoUnlock();
							changes.add(lc); 
						}
					}
					}
				} else {
					for (int j = 0; j < 4; j++) {
					if (lc.edge.equals(edges[j])) {
						if (lc.success) {
							//payment successful, value added to opposite direction
							int index = (j+2)%4; 
							pots[index] = pots[index] + lc.val; 
							//if node is sending party, it receives fee
							if (j==1 || j==2) {
								fRun = fRun + this.params.computeFee(edges[j], lc.val);
							}
						} else {
							//payment fails, return collateral 
							pots[j] = pots[j] + lc.val;
						}
					}
					}					
				}
			}
			
			
			feeAv = feeAv + fRun; 
		}
		feeAv = feeAv/this.itMC;
		return feeAv; 
	}
    
    private void addOngoingToPQ(PriorityQueue<LockChange> changes, Edge e, Vector<TransactionRecord> vec,  HashMap<Integer, double[]> scores) {
    	if (!this.preScheduled.containsKey(e)) return ; //no locks to take care of 
    	Iterator<ScheduledUnlock> preSL = this.preScheduled.get(e).values().iterator();
		while (preSL.hasNext()) {
			ScheduledUnlock lock = preSL.next();
			if (lock.getNr() == this.getCurT()) continue; //ongoing tx handled above 
			double[] probs = scores.get(lock.getNr()); 
			if (probs != null) {
			   int trIndex = this.selectProp(probs, rand);
			   TransactionRecord record = vec.get(trIndex);
			   LockChange ch = new LockChange(lock.getTime() + record.getDuration(), lock.getVal(), e, false, record.isSuccess(),0); 
               changes.add(ch); 
			} else {
				LockChange ch = new LockChange(lock.getMaxTime(), lock.getVal(), e, false, false,0); 
	               changes.add(ch);
			}
		}
    }
    
    private void addNewtoPQ(PriorityQueue<LockChange> changes, Edge e, Vector<TransactionRecord> vec, double timelimit, Random rand) {
    	double t = this.curTime;
    	if (vec.size() > 0) {
    	while (t < timelimit) {
    		TransactionRecord r = vec.get(rand.nextInt(vec.size()));
    		t = t + r.getInterval(); 
    		LockChange lockSt = new LockChange(t, r.getVal(), e, true, r.isSuccess(), r.getDuration());
    		changes.add(lockSt); 
    	}
    	}
    }
    
    private boolean acceptFee(double feeOffer,int pre, int succ, int node, double val, double timeout, double curVal) {
		switch (this.aFeeStrategy) {
		case ALWAYS: return true; 
		case THRESHOLD: return (feeOffer <= this.threshold);
		case EXPECTED: return (feeOffer <= this.estimateMCFee(pre, succ, node, val, false, timeout, curVal));
		default: return false;
		}
	}
    
	
	public void preprocess(Graph g) {
	     super.preprocess(g);
	     this.params = (LNParams) (g.getProperty("LN_PARAMS"));
	     this.bailouts = 0;
	     this.bailnot = 0;
	     this.foundnot = 0; 
	     this.feeGainedBailout = new double[g.getNodeCount()];
	     this.react.init(g, rand);
	     this.inBailout = new HashMap<Edge, Double>(); 
	     this.bailoutTime = new double[(int)Math.ceil(this.transactions.length/1000)];
	     this.bailoutAttTime = new double[(int)Math.ceil(this.transactions.length/1000)];
	}
	
	@Override
	public Single[] getSingles() {
		Single[] singles = super.getSingles();
		Single[] allSingle = new Single[singles.length+10];
		for (int i = 0; i < singles.length; i++) {
			allSingle[i] = singles[i]; 
		}
		
		Single f1 = new Single(this.key + "_FEE_BAIL_AV", this.feeMeanBail);
		Single f2 = new Single(this.key + "_FEE_BAIL_MED", this.feeMedianBail);
		Single f3 = new Single(this.key + "_FEE_BAIL_Q1", this.feeQ1Bail);
		Single f4 = new Single(this.key + "_FEE_BAIL_Q3", this.feeQ3Bail);
		Single f5 = new Single(this.key + "_FEE_BAIL_MIN", this.feeMinBail);
		Single f6 = new Single(this.key + "_FEE_BAIL_MAX", this.feeMaxBail);
		Single b = new Single(this.key + "_BAILOUTS", this.bailouts);
		Single bfn = new Single(this.key + "_BAILOUT_NOT_ACCEPTED", this.bailnot);
		Single bnf = new Single(this.key + "_BAILOUT_NOT_FOUND", this.foundnot);
		Single ball = new Single(this.key + "_BAILOUT_ATTEMPTS", this.foundnot + this.bailnot + this.bailouts);
		int index = singles.length;
		allSingle[index++] = f1; 
		allSingle[index++] = f2; 
		allSingle[index++] = f3; 
		allSingle[index++] = f4; 
		allSingle[index++] = f5; 
		allSingle[index++] = f6;
		allSingle[index++] = b; 
		allSingle[index++] = bfn;
		allSingle[index++] = bnf;
		allSingle[index++] = ball; 

		return allSingle; 
	}
	
	@Override
	public boolean writeData(String folder) {
		boolean succ = super.writeData(folder);
		succ &= DataWriter.writeWithIndex(this.bailoutAttTime,
				this.key+"_ATTEMPTED_BAILOUT_TIME", folder);
		succ &= DataWriter.writeWithIndex(this.bailoutTime,
				this.key+"_BAILOUT_TIME", folder);
	}
	
	@Override 
	public void unlock(ScheduledUnlock lock) {
		super.unlock(lock);
		
		//fee part
	    int s = lock.getEdge().getSrc();
	    if (this.transactions[lock.getNr()].getSrc() != s) { //source does not take fees 
	    	double fee = this.params.computeFee(lock.getEdge(), lock.getVal()); 
	    	this.feeGained[s] = this.feeGained[s] + fee; 
	    }
	}
	
	@Override
	public void postprocess() {
		//compute final stats
		super.postprocess();
		//bailout fee
		Arrays.parallelSort(this.feeGainedBailout);
		this.feeMedianBail = this.feeGainedBailout[Math.floorDiv(this.feeGainedBailout.length, 2)]; 
		this.feeQ1Bail = this.feeGainedBailout[Math.floorDiv(this.feeGainedBailout.length, 4)]; 
		this.feeQ3Bail = this.feeGainedBailout[Math.floorDiv(3*this.feeGainedBailout.length, 4)]; 
		this.feeMinBail = this.feeGainedBailout[0];
		this.feeMaxBail = this.feeGainedBailout[this.feeGainedBailout.length-1]; 
		this.feeMeanBail = 0;
		for (int i = 0; i < this.feeGainedBailout.length; i++) {
			this.feeMeanBail = this.feeMeanBail + this.feeGainedBailout[i];
		}
		this.feeMeanBail = this.feeMeanBail/this.feeGainedBailout.length; 
	}
	
	
	
	private class LockChange implements Comparable<LockChange>{
		double time;
		double duration; 
		double val;
		Edge edge;
		boolean lock; 
		boolean success; 
		
		public LockChange(double t, double v, Edge e, boolean l, boolean s, double d) {
			this.time = t;
			this.val = v;
			this.edge = e;
			this.lock = l; 
			this.success = s; 
			this.duration = d; 
		}
		
		public LockChange(double t, double v, Edge e, boolean l) {
			this.time = t;
			this.val = v;
			this.edge = e;
			this.lock = l; 
			this.success = false; 
		}
		
		private void changetoUnlock() {
			this.time = this.time + this.duration;
			this.lock = false; 
		}

		@Override
		public int compareTo(LockChange o) {
			return (int) Math.signum(this.time-o.time); 
		}
		
	}

}
