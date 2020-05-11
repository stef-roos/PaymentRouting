package paymentrouting.route.attack;

import java.util.Random;

import gtna.graph.Graph;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;

public abstract class DropSplits extends AttackPathSelection {
	double fraction;
	int delay; 

	public DropSplits(String n, PathSelection select, double fraction, int delay) {
		super(n+"_"+fraction+"_"+delay, select);
		this.fraction = fraction;
		this.delay = delay; 
	}

	@Override
	public void selectAtt(Graph g, Random rand) {
		int nodes = g.getNodeCount();
		this.att = new boolean[nodes];
		int choose = fraction<=0.5?(int)Math.round(this.fraction*nodes):(int)Math.round((1-this.fraction)*nodes);
		for (int i = 0; i < choose; i++) {
			int j = rand.nextInt(nodes);
			while (att[j]) {
				j = rand.nextInt(nodes);
			}
			this.att[j] = true;
		}
		
		//if more than 50% attackers, negate
		if (this.fraction > 0.5) {
			for (int k = 0; k < this.att.length; k++) {
				this.att[k] = !this.att[k]; 
			}
		}
//		for (int k = 0; k < this.att.length; k++) {
//			System.out.println(k + " is attacker " + this.att[k]); 
//		}
		
		
	}

	public double[] getResZero(Graph g, int cur) {
		int l = g.getNodes()[cur].getOutDegree();
		double[] res = new double[l];
		return res; 
	}

}
