package gtna.transformation.util;


import gtna.graph.Edges;
import gtna.graph.Graph;
import gtna.graph.GraphProperty;
import gtna.graph.Node;
import gtna.transformation.Transformation;
import gtna.util.ArrayUtils;

import java.util.HashMap;
import java.util.Random;

public class RandomizeNodeOrder extends Transformation {
	int seed = 1;

	public RandomizeNodeOrder() {
		super("RANDOMIZE_NODE_ORDER");
	}

	@Override
	public Graph transform(Graph g) {
		Random rand;
		rand = new Random();
		Node[] nodes = g.getNodes();
		int[] numbs = new int[nodes.length];
		for (int i = 0; i < numbs.length; i++){
		numbs[i] = i;
	    }
	    ArrayUtils.shuffle(numbs, rand);
	    HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
	    for (int i = 0; i < numbs.length; i++){
	    	map.put(i, numbs[i]);
	    }
	    int edgecount = g.getEdges().size();
	    Node[] nodes2 = Node.init(nodes.length, g);
		Edges edges = new Edges(nodes2, edgecount);
		for (int i = 0; i < nodes.length; i++) {
			int s = map.get(i);
			int[] out = nodes[i].getOutgoingEdges();
			for (int j = 0; j < out.length; j++){
				edges.add(s, map.get(out[j]));
			}
		}
		edges.fill();
		g.setNodes(nodes2);
		return g;
	}

	@Override
	public boolean applicable(Graph g) {
		HashMap<String,GraphProperty> prop = g.getProperties();
		if (prop.isEmpty()){
			return true;
		}
		return false;
	}

}
