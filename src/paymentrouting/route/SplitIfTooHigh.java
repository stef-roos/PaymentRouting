package paymentrouting.route;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import gtna.graph.Graph;
import gtna.graph.Node;

/**
 * split if too many funds are used by payment 
 * NOT EVALUATED 
 * @author mephisto
 *
 */
public class SplitIfTooHigh extends SplitClosest {
	double reserve;
	double cur_reserve; 
	double step;

	public SplitIfTooHigh(DistanceFunction df, double p, double s) {
		super("SPLIT_IFTOOHIGH_"+p+"_"+s, df);
		this.reserve = p;
		this.cur_reserve = p;
		this.step = s; 
	}
	
	@Override
	public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, 
			RoutePayment rp, double curVal,
			Random rand, int reality) {
		this.cur_reserve = this.reserve; 
		Node[] nodes = g.getNodes();
		int[] out = nodes[cur].getOutgoingEdges();
		boolean done = false;
		HashMap<Double, Vector<Integer>> dists = new HashMap<Double, Vector<Integer>>();
		while (!done) {
		double sum = 0;
		for (int k = 0; k < out.length; k++) {
			if (out[k] == pre || excluded[out[k]]) continue;
			if (this.dist.isCloser(out[k], cur, dst, reality)) {
				double d = this.dist.distance(out[k], dst, reality);
				Vector<Integer> vec = dists.get(d);
				if (vec == null) {
					vec = new Vector<Integer>(); 
					dists.put(d, vec);
				}
				double tot = rp.getTotalCapacity(cur, out[k]);
				double rest = rp.computePotential(cur, out[k]);
				double toUse = Math.max(0, rest - this.cur_reserve*tot);
				sum = sum + toUse; 
				if (toUse > 0) {
					vec.add(k);
				}
			}
		}
		if (sum < curVal) {
			if (this.cur_reserve > 0) {
				this.cur_reserve = Math.max(0, this.cur_reserve-this.step); 
                dists = new HashMap<Double, Vector<Integer>>();
			} else {
				return null; 
			}
		} else {
			done = true; 
		}
		}
		

			double[] partVal = new double[out.length];
			Iterator<Double> it = dists.keySet().iterator();
			double[] vals = new double[dists.size()];
			int c = 0;
			while (it.hasNext()) {
				vals[c] = it.next();
				c++;
			}
			Arrays.sort(vals);
			
			double all = 0;
			for (int i = 0; i < vals.length; i++) {
				Vector<Integer> vec = dists.get(vals[i]);
				while (vec.size() > 0) {
					int node = vec.remove(rand.nextInt(vec.size()));
					double tot = rp.getTotalCapacity(cur, out[node]);
					double rest = rp.computePotential(cur, out[node]);
					double toUse = Math.max(0, rest - this.cur_reserve*tot);
					double valNode = Math.min(toUse, curVal-all);
					all = all + valNode;
					partVal[node] = valNode;
					if (all >= curVal) {
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
