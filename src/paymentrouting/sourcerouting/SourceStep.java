package paymentrouting.sourcerouting;

import java.util.HashMap;
import java.util.Random;

import gtna.graph.Graph;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;
import paymentrouting.route.fee.BaseRateFee;
import paymentrouting.route.fee.PathFee;

public abstract class SourceStep extends PathSelection {
	
	
	public SourceStep(String n, DistanceFunction d) {
		super(n, d);
	}

	HashMap<Integer, HashMap<Integer,Integer>> toroute; 
	BaseRateFee fees; 

	@Override
	public void initRoutingInfo(Graph g, Random rand) {
		toroute = new HashMap<Integer, HashMap<Integer,Integer>>(); 
		this.fees = (BaseRateFee) g.getProperty("BASERATEFEE");
	}

	@Override
	public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, RoutePayment rp, double curVal,
			Random rand, int reality) {
		int trnr = ((RoutePaymentConcurrent)rp).getCurT();
		if (pre ==  -1) {
			//source determines paths
			PathFee[] paths = this.calculateRoute(g, cur, dst, trnr, curVal, rand, reality);
			//record path info
			for (int j = 0; j< paths.length; j++) {
				int[] p = paths[j].getPath();
				for (int i = 1; i < p.length-1; i++) {
					HashMap<Integer, Integer> nexts = toroute.get(p[i]);
					if (nexts == null) {
						nexts = new HashMap<Integer, Integer>();
						toroute.put(p[i], nexts); 
					}
					nexts.put(trnr,p[i+1]);
				}
			}
			//prepare return array	
			int[] out = g.getNodes()[cur].getOutgoingEdges();
			double[] vals = new double[out.length];
			for (int j = 0; j< paths.length; j++) {
				int[] p = paths[j].getPath();
				int first = p[1];
			    for (int i = 0; i < out.length; i++) {
				    if (out[i] == first) {
				    	vals[i] = vals[i]+ paths[j].getTotal(); 
				    	break; 
				    }
				
			    }
			}
			return vals;
		} else {
			HashMap<Integer, Integer> txs = this.toroute.get(cur);
			int succ = txs.get(trnr);
			txs.remove(trnr); 
			double[] f = this.fees.getFees(cur); 
			double nval = (curVal - f[0])/(1+f[1]);
			int[] out = g.getNodes()[cur].getOutgoingEdges();
			double[] vals = new double[out.length]; 
			for (int i = 0; i < out.length; i++) {
			    if (out[i] == succ) {
			    	vals[i] = vals[i]+ nval; 
			    }
			
		    }
			return vals; 
		}
		
	}
	
	public abstract PathFee[] calculateRoute(Graph g, int src, int dst, int nr, double val, Random rand, int reality);
	

}
