package paymentrouting.route.bailout;

import java.util.HashSet;
import java.util.Random;

import gtna.graph.Graph;
import paymentrouting.route.attack.GriefingAttack;

public class PaymentReactionGriefingRandom extends PaymentReactionGriefing {
	double attFraction;
	
	public PaymentReactionGriefingRandom(double attP, GriefingAttack att) {
		super("GRIEF_RANDOM", att); 
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
