package paymentrouting.sourcerouting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Vector;

import gtna.graph.Edge;
import gtna.graph.Node;
import paymentrouting.datasets.LNParams;
import paymentrouting.route.RoutePayment;
import paymentrouting.route.fee.PathFee;
import treeembedding.credit.CreditLinks;

public class Dijkstra {
	
	public static DijkstraResult dijkstra(RoutePayment rp, CreditLinks edgeweights, Node[] nodes, int src, int dst, double val, 
			CostFunction cf, HashSet<Edge> excluded, LNParams par) {
		//setup
		//System.out.println("Start with src " + src + " dst " + dst + " val " + val); 
		boolean success = false; 
	    double[] minCost = new double[nodes.length];
		int[] predeccessor = new int[nodes.length]; 
		double[] fees = new double[nodes.length]; 
		double[] locks = new double[nodes.length]; 
		for (int j = 0; j < nodes.length; j++) {
			minCost[j] = -1;
		}
		minCost[dst] = 0;
		HashMap<Double, Vector<double[]>> mapFeeNode = new HashMap<Double, Vector<double[]>>();
		Vector<double[]> vec = new Vector<double[]>();
		vec.add(new double[] {dst,0});
		mapFeeNode.put(0.0, vec); 
		PriorityQueue<Double> pQueue = new PriorityQueue<Double>(); 
		pQueue.add(0.0);
		
		//run algo
		while (!pQueue.isEmpty() && !success) {
			double min = pQueue.poll();
			Vector<double[]> v = mapFeeNode.remove(min); 
			for (int l = 0; l < v.size(); l++) {
				double[] entry = v.get(l);
				int node = (int) entry[0];
				if (node == src) {
					//System.out.println("Success"); 
					success = true;
					break;
				}
				double fee = entry[1]; 
				//System.out.println("Processing node " + node + " with cost " + min + " and fees " + fee); 
				int[] out = nodes[node].getOutgoingEdges();
				for (int k = 0; k < out.length; k++) {
					int neigh = out[k];
					//System.out.println("Potential next node " + neigh); 
					Edge e = new Edge(neigh, node); 
					//proceed if neigh has not already been visited or edge is excluded 
					if (!excluded.contains(e)) {
						//check if capacity might be sufficient
						double toroute = val + fee; //has to route val + fees for later nodes
						double pot1 = rp.computePotential(neigh, node);
						//System.out.println("pot toroute" + pot1 + " " + toroute); 
						if (neigh == src) {
							//source knows its own capacities and can hence exclude certain edges
                        	if (pot1 < toroute) {
                        		continue; 
                        	}
						}
						double capacity = edgeweights.getPot(node, neigh)+edgeweights.getPot(neigh, node);
						if (capacity < toroute) {
							continue; //total capacity insufficient -> exclude 	
						}
						
						boolean addFee = true;
						if (neigh==src) {
							addFee = false; 
						} 
							//add neigh to queue, compute fees 
						    double potCost = minCost[node] + cf.compute(neigh, node, toroute, edgeweights, par, !addFee);
						    if (minCost[neigh] != -1 && potCost >= minCost[neigh]) {
						    	continue;  
						    }
						    //System.out.println("Passed checks node " + neigh); 
							
						    minCost[neigh] =  potCost; 
						    double feeNeigh = fee + cf.computeFee(neigh, node, toroute, edgeweights, par, !addFee);  	
						    Vector<double[]> cost = mapFeeNode.get(minCost[neigh]);
						    if (cost == null) {
						    	cost = new Vector<double[]>();
						    	mapFeeNode.put(minCost[neigh], cost);
						    	pQueue.add(minCost[neigh]); 
						    }
						    cost.add(new double[] {neigh, feeNeigh}); 
						
						predeccessor[neigh] = node; 
						fees[neigh] = feeNeigh; 
						locks[neigh] = locks[node] + par.getDelay(new Edge( neigh, node)); 
					}
				}
			}
		}
		if (success) {
			return new DijkstraResult(minCost, predeccessor,fees, src, dst, val, locks[src]);
		} else {
			return null; 
		}
	}

}

class DijkstraResult{
	private double[] costs;
	private double[] fees;
	private int[] pre;
	private int src,dst;
	private double val;
	private double lock; 
	
	public DijkstraResult(double[] c, int[] p, double[] f, int src, int dst, double v, double l) {
		this.costs = c;
		this.pre = p;
		this.fees = f;
		this.src = src;
		this.dst = dst;
		this.val = v; 
		this.lock = l; 
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

	public double[] getFees() {
		return fees;
	}

	public void setFees(double[] fees) {
		this.fees = fees;
	}
	
	public PathFee turnPathFee() {
		int l = 1; 
		int cur = src;
		while (cur != dst) {
			cur = pre[cur];
			l++;
		}
		int[] p = new int[l];
		int i = 0;
		p[i] = src; 
		cur = src;
		while (cur != dst) {
			cur = pre[cur];
			i++;
			p[i] = cur;
		}
		PathFee pfee = new PathFee(p,this.fees[src], this.fees[src]+val, this.lock); 
		return pfee; 
	}
}
