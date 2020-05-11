package gtna.graph.sorting;

import gtna.graph.Graph;
import gtna.graph.GraphProperty;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class TreeRootNodeSorter extends NodeSorter {
	
	public TreeRootNodeSorter() {
		super("TREE_ROOT_SORTER");
	}

	@Override
	public Node[] sort(Graph g, Random rand) {
		GraphProperty[] sp = g.getProperties("SPANNINGTREE");
		Node[] nodes = g.getNodes();
		Node[] sorted = new Node[sp.length];
		for (int i = 0; i < sp.length; i++){
			sorted[i] = nodes[((SpanningTree)sp[i]).getSrc()];
		}
		this.randomize(sorted, rand);
		return sorted;
	}
	


	@Override
	public boolean applicable(Graph g) {
		return g.hasProperty("SPANNINGTREE_0") || g.hasProperty("SPANNINGTREE");
	}

	@Override
	protected boolean isPropertyEqual(Node n1, Node n2) {
		return true;
	}

	

}
