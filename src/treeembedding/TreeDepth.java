package treeembedding;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;
import gtna.metrics.Metric;
import gtna.networks.Network;

import java.util.HashMap;

public class TreeDepth extends Metric{
	double avDep;
	double avMaxDep;
	double maxDep;

	public TreeDepth() {
		super("TREE_DEPTH");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		avDep=0;
		avMaxDep=0;
		maxDep=0;
		int r = 0;
		boolean done = false;
		Node[] nodes = g.getNodes();
		while (!done){
			if (g.hasProperty("SPANNINGTREE_"+r)){
		        SpanningTree sp = (SpanningTree) g.getProperty("SPANNINGTREE_"+r);
		        int max = 0;
		        for (int i = 0; i < nodes.length; i++){
		        	int dep = sp.getDepth(nodes[i].getIndex());
		        	avDep = avDep + dep;
		        	if (dep > max){
		        		max = dep;
		        	}
		        }
		        avMaxDep = avMaxDep+max;
		        if (max > maxDep){
		        	maxDep = max;
		        }
		        r++;
			} else {
				done = true;
			}
		    
		}
		avMaxDep = avMaxDep/(double)r;
	    avDep = avDep/(double)(r*nodes.length);
	}

	@Override
	public boolean writeData(String folder) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Single[] getSingles() {
		// TODO Auto-generated method stub
		return new Single[]{new Single("TREE_DEPTH_AV_DEP", avDep), new Single("TREE_DEPTH_MAX_DEP", maxDep),
				new Single("TREE_DEPTH_AV_MAX_DEP", avMaxDep)};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		// TODO Auto-generated method stub
		return g.hasProperty("SPANNINGTREE_0");
	}

}
