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
 * SpanningTreeWTraversalOrder.java
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
package gtna.graph.spanningTree;

import gtna.graph.Graph;
import gtna.io.Filereader;
import gtna.io.Filewriter;
import gtna.util.Config;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Andreas Höfer
 * 
 * Spanning Tree with predefined traversal order. It is assumed that the order in the list of ParentChild relations passed to the
 * constructor defines the traversal order. This traversal order is stored as ParentChild list in the spanning tree object.
 * It is assumed that the traversal order is respected when constructing the children lists of the nodes, 
 * this depends on the behavior of the superclass SpanningTree. 
 */
public class SpanningTreeWTraversalOrder extends SpanningTree {
	private ArrayList<ParentChild> traversalorder;
	private boolean debug = false;
	
	public SpanningTreeWTraversalOrder() {
		super();
	}
	
	/**
	 * @param g the graph
	 * @param pcs list of ParentChild relation defindes the spanning tree, 
	 * the order in the list is also interpreted as the traversal order
	 */
	public SpanningTreeWTraversalOrder(Graph g, ArrayList<ParentChild> pcs) {
		super(g, pcs);
		traversalorder = pcs;
		if (debug && !testTraversalOrder(pcs)) 
			throw new RuntimeException("Traversal order broken.");
	}
	
	public ParentChild[] generateParentChildList() {
		throw new RuntimeException("Invalid Operation: SpanningTreeWTraversalOrder.generateParentChildList()");
	}



	@Override
	public String read(String filename) {
		Filereader fr = new Filereader(filename);

		String key = this.readHeader(fr);

		int nodes = Integer.parseInt(fr.readLine());

		// Construct ParentChild list
		ArrayList<ParentChild> pcs = new ArrayList<ParentChild>();
		String line = null;
		while ((line = fr.readLine()) != null) {
			pcs.add(new ParentChild(line));
		}
		
		this.fill(nodes, pcs);
		this.traversalorder = pcs;
		
		fr.close();

		return key;
	}
		
	@Override
	public boolean write(String filename, String key) {
		Filewriter fw = new Filewriter(filename);

		this.writeHeader(fw, this.getClass(), key);

		this.writeParameter(fw, "Nodes", this.parent.length);

		// LIST OF PCS
		for (ParentChild pc : traversalorder) {
			if (pc != null)
				fw.writeln(pc.toString());
		}

		return fw.close();
	}

	/*
	 * Check whether the traversal order given by the children lists of the nodes and by the ParentChild list fit together
	 */
	private boolean testTraversalOrder(ArrayList<ParentChild> pcs){
		int curNode, curPos = 0;
		Queue<Integer> stNodes = new LinkedList<Integer>();
		stNodes.add(getSrc());
		while (!stNodes.isEmpty()){
			curNode = stNodes.poll();
			System.out.println("st node=" + curNode + ", pcs node=" + pcs.get(curPos).getChild());
			if (curNode != pcs.get(curPos).getChild()){
				return false;
			}
			int[] children = getChildren(curNode);
			for (int c : children)
				stNodes.add(c);
			curPos++;
		}
		return true;
	}
}
