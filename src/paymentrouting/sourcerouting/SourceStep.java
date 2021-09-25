package paymentrouting.sourcerouting;

import java.util.HashMap;
import java.util.Random;

import gtna.graph.Edge;
import gtna.graph.Graph;
import paymentrouting.datasets.LNParams;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;
import paymentrouting.route.fee.PathFee;
import treeembedding.credit.CreditLinks;

public abstract class SourceStep extends PathSelection {
	double[] locks; 
	
	
	public SourceStep(String n, DistanceFunction d) {
		super(n, d);
	}

	HashMap<Integer, HashMap<Integer,Integer>> toroute; 
	LNParams params; 

	@Override
	public void initRoutingInfo(Graph g, Random rand) {
		toroute = new HashMap<Integer, HashMap<Integer,Integer>>(); 
		this.params = (LNParams) g.getProperty("LN_PARAMS");
	}

	@Override
	public double[] getNextsVals(Graph g, int cur, int dst, int pre, boolean[] excluded, RoutePayment rp, double curVal,
			Random rand, int reality) {
		int trnr = ((RoutePaymentConcurrent)rp).getCurT();
		if (pre ==  -1) {
			//source determines paths
			PathFee[] paths = this.calculateRoute(rp, g, cur, dst, trnr, curVal, rand, reality, excluded);
			if (paths == null) return null; 
			//record path info
			this.locks = new double[paths.length]; 
			for (int j = 0; j< paths.length; j++) {
				int[] p = paths[j].getPath();
				locks[j] = paths[j].getTotalLock(); 
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
				double pot = rp.computePotential(cur, first); 
			    for (int i = 0; i < out.length; i++) {
				    if (out[i] == first) {
				    	vals[i] = vals[i]+ paths[j].getTotal(); 
				    	if (vals[i] > pot) { //check if capacity sufficient 
				    		return null; //should only happen if concurrency 
				    	}
				    	break; 
				    }
				
			    }
			}
			return vals;
		} else {
			locks = null; 
			HashMap<Integer, Integer> txs = this.toroute.get(cur);
			int succ = txs.get(trnr);
			txs.remove(trnr); 
			Edge e = new Edge(cur, succ);
			double base = this.params.getBase(e);
			double rate = this.params.getRate(e);
			double nval = (curVal - base)/(1+rate);
			if (rp.computePotential(cur, succ) < nval) {
				return null; 
			}
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
	
	public abstract PathFee[] calculateRoute(RoutePayment rp, Graph g, int src, int dst, int nr, double val, Random rand, int reality, boolean[] excluded);
	

	public double maxLocktime(Graph g, int s, int t, int i) {
		if (locks != null) {
		   return this.locks[i];
		} else {
			return Double.MAX_VALUE; 
		}
	}
	
	public double decreaseLock(Graph g, int i, int j) {
		return this.params.getDelay(new Edge(i,j)); 
	}
}
