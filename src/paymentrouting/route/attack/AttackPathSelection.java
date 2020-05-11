package paymentrouting.route.attack;

import java.util.Random;

import gtna.graph.Graph;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;

public abstract class AttackPathSelection extends PathSelection {
	PathSelection sel; 
	boolean[] att;

	public AttackPathSelection(String n, PathSelection select) {
		super(n+"_"+select.getName(), select.getDist());
		this.sel = select; 
	}
	
	public abstract void selectAtt(Graph g, Random rand);
	
	@Override 
	public void initRoutingInfo(Graph g, Random rand) {
		//normal init
		this.sel.initRoutingInfo(g, rand);
		//select the attacker 
		this.selectAtt(g, rand);
	}
	
	public abstract double[] performAttack(Graph g, int cur, int dst, int pre, boolean[] excluded, 
			RoutePayment rp,
			double curVal, Random rand, int reality); 
	
	public abstract void prepareAttack(Graph g, int next, 
			double curVal, Random rand); 
	
	@Override
	public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, 
			RoutePayment rp,
			double curVal, Random rand, int reality) {
		double[] res = null;
		if (!this.att[cur]) {
			//no attack, normal behavior
			res = this.sel.getNextsVals(g, cur, dst, pre, excluded, rp, curVal, rand, reality);
			
		} else {
			//attack
			res = this.performAttack(g, cur, dst, pre, excluded, rp, curVal, rand, reality);
		}
		
		
		if (res != null) {
			//attack on receiving share
			int[] out = g.getNodes()[cur].getOutgoingEdges();
			for (int k = 0; k < out.length; k++) {
				if (res[k] > 0 && this.att[out[k]]) {
					this.prepareAttack(g, out[k], curVal, rand);
				}
			}
		}
		return res;
	}
	
	
	

}
