package paymentrouting.sourcerouting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import gtna.graph.Edge;
import gtna.graph.Graph;
import paymentrouting.datasets.LNParams;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.RoutePayment;
import paymentrouting.route.fee.PathFee;
import treeembedding.credit.CreditLinks;

/**
 * based on Dan Andreescu's https://github.com/dandreescu/PaymentRouting/blob/lightning/src/paymentrouting/route/costfunction/LND.java
 * 
 *
 */

public class LND extends SourceStep implements CostFunction{
	  Map[] lastFailure;
	  int observer;
	  double time;

	  static double LND_RISK_FACTOR = 0.000000015;
	  static double A_PRIORI_PROB = 0.6;

	public LND(DistanceFunction d) {
		super("LND", d);
	}

	@Override
	public PathFee[] calculateRoute(RoutePayment rp, Graph g, int src, int dst, int nr, double val, Random rand, int reality, boolean[] excluded) {
		CreditLinks edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		LNParams par = (LNParams) g.getProperty("LN_PARAMS"); 
		DijkstraResult res = Dijkstra.dijkstra(rp, edgeweights, g.getNodes(), src, dst, val, this, new HashSet<Edge>(), par); 
        if (res == null) {
        	return null; 
        }
		PathFee pfee = res.turnPathFee(); 
		return new PathFee[] {pfee};
	}

	@Override
	public double compute(int src, int dst, double amt, CreditLinks edgeweights, LNParams params, boolean direct) {
	    double[] ps = params.getParams(src, dst);
	    double base = ps[0];
	    double rate = ps[1];
	    double delay = ps[2];
	    double fee = base + amt * rate;
	    if (direct) fee = 0;
	    return (amt + fee) * delay * LND_RISK_FACTOR + fee + probBias(src, dst);
	}


	  private double probBias(int src, int dst) {
	    double lastFailure;
	    try {
	      lastFailure = (Double) this.lastFailure[observer].get(new Edge(src, dst));
	    } catch (NullPointerException npe) {
	      return 100d / A_PRIORI_PROB;
	    }
//	    System.out.println("src: "+observer+"\ttime: "+time+"********");
	    double deltaHours = (time - lastFailure);//todo
//	    System.out.println(deltaHours);
	    if (deltaHours < 1)
	      return Double.MAX_VALUE;
	    return 100d / (A_PRIORI_PROB * (1 - 1 / (Math.pow(2, deltaHours))));
	  }
	  
		public void initRoutingInfo(Graph g, Random rand) {
			super.initRoutingInfo(g, rand);
			this.lastFailure = new Map[g.getNodes().length];
			for (int i = 0; i < this.lastFailure.length; i++) {
				this.lastFailure[i] = new HashMap<Integer, Double>();
			}
		}


	  public void init(Map[] lastFailure) {
	    this.lastFailure = lastFailure;
	  }

	  public void setObserver(int observer, double time) {
	    this.observer = observer;
	    this.time = time;
	  }
	  
	  public void updateFailure(int observer, double time, Edge e) {
		  this.lastFailure[observer].put(e, time); 
	  }

	@Override
	public double computeFee(int src, int dst, double amt, CreditLinks edgeweights, LNParams params, boolean direct) {
	    double[] ps = params.getParams(src, dst);
	    double base = ps[0];
	    double rate = ps[1];
	    double fee = base + amt * rate;
	    if (direct) fee = 0;
		return fee;
	}

}
