package paymentrouting.route;

import java.util.Vector;

/**
 * info about path between source and intermediary 
 * @param n
 * @param val2
 * @param p
 * @param r
 */

public class PartialPath {
        public int node;
		public double val;
		public Vector<Integer> pre;
		public int reality;
		
		public PartialPath(int n, double val2, Vector<Integer> p, int r) {
			this.node = n;
			this.val = val2;
			this.pre = p;
			this.reality = r;
		}
	
}
