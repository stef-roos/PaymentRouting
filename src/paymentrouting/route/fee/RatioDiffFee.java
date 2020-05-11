package paymentrouting.route.fee;

import gtna.graph.Graph;
import treeembedding.credit.CreditLinks;

public class RatioDiffFee extends FeeComputation {
	double zone;  
	double defaultFee; 
	boolean zero;
	
	public RatioDiffFee(double z, double df, boolean zb) {
		super("RATIO_DIFF_"+z+"_"+df+"_"+zb);
		this.zone = z; 
		this.defaultFee = df; 
		this.zero = zb; 		
	}

	@Override
	public double getFee(Graph g, CreditLinks edgeweights, double val, int s, int t) {
		double b1 = edgeweights.getPot(s, t);
		double b2 = edgeweights.getPot(t, s);
		double capacity = b1+b2;
		double refcapacity = capacity*0.5;
		double oldDiff = Math.abs(b1-refcapacity); 
		double newDiff = Math.abs(b1-val-refcapacity);
		if (this.zero && newDiff < oldDiff) {
			return 0; 
		}
		double neg = this.zone*capacity;
		if (oldDiff < neg) {
			oldDiff = neg;
		}
		double ratio = newDiff/oldDiff; 
		if (this.zero) {
			ratio = ratio - 1; 
		}
		return ratio*this.defaultFee; 
	}

}
