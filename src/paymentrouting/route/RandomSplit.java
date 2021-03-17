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
 * split a payment value randomly between neighbors closer to destination
 * ensure that forwarded value is in line with balance restrictions (if possible) 
 * @author mephisto
 *
 */
public class RandomSplit extends PathSelection {
	double minValue = 1; //minimal value for which splitting makes sense; 1 by default (can be adjusted though)  

	public RandomSplit(DistanceFunction df) {
		super("RANDOM_SPLIT", df);
	}

	@Override
	public void initRoutingInfo(Graph g, Random rand) {
		this.dist.initRouteInfo(g, rand);
		
	}


	@Override
	public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, 
			RoutePayment rp, double curVal,
			Random rand, int reality) {
		if (curVal < this.minValue) {
			return this.splitNecessary(g, cur, dst, pre, excluded, rp, curVal, rand, reality);
		}
		//compute total funds of all neighbors closer to destination 
		Node[] nodes = g.getNodes();
		int[] out = nodes[cur].getOutgoingEdges();
		Vector<Integer> poss = new Vector<Integer>();
		double sum = 0;
		for (int k = 0; k < out.length; k++) {
			//ignore excluded, node on path before, and at farer distance  
			if (out[k] == pre || excluded[out[k]]) continue;
			if (this.dist.isCloser(out[k], cur, dst, reality)) {
				poss.add(k);
				sum = sum  + rp.computePotential(cur, out[k]);
			} 
		}
		if (poss.isEmpty() || sum < curVal) { 
			//not enough funds to forward 
		    return null;
		} else {
			//initialize partial values given to neighbors 
			double[] partVals = new double[out.length];
			int count = poss.size();
			//check if node can still receive more funds or capacity is reached 
			boolean[] remove = new boolean[poss.size()];
			//keep assigning funds randomly, sum up funds that were assigned too high for capacity and reallocate those until everything is allocated
			while (curVal > 0) {
				//choose random values such that a node i gets rands[i]-rands[i-1]
				double[] rands = new double[count];
				for (int i = 0; i < rands.length-1; i++) {
					rands[i] = rand.nextDouble()*curVal;
				}
				rands[rands.length-1] = curVal;
				Arrays.sort(rands);
				int c = 0;
				for (int j = 0; j < rands.length; j++) {
					//skip nodes that do not have funds left 
					while (remove[c]) {
						c++;
					}
					int a = poss.get(c);
					double last = (j==0?0:rands[j-1]);//previous value or 0 if first 
					//add to value for node a 
					double old = partVals[a];
					partVals[a] = partVals[a] + rands[j]-last; 
					//ensure above line did not add too much; if so, only add maximal possible value and stop considering node
					double p = rp.computePotential(cur, out[a]);
					if (partVals[a] > p) {
						double rem = p - old;
						curVal = curVal - rem;
						partVals[a] = rp.computePotential(cur, out[a]); 
						remove[c] = true;
						count--; 
					} else {
						curVal = curVal - partVals[a];
					}
					c++;
				}	
			}
			return partVals;
		}
	}
	
	/**
	 * if forwarded value is below a threshold, only split if necessary
	 * (motivation: does not make sense to split tiny values) 
	 * @param g
	 * @param cur
	 * @param dst
	 * @param pre
	 * @param excluded
	 * @param rp
	 * @param curVal
	 * @param rand
	 * @param reality
	 * @return
	 */
	public double[] splitNecessary(Graph g, int cur, int dst, int pre, boolean[] excluded, 
			RoutePayment rp, double curVal,
			Random rand, int reality) {
		Node[] nodes = g.getNodes();
		int[] out = nodes[cur].getOutgoingEdges();
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
			for (int i = 0; i < vals.length; i++) {
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

