package gtna.transformation.churn;

public class TopologyChange implements Comparable<TopologyChange> {
	double time;
	int node;
	boolean on;
	
	public TopologyChange(double t, boolean o, int n){
		this.time = t;
		this.on = o;
		this.node=n;
	}
	

	/**
	 * @return the time
	 */
	public double getTime() {
		return time;
	}



	/**
	 * @return the node
	 */
	public int getNode() {
		return node;
	}



	/**
	 * @return the on
	 */
	public boolean isOn() {
		return on;
	}



	@Override
	public int compareTo(TopologyChange arg0) {
		return (int)Math.signum(this.time-arg0.time);
	}

}
