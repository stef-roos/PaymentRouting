package paymentrouting.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map.Entry;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import treeembedding.credit.CreditLinks;

public class MaxFlow {
	
	/**
	 * compute the max flow between src and dst in given graph with given link weights 
	 * @param g
	 * @param edgeweights
	 * @param src
	 * @param dst
	 * @return
	 */
	public static double getMaxFlow(Graph g, CreditLinks edgeweights, int src, int dst) {
		HashMap<Edge, Double> original = new HashMap<Edge, Double>();  
		 double totalflow = 0; 
		 int[] respath=new int[0];
			while ((respath = findResidualFlow(edgeweights,g.getNodes(),src,dst)) != null){
				//pot flow along this path
				double min = Double.MAX_VALUE;
				for (int i = 0; i < respath.length-1; i++){
					double a = edgeweights.getPot(respath[i], respath[i+1]);
					if (a < min){
						min = a;
					}
				}
				//update flows
				totalflow = totalflow + min;
				for (int i = 0; i < respath.length-1; i++){
					int n1 = respath[i];
					int n2 = respath[i+1];
					double w = edgeweights.getWeight(n1, n2);
					Edge e = n1<n2?new Edge(n1,n2):new Edge(n2,n1);
					if (!original.containsKey(e)){
						original.put(e, w);
					}
					if (n1 < n2){
						edgeweights.setWeight(e, w+min);
					} else {
						edgeweights.setWeight(e, w-min);
					}
				}
			}
			
			weightUpdate(edgeweights, original);
		
		return totalflow; 
	}
	
	/**
	 * check if max flow between src and dst is at least flow 
	 * @param g
	 * @param edgeweights
	 * @param src
	 * @param dst
	 * @param flow
	 * @return
	 */
	public static boolean satisfiesFlow(Graph g, CreditLinks edgeweights, int src, int dst, double flow) {
		 HashMap<Edge, Double> original = new HashMap<Edge, Double>();  
		 double totalflow = 0; 
		 int[] respath=new int[0];
			while (totalflow < flow && 
					(respath = findResidualFlow(edgeweights,g.getNodes(),src,dst)) != null){
				//pot flow along this path
				double min = Double.MAX_VALUE;
				for (int i = 0; i < respath.length-1; i++){
					double a = edgeweights.getPot(respath[i], respath[i+1]);
					if (a < min){
						min = a;
					}
				}
				//update flows
				min = Math.min(min, flow-totalflow);
				totalflow = totalflow + min;
				for (int i = 0; i < respath.length-1; i++){
					int n1 = respath[i];
					int n2 = respath[i+1];
					double w = edgeweights.getWeight(n1, n2);
					Edge e = n1<n2?new Edge(n1,n2):new Edge(n2,n1);
					if (!original.containsKey(e)){
						original.put(e, w);
					}
					if (n1 < n2){
						edgeweights.setWeight(e, w+min);
					} else {
						edgeweights.setWeight(e, w-min);
					}
				}
			}
			
			weightUpdate(edgeweights, original);
			if (flow - totalflow > 0){
				//fail
				return false; 	
			} else {
				return true;
			}
			
	}
	
	private static void weightUpdate(CreditLinks edgeweights, HashMap<Edge, Double> updateWeight){
		Iterator<Entry<Edge, Double>> it = updateWeight.entrySet().iterator();
		while (it.hasNext()){
			Entry<Edge, Double> entry = it.next();
			edgeweights.setWeight(entry.getKey(), entry.getValue());
		}
	}
	
	private static int[] findResidualFlow(CreditLinks ew, Node[] nodes, int src, int dst){
		int[][] pre = new int[nodes.length][2];
		for (int i = 0; i < pre.length; i++){
			pre[i][0] = -1;
		}
		Queue<Integer> q = new LinkedList<Integer>();
		q.add(src);
		pre[src][0] = -2;
		boolean found = false;
		while (!found && !q.isEmpty()){
			int n1 = q.poll();
			int[] out = nodes[n1].getOutgoingEdges();
			for (int n: out){
				if (pre[n][0] == -1 && ew.getPot(n1, n)> 0){
					pre[n][0] = n1;
					pre[n][1] = pre[n1][1]+1;
					if (n == dst){
						int[] respath = new int[pre[n][1]+1];
						while (n != -2){
							respath[pre[n][1]] = n;
							n = pre[n][0];
						}
						return respath;
					}
					q.add(n);
				}
				
			}
		}
		return null;
	}

}
