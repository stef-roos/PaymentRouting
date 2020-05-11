package gtna.transformation.spanningtree;

import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.ParentChild;
import gtna.graph.spanningTree.SpanningTree;
import gtna.transformation.Transformation;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

public class BFSRand extends Transformation {
	String rootSelector;
	boolean randomorder; 
	Random rand;

	public BFSRand(Random rand) {
		this("zero", rand);
	}

	public BFSRand(String rootSelector, Random rand) {
		super("SPANNINGTREE_BFS", new Parameter[] { new StringParameter(
				"ROOT_SELECTOR", rootSelector), new BooleanParameter("RANDOM_ORDER",true) });
		this.rootSelector = rootSelector;
		randomorder = true;
        this.rand = rand;
	}

	@Override
	public Graph transform(Graph graph) {
		Node root = selectRoot(graph, rootSelector);

		Node tempNodeFromList;
		Integer[] edges;
		Node[] nodes = graph.getNodes();
		int depth=0;

		LinkedList<Node> todoList = new LinkedList<Node>();

		HashMap<Integer, ParentChild> parentChildMap = new HashMap<Integer, ParentChild>();

		todoList.add(root);
		parentChildMap.put(root.getIndex(), new ParentChild(-1,
				root.getIndex(), 0));
		while (!todoList.isEmpty()){
			depth++;
			HashMap<Integer,Vector<Integer>> newNodes = new HashMap<Integer,Vector<Integer>>();
			while (!todoList.isEmpty()){
				Node next = todoList.poll();
				int[] out = next.getOutgoingEdges();
				for (int i = 0; i < out.length; i++){
					if (!parentChildMap.containsKey(out[i])){
						Vector<Integer> potP = newNodes.get(out[i]);
						if (potP == null){
							potP = new Vector<Integer>();
							newNodes.put(out[i], potP);
						}
						potP.add(next.getIndex());
					}
				}
			}
			Iterator<Entry<Integer,Vector<Integer>>> it = newNodes.entrySet().iterator();
			while (it.hasNext()){
				Entry<Integer,Vector<Integer>> e = it.next();
				int index = e.getKey();
				Vector<Integer> potP = e.getValue();
				int p = potP.get(rand.nextInt(potP.size()));
				parentChildMap.put(index,new ParentChild(p, index,depth));
				todoList.add(nodes[index]);
			}
			
		}
		
		
		int graphNodeSize = graph.getNodes().length;
		int spanningTreeSize = parentChildMap.size();
		if ((spanningTreeSize + 1) < graphNodeSize) {
			for (Node sN : nodes) {
				if (!parentChildMap.containsKey(sN.getIndex())) {
					System.err.print(sN + " missing, connections to ");
					for (int e : sN.generateOutgoingEdgesByDegree()) {
						System.err.print(e + " ");
					}
					System.err.println();
				}
			}
			throw new RuntimeException("Error: graph contains " + graphNodeSize
					+ ", but spanning tree only contains " + spanningTreeSize
					+ " nodes - graph might be disconnected");
		}
		ArrayList<ParentChild> parentChildList = new ArrayList<ParentChild>();
		parentChildList.addAll(parentChildMap.values());
		SpanningTree result = new SpanningTree(graph, parentChildList);

		graph.addProperty(graph.getNextKey("SPANNINGTREE"), result);
		return graph;
	}

	private Node selectRoot(Graph graph, String rootSelector) {
		Node result = null;
		Node[] nodeList = graph.getNodes();
		if (rootSelector == "zero") {
			return nodeList[0];
		} else if (rootSelector == "rand") {
			return nodeList[rand.nextInt(nodeList.length)];
		} else if (rootSelector == "hd") {
			/*
			 * Here, one of the nodes with highest degree will be chosen
			 */
			List<Node> sortedNodeList = Arrays.asList(nodeList.clone());
			Collections.sort(sortedNodeList);
			List<Node> highestDegreeNodes = sortedNodeList.subList(
					(int) (0.9 * sortedNodeList.size()), sortedNodeList.size());
			result = highestDegreeNodes.get(rand.nextInt(highestDegreeNodes
					.size()));
		} else {
			throw new RuntimeException("Unknown root selector " + rootSelector);
		}
		return result;
	}

	@Override
	public boolean applicable(Graph g) {
		return true;
	}

}
