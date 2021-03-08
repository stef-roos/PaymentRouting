package paymentrouting.route.fee;

import gtna.graph.Graph;
import treeembedding.credit.CreditLinks;

public class LightningFees extends FeeComputation {
	double base;
	double rate; 
	boolean zero;
	
	public LightningFees(double b, double r, boolean zb) {
		super("LIGHTNING_FEES_"+b+"_"+r+"_"+zb);
		this.base = b;
		this.rate = r;
		this.zero = zb; 
	}

	@Override
	public double getFee(Graph g, CreditLinks edgeweights, double val, int s, int t) {
		if (this.zero) {
			double b1 = edgeweights.getPot(s, t);
			double b2 = edgeweights.getPot(t, s);
			double capacity = b1+b2;
			double refcapacity = capacity*0.5;
			double oldDiff = Math.abs(b1-refcapacity); 
			double newDiff = Math.abs(b1-val-refcapacity);
			if (newDiff < oldDiff) {
				return 0; 
			}
		}
		return this.base+this.rate*val;
	}

}
