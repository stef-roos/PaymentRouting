package paymentrouting.route.bailout;

import java.util.HashSet;
import java.util.Random;

import gtna.graph.Graph;

public class PaymentReactionGriefingRandom extends PaymentReactionGriefing {
	double attFraction;
	
	public PaymentReactionGriefingRandom(double attP) {
		this.attFraction = attP; 
	}

	@Override
	public HashSet<Integer> getAttackers(Graph g, Random rand) {
		HashSet<Integer> set = new HashSet<Integer>();
		int n = g.getNodeCount();
		int atts = (int) (n*this.attFraction);
		while (set.size() < atts) {
			set.add(rand.nextInt(n));
		}
		return set;
	}
	
	

}
