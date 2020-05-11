package gtna.graph.sorting;

import gtna.graph.Graph;
import gtna.graph.GraphProperty;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class TreeAttackSorter extends NodeSorter {
    int[] att;
	
	public TreeAttackSorter() {
		super("TREE_ATTACK_SORTER");
	}

	@Override
	public Node[] sort(Graph g, Random rand) {
		GraphProperty[] sp = g.getProperties("SPANNINGTREE");
		Node[] nodes = g.getNodes();
		this.att = new int[nodes.length];
		for (int i = 0; i < sp.length; i++){
			SpanningTree st = (SpanningTree)sp[i];
			this.att[nodes.length-1] = 1;
		}
		Node[] sorted = this.clone(nodes);
		Arrays.sort(sorted, new AttSort());
		this.randomize(sorted, rand);
		return sorted;
	}
	
	private class AttSort implements Comparator<Node> {
		public int compare(Node n1, Node n2) {
			return att[n2.getIndex()]-att[n1.getIndex()];
		}
	}

	@Override
	public boolean applicable(Graph g) {
		return g.hasProperty("SPANNINGTREE_0") || g.hasProperty("SPANNINGTREE");
	}

	@Override
	protected boolean isPropertyEqual(Node n1, Node n2) {
		return att[n1.getIndex()]==att[n2.getIndex()];
	}


}
