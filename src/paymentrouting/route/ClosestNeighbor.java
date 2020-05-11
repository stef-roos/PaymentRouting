package paymentrouting.route;

import java.util.Random;
import java.util.Vector;

import gtna.graph.Graph;
import gtna.graph.Node;

public class ClosestNeighbor extends PathSelection {

	public ClosestNeighbor(DistanceFunction d) {
		super("CLOSEST_NEIGHBOR", d);
	}

	@Override
	public void initRoutingInfo(Graph g, Random rand) {
		this.dist.initRouteInfo(g, rand);		
	}


	@Override
	public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, 
			RoutePayment rp, double curVal,
			Random rand, int reality) {
		Node[] nodes = g.getNodes();
		int[] out = nodes[cur].getOutgoingEdges();
		//double curDist = this.dist.distance(cur, dst, reality);
		double bestDist = Double.MAX_VALUE;
		Vector<Integer> bests = new Vector<Integer>();
		
		for (int k = 0; k < out.length; k++) {
			if (out[k] == pre || excluded[out[k]]) continue;
			if (!this.dist.isCloser(out[k], cur, dst, reality)) continue; 
			double d = this.dist.distance(out[k], dst, reality);
			double pot = rp.computePotential(cur, out[k]);
			if (pot >= curVal) {
			  if (d < bestDist) {
				bests = new Vector<Integer>();
			  }
			  if (d <= bestDist) {
				 bestDist = d;
				 bests.add(k);
			  }
			} 
		}
		if (bests.isEmpty()) { 
		    return null;
		} else {
			double[] partVals = new double[out.length];
			int choice = bests.get(rand.nextInt(bests.size())); 
			partVals[choice] = curVal;
			return partVals;
		}
	}


	
	

}
