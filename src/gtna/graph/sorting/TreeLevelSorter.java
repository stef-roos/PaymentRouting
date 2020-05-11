package gtna.graph.sorting;

import gtna.graph.Graph;
import gtna.graph.GraphProperty;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class TreeLevelSorter extends NodeSorter {
    int[] levlsum;
	
	public TreeLevelSorter() {
		super("TREE_LEVEL_SORTER");
	}

	@Override
	public Node[] sort(Graph g, Random rand) {
		GraphProperty[] sp = g.getProperties("SPANNINGTREE");
		Node[] nodes = g.getNodes();
		this.levlsum = new int[nodes.length];
		for (int i = 0; i < sp.length; i++){
			SpanningTree st = (SpanningTree)sp[i];
			for (int j = 0; j < nodes.length; j++){
				this.levlsum[j] = this.levlsum[j]+st.getDepth(j);
			}
		}
		Node[] sorted = this.clone(nodes);
		Arrays.sort(sorted, new LevelAsc());
		this.randomize(sorted, rand);
		return sorted;
	}
	
	private class LevelAsc implements Comparator<Node> {
		public int compare(Node n1, Node n2) {
			return levlsum[n1.getIndex()]-levlsum[n2.getIndex()];
		}
	}

	@Override
	public boolean applicable(Graph g) {
		return g.hasProperty("SPANNINGTREE_0") || g.hasProperty("SPANNINGTREE");
	}

	@Override
	protected boolean isPropertyEqual(Node n1, Node n2) {
		return levlsum[n1.getIndex()]==levlsum[n2.getIndex()];
	}

}
