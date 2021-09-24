package paymentrouting.route.concurrency;

import gtna.graph.Edge;

public class ScheduledUnlock implements Comparable<ScheduledUnlock>{
	double time;
	Edge edge; 
	boolean success;
	double val; 
	int nr;
	
	/**
	 * constructor with unlock time known and success status known 
	 * @param e
	 * @param t
	 * @param s
	 * @param v
	 * @param nr
	 */
	public ScheduledUnlock(Edge e, double t, boolean s, double v, int nr) {
		this.time = t;
		this.edge = e;
		this.success = s;
		this.val = v; 
		this.nr = nr; 
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/**
	 * constructor when unlock time and success not yet known 
	 * @param e
	 * @param v
	 * @param nr
	 */
	public ScheduledUnlock(Edge e, double v, int nr) {
		this.edge = e;
		this.val = v; 
		this.nr = nr; 
	}
	
	public void finalize(double t, boolean s) {
		this.time = t;
		this.success = s;		
	}

	public double getTime() {
		return time;
	}

	public Edge getEdge() {
		return edge;
	}

	public boolean isSuccess() {
		return success;
	}

	public double getVal() {
		return val;
	}

	public int getNr() {
		return nr;
	}

	@Override
	public int compareTo(ScheduledUnlock o) {
		return (int) Math.signum(this.time-o.time);
	}
	

}
