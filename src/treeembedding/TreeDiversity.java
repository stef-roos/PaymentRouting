package treeembedding;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.Distribution;

import java.util.HashMap;
import java.util.HashSet;

public class TreeDiversity extends Metric {
	private Distribution parCountDistribution;
	private double parAv;
	private double parFrac;

	public TreeDiversity() {
		super("TREE_DIVERSITY");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		int k = 0;
		while (g.hasProperty("SPANNINGTREE_"+k)){
			k++;
		}
        SpanningTree[] sps = new SpanningTree[k];
        for (int i = 0; i < k; i++){
        	sps[i] = (SpanningTree)g.getProperty("SPANNINGTREE_"+i);
        }
        Node[] nodes = g.getNodes();
        parFrac = 0;
        int[] dist = new int[0];
        for (int i = 0; i < nodes.length; i++){
        	HashSet<Integer> parents = new HashSet<Integer>(k);
        	for (int j = 0; j < k; j++){
        		int p = sps[j].getParent(i);
        		if (!parents.contains(p)){
        			parents.add(p);
        		}
        	}
        	dist = inc(dist,parents.size());
        	parFrac = parFrac + parents.size()/(double)nodes[i].getInDegree();
        }
        parFrac = parFrac/nodes.length;
        this.parCountDistribution = new Distribution(dist, nodes.length);
        this.parAv = this.parCountDistribution.getAverage();
	}

	@Override
	public boolean writeData(String folder) {
		boolean success = DataWriter.writeWithIndex(
				this.parCountDistribution.getDistribution(), this.key +
				"_PARENT_COUNT", folder);
		success = success & DataWriter.writeWithIndex(
				this.parCountDistribution.getCdf(), this.key +
				"_PARENT_COUNT_CDF", folder);
		return success;
	}

	@Override
	public Single[] getSingles() {
		Single av = new Single(this.key+"_MEAN_PARENTS", this.parAv);
		Single frac = new Single(this.key+"_FRAC_PARENTS", this.parFrac);
		return new Single[]{av, frac};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		// TODO Auto-generated method stub
		return g.hasProperty("SPANNINGTREE_0");
	}

	private int[] inc(int[] values, int index) {
		try {
			values[index]++;
			return values;
		} catch (ArrayIndexOutOfBoundsException e) {
			int[] valuesNew = new int[index + 1];
			System.arraycopy(values, 0, valuesNew, 0, values.length);
			valuesNew[index] = 1;
			return valuesNew;
		}
	}

}
