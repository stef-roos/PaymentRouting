package paymentrouting.route;

import java.util.Random;

import gtna.graph.Graph;
import treeembedding.credit.CreditLinks;

public abstract class PathSelection {
	private String name;
	public DistanceFunction dist;  
	

	public PathSelection(String n, DistanceFunction d) {
		this.name = n;
		this.dist = d; 
	}
	
	public String getName() {
		return name;
	}
	
	public DistanceFunction getDist() {
		return dist;
	}
	
	/**
	 * initial info distributed during setup
	 * @param g
	 */
	public abstract void initRoutingInfo(Graph g, Random rand);
	
	/**
	 * clearing information of routing attempt 
	 * 
	 */
	
	public void clear() {
		
	}
	
	public double maxLocktim(Graph g, int s, int t, int i) {
		return Double.MAX_VALUE; 
	}
	
	public double decreaseLock(Graph g, int i, int j) {
		return 1; 
	}

	/**
	 * decide on how to split curVal over neighbors based on distances and capacities
	 * @param g
	 * @param cur
	 * @param dst
	 * @param pre: predeccessor 
	 * @param excluded: nodes excluded from routing
	 * @param cl
	 * @param curVal
	 * @return
	 */
	public abstract double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, 
			RoutePayment rp,
			double curVal, Random rand, int reality);

}
