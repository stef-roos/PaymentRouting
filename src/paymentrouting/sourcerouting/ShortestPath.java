package paymentrouting.sourcerouting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import treeembedding.credit.CreditLinks;

public class ShortestPath extends SourcePathSelection {
	double[][] baseRateFee; 
	

	public ShortestPath() {
		super("SHORTEST_PATH");  
	}
	
	

	@Override
	public RoutingResult getPaths(CreditLinks edgeweights, Graph g, int src, int dst, double val, int maxtries,
			boolean up) {
		//give everyone same fees
		this.baseRateFee = new double[g.getNodeCount()][2];
		for (int i = 0; i < this.baseRateFee.length; i++) {
			this.baseRateFee[i][0] = 1; 
		}

		HashSet<Edge> excluded = new HashSet<Edge>();
		boolean success = false;
		boolean impossible = false;
		int tries = 0;
		int messages = 0; 
		int hops = 0;
		double fee = 0;
		int[][] path = new int[1][];
		Node[] nodes = g.getNodes(); 
		while (!success && tries < maxtries && !impossible) {
			//execute Dijkstra from receiver 
			DijkstraResult dijk = Dijkstra.dijkstra(edgeweights, nodes, src, dst, val, baseRateFee, excluded);
			success = !(dijk==null); 
			
			//check if impossible or retry
			if (success) {
				//path found but now need to check capacity
				int[] predeccessor = dijk.getPre();
				double[] minCost = dijk.getCosts(); 
				Vector<Integer> potPath = new Vector<Integer>();
				int next = src;
				potPath.add(next);
				while (next != dst && success) {
					int old = next;
					next = predeccessor[next];
					double pot = edgeweights.getPot(old, next);
					if (pot < val + minCost[next]) {
						success = false; 
						excluded.add(new Edge(old, next)); 
					}
					potPath.add(next);
				}
				messages = messages + potPath.size();
				hops = hops + potPath.size();
				if (success) {
					path[0] = new int[potPath.size()];
					for (int l = 0; l < potPath.size(); l++) {
						path[0][l] = potPath.get(l);
					}
					fee = minCost[src]; 
				}
			} else {
				impossible = true; 
			}
			tries++; 
		}
		
		return new RoutingResult(path,success, tries,hops, messages, fee);
	}
	


}
