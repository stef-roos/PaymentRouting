package paymentrouting.route.attack;

import java.util.HashMap;
import java.util.Random;

import gtna.graph.Graph;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;

public class ColludingDropSplits extends DropSplits {
		int inwaiting = -1;
		int waited; 
		

		public ColludingDropSplits(PathSelection select, double fraction, int delay) {
			super("COLLUDING_DROP_SPLITS", select, fraction, delay);
            this.inwaiting = -1; 
            this.waited = 0;
		}


		@Override
		public double[] performAttack(Graph g, int cur, int dst, int pre, boolean[] excluded, RoutePayment rp,
				double curVal, Random rand, int reality) {
			//System.out.println("Perform attack with cur " + cur + " counter " + this.waited + " " + this.inwaiting); 
			//no attack at sender
			if (pre == -1) {
				return this.sel.getNextsVals(g, cur, dst, pre, excluded, rp, curVal, rand, reality); 
			}
			//merge -> drop
			if (this.waited == -1) {
				return null; 
			}
			
			if (this.inwaiting != -1 ) {
				if (this.inwaiting != cur) {
					this.inwaiting = cur; //signal other to drop as well
				    return null;
				} else {
					this.waited++;
					int maxdelay = this.delay - (int)Math.round(this.getDist().distance(cur, dst, reality));
					if (waited >= maxdelay) {
						this.inwaiting = -1;
						//no more delaying -> normal beahvior
						return this.sel.getNextsVals(g, cur, dst, pre, excluded, rp, curVal, rand, reality);
					} else {
						//delay 
						return this.getResZero(g, cur); 
					}
					
				}
			} else {
				//weird case: removed inwaiting tag in last round 
				this.inwaiting = cur;
				this.waited = 0;
				return this.getResZero(g, cur); 
			}
			
		}
		
		@Override 
		public void clear() {
			this.inwaiting = -1;  
		}


		@Override
		public void prepareAttack(Graph g, int next, double curVal, Random rand) {
			if (this.inwaiting == -1) {
			   this.inwaiting = next;
			   this.waited = 0;
			} else {
				//a merge 
				if (inwaiting == next) {
					this.waited = -1; 
				}
			}
		}

}
