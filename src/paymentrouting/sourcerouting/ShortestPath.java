package paymentrouting.sourcerouting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import paymentrouting.datasets.LNParams;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.RoutePayment;
import paymentrouting.route.fee.PathFee;
import treeembedding.credit.CreditLinks;

public class ShortestPath extends SourceStep implements CostFunction{
     


	public ShortestPath(DistanceFunction d) {
		super("SHORTEST_PATH", d);
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
	    return 1;
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