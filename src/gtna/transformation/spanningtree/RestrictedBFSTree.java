/* ===========================================================
 * GTNA : Graph-Theoretic Network Analyzer
 * ===========================================================
 *
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors
 *
 * Project Info:  http://www.p2p.tu-darmstadt.de/research/gtna/
 *
 * GTNA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GTNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * ---------------------------------------
 * RestrictedBFS.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: andi;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna.transformation.spanningtree;

import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.ParentChild;
import gtna.graph.spanningTree.SpanningTreeWTraversalOrder;
import gtna.transformation.Transformation;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Andreas Höfer
 * Create a Breath First Search (BFS) spanning tree with given root and restricted max degree (= max nr of children
 */
public class RestrictedBFSTree extends Transformation{

	private int root;
	private int maxdegree;
	/**
	 * @param root index of the root node of the ST; root = -1 => draw the root randomly; root = -2 => root is one node of the one percent highest degree nodes 
	 * @param maxdegree max nr of children of any node in the spanning tree
	 */
	
	public RestrictedBFSTree(int root, int maxdegree){
		super("RESTRICTED_BFS_TREE", new Parameter[]{new IntParameter("ROOT", root), new IntParameter("MAXDEGREE", maxdegree)});
		this.root = root;
		this.maxdegree = maxdegree;
	}
	/* (non-Javadoc)
	 * @see gtna.transformation.Transformation#transform(gtna.graph.Graph)
	 */
	@Override
	public Graph transform(Graph g) {
		Node[] nodes = g.getNodes(); 
		
		int rootIndex = root;
		if (root == -1){
			// choose a node randomly as root node
			rootIndex = (int) Math.floor(nodes.length * Math.random());
		} else {
			if (root == -2)
				// choose one of the nodes with highest degree as root node
				rootIndex = randomHighDegreeNode(nodes).getIndex();
		}
		
		boolean[] visited = new boolean[nodes.length];
		// create a BFS spanning tree
		Queue<Node> stNodeQueue = new LinkedList<Node>();
		Queue<Integer> depthQueue = new LinkedList<Integer>();
		ArrayList<ParentChild> pcs = new ArrayList<ParentChild>();
		stNodeQueue.add(nodes[rootIndex]);
		depthQueue.add(0);
		// root has no parent
		pcs.add(new ParentChild(-1, rootIndex, 0));
		visited[rootIndex] = true;	
		while(!stNodeQueue.isEmpty()){
			Node parent = stNodeQueue.poll(); 
			int depth = depthQueue.poll();
			
			int[] outgoing = parent.getOutgoingEdges();
			// to randomize the order of the children during the construction of the ST shuffle the neighbors
			int[] outgoingShuffled = new int[outgoing.length];
			System.arraycopy(outgoing, 0, outgoingShuffled, 0, outgoing.length);
			shuffle(outgoingShuffled);	
//			System.out.println("outgoing");
//			for (int i=0; i < outgoing.length; i++){
//				System.out.println(outgoing[i]);
//			}
			int children = 0;
//			System.out.println("shuffled");
			for (int n: outgoingShuffled){
//				System.out.println(n);
				if (!visited[n] && children < maxdegree){			
					visited[n] = true;
					pcs.add(new ParentChild(parent.getIndex(), n, depth+1));
					stNodeQueue.add(nodes[n]);
					depthQueue.add(depth+1);
					children++;
				}					
			}
		}
//		for (int i=0; i < pcs.size(); i++)
//			System.out.println("pcs" + "[" + i + "]=" + pcs.get(i));
		g.addProperty("SPANNINGTREE", new SpanningTreeWTraversalOrder(g, pcs));
		return g;
	}

	/* (non-Javadoc)
	 * @see gtna.transformation.Transformation#applicable(gtna.graph.Graph)
	 */
	@Override
	public boolean applicable(Graph g) {
		return true;
	}

//	/*
//	 * Shuffle the array given as param and return a new ArrayList with the shuffled elements of the array
//	 */
//	private ArrayList<Integer> shuffleNeighbours(int[] outgoing){
//		ArrayList<Integer> list = new ArrayList<Integer>(outgoing.length);
//		for (int elem:outgoing)
//			list.add(elem);
//		Collections.shuffle(list);
//		return list;
//	}
	
	/*
	 * find one node of the 1 % nodes with highest degree
	 */
	private Node randomHighDegreeNode(Node[] nodes){
		int onePercent = nodes.length / 100;
		// in networks with less than 100 nodes compute the highest degree node
		if (onePercent == 0)
			onePercent = 1;
		Node[] sortedNodes = nodes.clone();
		shuffle(sortedNodes);
		// sort the array ascending
		Arrays.sort(sortedNodes, 0, nodes.length);
		Node[] onePercentHD = new Node[onePercent];
		// take the last one percent of the nodes as the array is sorted ascending but we need it descending
		for (int i= 0; i < onePercent; i++){
			onePercentHD[i] = sortedNodes[nodes.length - 1 - i]; 
			// System.out.println("OnePercentHD[" + i + "]: " + onePercentHD[i]);
		}	
		return onePercentHD[(int) Math.floor(onePercent * Math.random())];
	}
	
	/*
	 * Shuffle the given object array in place
	 */
	private void shuffle(Object[] array){
		int swappos;
		Object temp;
		for (int i=0; i < array.length; i++){
			swappos = (int) Math.floor((array.length-i) * Math.random());
			temp = array[i + swappos]; 
			array[i + swappos] = array[i];
			array[i] = temp;
		}		
	}
	
	/*
	 * Shuffle the given int array in place
	 */
	private void shuffle(int[] array){
		int swappos, temp;
		for (int i=0; i < array.length; i++){
			swappos = (int) Math.floor((array.length-i) * Math.random());
			temp = array[i + swappos]; 
			array[i + swappos] = array[i];
			array[i] = temp;
		}		
	}

}
