package paymentrouting.route.bailout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import paymentrouting.datasets.LNParams;
import paymentrouting.route.PartialPath;
import paymentrouting.route.PathSelection;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;
import paymentrouting.route.concurrency.ScheduledUnlock;

public class RoutePaymentBailout extends RoutePaymentConcurrent{
	PaymentReaction react;
	LNParams params;
	BailoutFee feeStrategy; 
	double feeFactor; 
	double waitingTime; //time after locking when node considers bailout 
	
	public enum BailoutFee{
		NORMAL, FACTOR, EXPECTED  
	}

	public RoutePaymentBailout(PathSelection ps, int trials, double latency, PaymentReaction react, BailoutFee feeS, double fac, double wait) {
		super(ps, trials, latency);
		this.react = react; 
		this.feeStrategy = feeS; 
		this.feeFactor = fac; 
		this.waitingTime = wait; 
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
	
	private boolean bailout(int node, int pre, int succ, ScheduledUnlock lockPN, ScheduledUnlock lockNS) {
		//compute fees
		double valOut = lockNS.getVal(); 
		double fB = this.params.computeFee(new Edge(node, succ), valOut);
		double fA = this.params.computeFee(new Edge(pre, node), valOut+fB);
		double fC = this.params.getFeePart(new Edge(pre, node), valOut);
		double val = valOut-fC; 
		
		//find potential bailout node
		double minFee = valOut;
		int bailout = -1;
		Node[] nodes = this.graph.getNodes();
		int[] neighPre = nodes[pre].getIncomingEdges();
		for (int i: neighPre) {
			if (nodes[succ].hasNeighbor(i)) { //shared neighbor 
				double f = this.getFeeD(pre, succ, i, valOut+fB, lockNS.getTime() - this.curTime); //fee that neighbor charges 
				if (this.checkPossible(pre, succ, i, valOut+fB+f+fC, valOut+fB+fC, node, fB+fA+fC+f, fB)) { //check if possible 
					if (f < minFee) {
						minFee = f; 
						bailout = i; 
					}
				}
			}
		}
		
		 
		if (bailout == -1) {
			return false;
		} else {
			//change locks
			//remove old ones 
			this.qLocks.remove(lockPN); 
			this.qLocks.remove(lockNS); 
			//locks for new path
			ScheduledUnlock lockPD = new ScheduledUnlock(new Edge(pre,bailout), lockPN.getTime(), lockPN.isSuccess(), val, lockPN.getNr()); 
			ScheduledUnlock lockDS = new ScheduledUnlock(new Edge(bailout,succ), lockNS.getTime(), lockNS.isSuccess(), val, lockNS.getNr());
			this.qLocks.add(lockPD);
			this.qLocks.add(lockDS);
			//locks for fees 
			this.timeAdded = this.timeAdded + 6*this.linklatency; //6 for setting up (2 communication with D, 4 links to setup)
			double step = this.curTime + this.timeAdded + this.linklatency; //immediately resolved 
			ScheduledUnlock feeBA = new ScheduledUnlock(new Edge(node,pre), step, true, fB+fA+fC+minFee, lockPN.getNr());
			step = step + this.linklatency;
			ScheduledUnlock feeAD = new ScheduledUnlock(new Edge(pre, bailout), step, true, fB+fC+minFee, lockPN.getNr());
			step = step + this.linklatency;
			ScheduledUnlock feeDC = new ScheduledUnlock(new Edge(bailout, succ), step, true, fB+fC, lockPN.getNr());
			step = step + this.linklatency;
			ScheduledUnlock feeCB = new ScheduledUnlock(new Edge(succ, node), step, true, fB, lockPN.getNr());
			this.qLocks.add(feeBA); 
			this.qLocks.add(feeAD); 
			this.qLocks.add(feeDC); 
			this.qLocks.add(feeCB); 
			return true; 
		}
	}
	
	@Override 
	public boolean isSufficientPot(int s, int t, double val, int pre) {
		boolean a = super.isSufficientPot(s, t, val, pre);
		if (pre == -1) return a; //not a bailout try 
		if (!a) { //check if bailout an option 
			//step 1: check if there are locks on this edge
			Edge e = new Edge(s,t);
			double l = this.locked.get(e);
			if (l + this.computePotential(s, t) >= val) { //collateral locked is sufficient to forward payment 
				//retrieve all locks 
				Vector<ScheduledUnlock[]> locksEdge = this.getLocks(new Edge(s,t));
				for (int j = 0; j < locksEdge.size(); j++) {
					ScheduledUnlock[] locks = locksEdge.get(j); 
					if (locks[0] != null) { ///need way to determine
						 this.bailout(s, pre, t, locks[0], locks[1]); 
					}
				}
			}
			
			
		}
		return a; 
	}
	
	public Vector<ScheduledUnlock[]> getLocks(Edge e){
		Vector<ScheduledUnlock[]> vec = new Vector<ScheduledUnlock[]>();
		HashMap<Integer, ScheduledUnlock> preLink = new HashMap<Integer, ScheduledUnlock>();
		Iterator<ScheduledUnlock> it = this.qLocks.iterator();
		while (it.hasNext()) {
			ScheduledUnlock lock = it.next();
			if (lock.getEdge().equals(e)) {
				ScheduledUnlock[] locks = new ScheduledUnlock[2];
				locks[1] = lock;
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
		boolean works = super.isSufficientPot(pre, bailout, valPBail, -1);
		works = works & super.isSufficientPot(bailout, succ, valBailS, -1); 
		works = works & super.isSufficientPot(node, pre, valNP, -1); 
		works = works & super.isSufficientPot(succ, node, valSN, -1); 
		return works; 
	}
	
	private double getFeeD(int pre, int succ, int bailout, double val, double timeout) {
		switch (this.feeStrategy) {
		case NORMAL: return this.params.computeFee(new Edge(bailout, succ), val); 
		case FACTOR: return this.feeFactor*this.params.computeFee(new Edge(bailout, succ), val);
		case EXPECTED: return this.doMCSimulation(pre, succ, bailout, val, true, timeout);
		default: return Double.MAX_VALUE;
		}
	}
	
	private double estimateMCFee(int pre, int succ, int node, double val, boolean add, double timeout) {
		
	}
	
    private double doMCSimulation(int pre, int succ, int node, double val, double preStartPot, double preStartLocked, double succStartPot,
    		double succStartLocked) {
		
	}
	
	public void preprocess(Graph g) {
	     super.preprocess(g);
	     this.params = (LNParams) (g.getProperty("LN_PARAMS"));
	}

}
