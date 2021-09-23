package paymentrouting.route.concurrency;

import gtna.graph.Edge;

public class ScheduledUnlock implements Comparable<ScheduledUnlock>{
	double time;
	Edge edge; 
	boolean success;
	double val; 
	int nr;
	
	public ScheduledUnlock(Edge e, double t, boolean s, double v, int nr) {
		this.time = t;
		this.edge = e;
		this.success = s;
		this.val = v; 
		this.nr = nr; 
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
