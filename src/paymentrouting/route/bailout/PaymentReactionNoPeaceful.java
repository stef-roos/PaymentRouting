package paymentrouting.route.bailout;

import java.util.Random;

import gtna.graph.Edge;
import gtna.graph.Graph;
import paymentrouting.datasets.LNParams;

public class PaymentReactionNoPeaceful extends PaymentReaction {
	double pNotResolve;
	
	public PaymentReactionNoPeaceful(double p) {
		this.pNotResolve = p; 
	}

	@Override
	public void init(Graph g, Random rand) {
		// nothing to do 
		
	}

	@Override
	public boolean acceptLock(Graph g, int node, double time, double val, Random rand) {
		return true; //always accept 
	}

	@Override
	public double forwardHash(Graph g, Edge e, double timenow, Random rand) {
		return 0; //forward successful as soon as possible 
	}

	@Override
	public double resolve(Graph g, Edge e, double timenow, Random rand) {
		if (rand.nextDouble() > this.pNotResolve) {
			return 0;
		} else {
			LNParams par = (LNParams) g.getProperty("LN_PARAMS"); 
			if (par == null) {
				return 0;
			} else {
				return par.getDelay(e); 
			}
		}
		
	}

	@Override
	public boolean receiverReaction(int dst) {
		return true; //receiver always completes successful payment 
	}

}