package paymentrouting.route.concurrency;

import gtna.graph.Edge;

public class ScheduledUnlock implements Comparable<ScheduledUnlock>{
	double time;
	Edge edge; 
	boolean success;
	double val; 
	
	public ScheduledUnlock(Edge e, double t, boolean s, double v) {
		this.time = t;
		this.edge = e;
		this.success = s;
		this.val = v; 
	}

	@Override
	public int compareTo(ScheduledUnlock o) {
		return (int) Math.signum(this.time-o.time);
	}
	

}
