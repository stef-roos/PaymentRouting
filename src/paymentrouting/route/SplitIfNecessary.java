package paymentrouting.route;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import gtna.graph.Graph;
import gtna.graph.Node;


public class SplitIfNecessary extends PathSelection {
	ClosestNeighbor cn;

	public SplitIfNecessary(DistanceFunction df) {
		super("SPLIT_IFNECESSARY", df);
		this.cn = new ClosestNeighbor(df); 
	}
	
	
	

	@Override
	public void initRoutingInfo(Graph g, Random rand) {
		this.dist.initRouteInfo(g, rand);
		
	}


	@Override
	public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, 
			RoutePayment rp, double curVal,
			Random rand, int reality) {
		double[] noSplit = this.cn.getNextsVals(g, cur, dst, pre, excluded, rp, curVal, rand, reality);
		if (noSplit != null) {
			return noSplit; 
		}
		Node[] nodes = g.getNodes();
		int[] out = nodes[cur].getOutgoingEdges();
		double sum = 0;
		HashMap<Double, Vector<Integer>> dists = new HashMap<Double, Vector<Integer>>();
		for (int k = 0; k < out.length; k++) {
			if (out[k] == pre || excluded[out[k]]) continue;
			if (this.dist.isCloser(out[k], cur, dst, reality)) {
				double pot = rp.computePotential(cur, out[k]);
				Vector<Integer> vec = dists.get(pot);
				if (vec == null) {
					vec = new Vector<Integer>(); 
					dists.put(pot, vec);
				}
				vec.add(k);
				sum = sum + pot;
			}
		}
		if (sum < curVal) {
			return null;
		} else {
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
			for (int i = vals.length-1; i > -1; i--) {
				Vector<Integer> vec = dists.get(vals[i]);
				while (vec.size() > 0) {
					int node = vec.remove(rand.nextInt(vec.size()));
					double valNode = Math.min(rp.computePotential(cur, out[node]), curVal-all);
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

}
