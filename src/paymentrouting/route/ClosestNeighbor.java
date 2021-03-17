package paymentrouting.route;

import java.util.Random;
import java.util.Vector;

import gtna.graph.Graph;
import gtna.graph.Node;

/**
 * no splitting, forward to neighbor closest to destination and has sufficient funds 
 * @author mephisto
 *
 */
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
		double bestDist = Double.MAX_VALUE;
		Vector<Integer> bests = new Vector<Integer>();
		
		for (int k = 0; k < out.length; k++) {
			//exclude nodes not closer or marked as excluded 
			if (out[k] == pre || excluded[out[k]]) continue;
			if (!this.dist.isCloser(out[k], cur, dst, reality)) continue; 
			//compute distance 
			double d = this.dist.distance(out[k], dst, reality);
			//check if balance/potential of direction is sufficient 
			double pot = rp.computePotential(cur, out[k]);
			if (pot >= curVal) {
			  if (d < bestDist) {
				  //new neighbor that is closests 
				bests = new Vector<Integer>();
			  }
			  if (d <= bestDist) {
				  // add to neighbors closests 
				 bestDist = d;
				 bests.add(k);
			  }
			} 
		}
		if (bests.isEmpty()) { 
			//routing failed 
		    return null;
		} else {
			//choose random closests neighbor and forward amount via them  
			double[] partVals = new double[out.length];
			int choice = bests.get(rand.nextInt(bests.size())); 
			partVals[choice] = curVal;
			return partVals;
		}
	}


	
	

}
