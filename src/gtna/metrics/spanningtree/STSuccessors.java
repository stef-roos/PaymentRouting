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
 * STSuccessors.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: Andreas Höfer;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna.metrics.spanningtree;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.spanningTree.SpanningTree;
import gtna.graph.spanningTree.SpanningTreeWTraversalOrder;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;

/**
 * @author Andreas Höfer
 *
 * Compute for each node in the spanning tree the avg nr of successor nodes in the tree
 *
 */
public class STSuccessors extends Metric {

	double avg;
	double[] successors;
	
	/**
	 * @param key
	 */
	public STSuccessors() {
		super("STSUCCESSORS");
	}

	/* (non-Javadoc)
	 * @see gtna.metrics.Metric#computeData(gtna.graph.Graph, gtna.networks.Network, java.util.HashMap)
	 */
	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {	
		SpanningTree st =  (SpanningTree) g.getProperty("SPANNINGTREE");
		successors = new double[g.getNodeCount()];
		
		for (int i=0; i < g.getNodeCount(); i++){
			// check whether node at index i is part of the tree
//			if (!st.isPartOfTree(i)){
//				successors[i] = 0;
//				continue;
//			}
//			System.out.println("Got here");
			// do a bfs starting from node i (this is inefficient but should be good enough)
			Queue<Integer> nodeQueue = new LinkedList<Integer>();
			nodeQueue.add(i);
			int current = i;
			while(!nodeQueue.isEmpty()){
				current = nodeQueue.poll();
				int[] children = st.getChildren(current);
				successors[i] += children.length;
				for (int j=0; j < children.length; j++)
					nodeQueue.add(children[j]);
			}
		}
		
		for (int i=0; i < successors.length; i++)
			avg += successors[i] / successors.length;	
	}
	
	/* (non-Javadoc)
	 * @see gtna.metrics.Metric#writeData(java.lang.String)
	 */
	@Override
	public boolean writeData(String folder) {
		boolean success = DataWriter.writeWithIndex(this.successors, "STSUCCESSORS_STSUCCESSORS", folder);
		return success;
	}

	/* (non-Javadoc)
	 * @see gtna.metrics.Metric#getSingles()
	 */
	@Override
	public Single[] getSingles() {
		Single avg = new Single("STSUCCESSORS_AVG", this.avg);
		return new Single[]{avg};
	}

	/* (non-Javadoc)
	 * @see gtna.metrics.Metric#applicable(gtna.graph.Graph, gtna.networks.Network, java.util.HashMap)
	 */
	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("SPANNINGTREE");
	}

}
