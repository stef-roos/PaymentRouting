package paymentrouting.route;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import gtna.graph.Graph;
import gtna.graph.Node;
import treeembedding.credit.CreditLinks;

/**
 * split using the nodes closest to destination 
 * @author mephisto
 *
 */
public class SplitClosest extends PathSelection {

	public SplitClosest(DistanceFunction df) {
		super("SPLIT_CLOSEST", df);
	}
	
	public SplitClosest(String key, DistanceFunction df) {
		super(key, df);
	}

	@Override
	public void initRoutingInfo(Graph g, Random rand) {
		this.dist.initRouteInfo(g, rand);
		
	}



	@Override
	public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, RoutePayment rp, double curVal,
			Random rand, int reality) {
		Node[] nodes = g.getNodes();
		int[] out = nodes[cur].getOutgoingEdges();
		//sum all capacities available for forwarding 
		double sum = 0;
		HashMap<Double, Vector<Integer>> dists = new HashMap<Double, Vector<Integer>>();
		for (int k = 0; k < out.length; k++) {
			if (out[k] == pre || excluded[out[k]]) continue;
			if (this.dist.isCloser(out[k], cur, dst, reality)) {
				double d = this.dist.distance(out[k], dst, reality);
				Vector<Integer> vec = dists.get(d);
				if (vec == null) {
					vec = new Vector<Integer>(); 
					dists.put(d, vec);
				}
				vec.add(k);
				sum = sum + rp.computePotential(cur, out[k]);
			}
		}
		if (sum < curVal) {
			//routing failed as combined capacity of neighbors is insufficient 
			return null;
		} else {
			double[] partVal = new double[out.length];
			//write and sort distances  
			Iterator<Double> it = dists.keySet().iterator();
			double[] vals = new double[dists.size()];
			int c = 0;
			while (it.hasNext()) {
				vals[c] = it.next();
				c++;
			}
			Arrays.sort(vals);
			
			double all = 0; //funds already assigned to be forwarded 
			for (int i = 0; i < vals.length; i++) {
				//start with node(s) at least distance 
				Vector<Integer> vec = dists.get(vals[i]);
				while (vec.size() > 0) {
					int node = vec.remove(rand.nextInt(vec.size()));
					//forward all that still needs forwarding via this node if possible, otherwise: forward maximal value that can go via channel
					double valNode = Math.min(rp.computePotential(cur, out[node]), curVal-all);
					all = all + valNode;
					partVal[node] = valNode;
					if (all >= curVal) {
						//stop if all funds are assigned for forwarding 
						break;
					}
				}
				if (all >= curVal) {
					break;
				}
			}
			return partVal;
		}
	}

}
