package paymentrouting.route.fee;

import gtna.graph.Graph;
import treeembedding.credit.CreditLinks;

public abstract class FeeComputation {
	private String name;
	
	public FeeComputation(String name) {
		this.name = name; 
	}

	public String getName() {
		return name;
	}
	
	/**
	 * get fees of path for value val
	 * @param g
	 * @param edgeweights
	 * @param val
	 * @param path
	 * @return
	 */
	public double getFee(Graph g, CreditLinks edgeweights, double val, int[] path) {
		double sum = 0;
		for (int i = 0; i < path.length-1; i++) {
			sum = sum + this.getFee(g, edgeweights, val, path[i], path[i+1]);
		}
		return sum; 
	}
	
	/**
	 * get fee for link (s,t) for value val 
	 * @param g
	 * @param edgeweights
	 * @param val
	 * @param s
	 * @param t
	 * @return
	 */
	public abstract double getFee(Graph g, CreditLinks edgeweights, double val, int s, int t);

}
