package paymentrouting.sourcerouting;

import java.util.HashSet;
import java.util.Random;

import gtna.graph.Edge;
import gtna.graph.Graph;
import paymentrouting.datasets.LNParams;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.RoutePayment;
import paymentrouting.route.fee.PathFee;
import treeembedding.credit.CreditLinks;

public class CheapestPath extends SourceStep implements CostFunction{
    


	public CheapestPath(DistanceFunction d) {
		super("CHEAPEST_PATH", d);
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
		return this.computeFee(src, dst, amt, edgeweights, params, direct); 
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
