package treeembedding;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.GraphProperty;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.Distribution;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class MaintenanceCosts extends Metric{
	Distribution dist;
	
	public MaintenanceCosts() {
		super("TREE_MAINTENANCE");
	}

	

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		Node[] nodes = g.getNodes();
		GraphProperty[] trees = g.getProperties("SPANNINGTREE");
		SpanningTree[] sts = new SpanningTree[trees.length];
		long[] count = new long[nodes.length];
		for (int j = 0; j < sts.length; j++){
			sts[j] = (SpanningTree) trees[j];
		}
		for (int i = 0; i < nodes.length; i++){
			int nr = 0;
			for (int j = 0; j < trees.length; j++){
				Queue<Integer> toCheck = new LinkedList<Integer>();
				toCheck.add(i);
			    while (!toCheck.isEmpty()){
			    	int node = toCheck.poll();
			    	int[] children = sts[j].getChildren(node);
			    	nr = nr + children.length;
			    	for (int k = 0; k < children.length; k++){
			    		toCheck.add(children[k]);
			    	}
			    }
			}
			count = this.inc(count, nr);
		}
		this.dist = new Distribution(count,nodes.length);
	}
	
	private long[] inc(long[] values, int index) {
		try {
			values[index]++;
			return values;
		} catch (ArrayIndexOutOfBoundsException e) {
			long[] valuesNew = new long[index + 1];
			System.arraycopy(values, 0, valuesNew, 0, values.length);
			valuesNew[index] = 1;
			return valuesNew;
		}
	}

	@Override
	public boolean writeData(String folder) {
		// TODO Auto-generated method stub
		boolean success = DataWriter.writeWithIndex(
				this.dist.getDistribution(),
				"TREE_MAINTENANCE_DISTRIBUTION", folder);
		success &= DataWriter.writeWithIndex(
				this.dist.getCdf(),
				"TREE_MAINTENANCE_DISTRIBUTION_CDF", folder);
		return success;
	}

	@Override
	public Single[] getSingles() {
		Single av = new Single("TREE_MAINTENANCE_MEAN", this.dist.getAverage());
		Single med = new Single("TREE_MAINTENANCE_MEDIAN", this.dist.getMedian());
		Single min = new Single("TREE_MAINTENANCE_MIN", this.dist.getMin());
		Single max = new Single("TREE_MAINTENANCE_MAX", this.dist.getMax());
		return new Single[]{av,med, min,max};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("SPANNINGTREE_0");
	}
	
	

}
