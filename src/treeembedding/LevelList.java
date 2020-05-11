package treeembedding;


import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;

import java.util.HashMap;

public class LevelList extends Metric {
	double[] levels;
	
	public LevelList(){
		super("LEVEL_LIST");
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		SpanningTree sp = (SpanningTree) g.getProperty("SPANNINGTREE");
	    Node[] nodes = g.getNodes();
	    levels = new double[nodes.length];
	    for (int i = 0; i < nodes.length; i++){
	    	levels[i] = sp.getDepth(i);
	    }
	    
	}

	@Override
	public boolean writeData(String folder) {
		return DataWriter.writeWithIndex(
				levels,
				"LEVEL_LIST_LIST", folder);
	}

	@Override
	public Single[] getSingles() {
		// TODO Auto-generated method stub
		return new Single[0];
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		// TODO Auto-generated method stub
		return g.hasProperty("SPANNINGTREE");
	}

	

}
