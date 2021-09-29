package paymentrouting.route.bailout;

import java.util.HashSet;
import java.util.Random;

import gtna.graph.Edge;
import gtna.graph.Graph;
import paymentrouting.datasets.LNParams;

public abstract class PaymentReactionGriefing extends PaymentReaction {
	HashSet<Integer> attackers;
	
	public PaymentReactionGriefing(String name) {
		super(name); 
	}

	@Override
	public void init(Graph g, Random rand) {
		this.attackers = getAttackers(g,rand); 
		
	}
	
	public abstract HashSet<Integer> getAttackers(Graph g, Random rand);

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
		if (!this.attackers.contains(e.getDst())) {
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
		return !this.attackers.contains(dst); //attackers do not allow payment to succeed  
	}


}
