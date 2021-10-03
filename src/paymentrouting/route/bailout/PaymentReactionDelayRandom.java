package paymentrouting.route.bailout;

import java.util.Random;

import gtna.graph.Edge;
import gtna.graph.Graph;
import paymentrouting.datasets.LNParams;

public class PaymentReactionDelayRandom extends PaymentReaction {
	double pDelay;
	
	public PaymentReactionDelayRandom(double p) {
		super("DELAY_RANDOM"); 
		this.pDelay = p; 
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
		return delay(g,e,timenow,rand); 
	}

	@Override
	public double resolve(Graph g, Edge e, double timenow, Random rand) {
		return delay(g,e,timenow,rand); 
	}
	
	private double delay(Graph g, Edge e, double timenow, Random rand) {
		if (rand.nextDouble() > this.pDelay) {
			return 0;
		} else {
			LNParams par = (LNParams) g.getProperty("LN_PARAMS"); 
			if (par == null) {
				return 0;
			} else {
				System.out.println("Delay at edge " + e.toString()); 
				return par.getDelay(e); 
			}
		}
	}
	
	@Override
	public boolean receiverReaction(int dst) {
		return true; //receiver always completes successful payment 
	}


}
