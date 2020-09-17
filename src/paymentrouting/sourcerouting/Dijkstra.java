package paymentrouting.sourcerouting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Vector;

import gtna.graph.Edge;
import gtna.graph.Node;
import treeembedding.credit.CreditLinks;

public class Dijkstra {
	
	public static DijkstraResult dijkstra(CreditLinks edgeweights, Node[] nodes, int src, int dst, double val, 
			double[][] baseRateFee, HashSet<Edge> excluded) {
		//setup
		boolean success = false; 
	    double[] minCost = new double[nodes.length];
		int[] predeccessor = new int[nodes.length]; 
		for (int j = 0; j < nodes.length; j++) {
			minCost[j] = -1;
		}
		minCost[dst] = 0;
		HashMap<Double, Vector<Integer>> mapFeeNode = new HashMap<Double, Vector<Integer>>();
		Vector<Integer> vec = new Vector<Integer>();
		vec.add(dst);
		mapFeeNode.put(0.0, vec); 
		PriorityQueue<Double> pQueue = new PriorityQueue<Double>(); 
		pQueue.add(0.0);
		
		//run algo
		while (!pQueue.isEmpty() && !success) {
			double min = pQueue.poll();
			Vector<Integer> v = mapFeeNode.remove(min); 
			for (int l = 0; l < v.size(); l++) {
				if (success) break; 
				int node = v.get(l);
				int[] out = nodes[node].getOutgoingEdges();
				for (int k = 0; k < out.length; k++) {
					int neigh = out[k];
					Edge e = new Edge(neigh, node); 
					//proceed if neigh has not already been visited or edge is excluded 
					if (minCost[neigh] == -1 && !excluded.contains(e)) {
						//check if capacity might be sufficient
						double toroute = val + min; //has to route val + fees for later nodes
						double pot1 = edgeweights.getPot(neigh, node);
						if (neigh == src) {
							//source knows its own capacities and can hence exclude certain edges
                        	if (pot1 < toroute) {
                        		continue; 
                        	}
						}
						double pot2 = edgeweights.getPot(node, neigh);
						if (pot1 + pot2 < toroute) {
							continue; //total capacity insufficient -> exclude 	
						}
						
						predeccessor[neigh] = node; 
						if (neigh==src) {
							//done 
							success = true;
							break; 
						} else {
							//add neigh to queue, compute fees 
						    minCost[neigh] = minCost[node] + baseRateFee[neigh][0] + toroute*baseRateFee[neigh][1];
						    Vector<Integer> cost = mapFeeNode.get(minCost[neigh]);
						    if (cost == null) {
						    	cost = new Vector<Integer>();
						    	mapFeeNode.put(minCost[neigh], cost);
						    	pQueue.add(minCost[neigh]); 
						    }
						    cost.add(neigh); 
						}
					}
				}
			}
		}
		if (success) {
			return new DijkstraResult(minCost, predeccessor);
		} else {
			return null; 
		}
	}

}

class DijkstraResult{
	private double[] costs;
	private int[] pre;
	
	public DijkstraResult(double[] c, int[] p) {
		this.costs = c;
		this.pre = p;
	}

	public double[] getCosts() {
		return costs;
	}

	public void setCosts(double[] costs) {
		this.costs = costs;
	}

	public int[] getPre() {
		return pre;
	}

	public void setPre(int[] pre) {
		this.pre = pre;
	}
}
