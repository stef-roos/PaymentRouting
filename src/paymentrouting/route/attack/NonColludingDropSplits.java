package paymentrouting.route.attack;

import java.util.HashMap;
import java.util.Random;

import gtna.graph.Graph;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;

public class NonColludingDropSplits extends DropSplits {
	HashMap<Integer, Integer> inDelay; 
	

	public NonColludingDropSplits(PathSelection select, double fraction, int delay) {
		super("NON_COLLUDING_DROP_SPLITS", select, fraction, delay);
		this.inDelay = new HashMap<Integer, Integer>(); 
	}


	@Override
	public double[] performAttack(Graph g, int cur, int dst, int pre, boolean[] excluded, RoutePayment rp,
			double curVal, Random rand, int reality) {
		if (inDelay.containsKey(cur)) {
			//in waiting 
			int del = inDelay.get(cur);
			if ((excluded[cur] || del == 0) && del != -1) {
				//same path
				del++;
				int maxdelay = this.delay - (int)Math.round(this.getDist().distance(cur, dst, reality)); 
				if (del >= maxdelay) {
					//remove from waiting list, forward as normal
					inDelay.remove(cur);
					return this.sel.getNextsVals(g, cur, dst, pre, excluded, rp, curVal, rand, reality); 
				} else {
					inDelay.put(cur, del);
					//do not forward anything
					return this.getResZero(g, cur);
				}
			} else {
				//different path or merge (del==-1) -> drop 
				return null; 
			}
		} else {
			//first visit (can happen if removed in previous step)
			if (pre != -1) {
				//not sender -> delay
			   inDelay.put(cur, 0);
			   //do not forward anything
			   return this.getResZero(g, cur);
			} else {
				//sender -> no delay
				return this.sel.getNextsVals(g, cur, dst, pre, excluded, rp, curVal, rand, reality); 
			}
		}
		
	}
	
	@Override 
	public void clear() {
		this.inDelay.clear(); 
	}


	@Override
	public void prepareAttack(Graph g, int next, double curVal, Random rand) {
		if (!this.inDelay.containsKey(next)) {
			//put in map 
			inDelay.put(next, 0);
		} else {
			//merge going to happen -> need to drop in attack 
			inDelay.put(next, -1);
		}
		
	}
	
	

}
