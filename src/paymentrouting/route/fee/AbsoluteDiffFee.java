package paymentrouting.route.fee;

import gtna.graph.Graph;
import treeembedding.credit.CreditLinks;

public class AbsoluteDiffFee extends FeeComputation {
	double factor;  
	double defaultFee; 
	boolean zero;
	
	public AbsoluteDiffFee(double f, double df, boolean zb) {
		super("ABSOLUTE_DIFF_"+f+"_"+df+"_"+zb);
		this.factor = f; 
		this.defaultFee = df; 
		this.zero = zb; 
	}

	@Override
	public double getFee(Graph g, CreditLinks edgeweights, double val, int s, int t) {
		double b1 = edgeweights.getPot(s, t);
		double b2 = edgeweights.getPot(t, s);
		double refcapacity = (b1 + b2)*0.5;
		double oldDiff = Math.abs(b1-refcapacity); 
		double newDiff = Math.abs(b1-val-refcapacity);
		if (this.zero && newDiff < oldDiff) {
			return 0; 
		}
		double div = (newDiff-oldDiff)/(b1+b2); 
		if (newDiff < oldDiff) {
			//improvement, reduce fee
			return (1+div)*this.defaultFee;
		} else {
			if (this.zero) { 
				return this.factor*div*this.defaultFee; 
			} else {
			    return (1+this.factor*div)*this.defaultFee; 
			}    
			    
		}
	}

}
