package treeembedding.credit;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.weights.EdgeWeights;
import treeembedding.treerouting.Treeroute;

public class CreditPathWeights {
	Treeroute ra;
	

	public CreditPathWeights(Treeroute ra){
		this.ra = ra;
	}
	
	public double[] route(EdgeWeights edgeweights, int src, int dest, Graph g, Node[] nodes, boolean[] exclude){
		int trees = 0;
		while (g.hasProperty("SPANNINGTREE_"+trees)){
			trees++;
		}
		double[] mins = new double[trees];
		for (int j = 0; j < mins.length; j++){
			int[] path = ra.getRouteBacktrack(src, dest, j, g, nodes, exclude);
			if (path[path.length-1] == dest){
				int a = src;
				int i = 1;
				double min = Double.MAX_VALUE;
				while (i < path.length){
					int b = path[i];
					double w = edgeweights.getWeight(new Edge(a,b));
					if (w < min){
						min = w;
					}
					a = b;
					i++;
				}
				mins[j] = min;
			}
		}
		return mins;
	}

}
