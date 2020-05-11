package paymentrouting.route.fee;

import gtna.graph.Graph;
import treeembedding.credit.CreditLinks;

public class DiStasi extends FeeComputation {
	double base;
	double low;
	double high;
	
	public DiStasi(double f, double l, double h) {
		super("DI_STASI_"+f+"_"+l+"_"+h);
		this.base = f; 
		this.low = l; 
		this.high = h; 
	}

	@Override
	public double getFee(Graph g, CreditLinks edgeweights, double val, int s, int t) {
		double b1 = edgeweights.getPot(s, t);
		double b2 = edgeweights.getPot(t, s);
		double diff = Math.abs(b1-b2);
		double fee = this.base;
		if (b1 > b2) {
			if (val > diff/2) {
				fee = fee + diff/2*this.low+(val-diff/2)*this.high;
			} else {
				fee = fee + val*this.low;
			}
		} else {
			fee = fee + val*this.high;
		}
		return fee;
	}
	
	

}
