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
 * PrefixEmbedding.java
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
package gtna.transformation.embedding.greedy;

import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTreeWTraversalOrder;
import gtna.id.prefix.PrefixSIdentiferSpaceSimple;
import gtna.id.prefix.PrefixSIdentifier;
import gtna.id.prefix.PrefixSPartitionSimple;
import gtna.transformation.Transformation;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Andreas Höfer
 * Implementation of the prefix embedding algorithm as published in the paper:
 * Höfer, Roos, Strufe: Greedy Embedding, Routing, and Content Addressing For Darknets, NetSys 2013
 * 
 * The spanning tree used for the embedding is passed in the SPANNINGTREE graph property. 
 * Please note that the bitsPerCoord property of the embedding defines an upper bound for the degree in the spanning tree 
 * 
 */
public class PrefixEmbedding extends Transformation {

	private boolean debug = false;

	private int bitsPerCoord;
	private int idSpaceSize;
	
	/**
	 * @param bitsPerCoord how many bits are used per coordinate / per level of the spanning tree
	 * (2^bitsPerCoord is an upper limit for the nr of children of a node in the spanning tree)
	 * 
	 * @param idSpaceSize size of the id space in bits
	 */
	public PrefixEmbedding(int bitsPerCoord, int idSpaceSize) {
		super("PREFIX_EMBEDDING", new Parameter[] {new IntParameter("BITSPERCOORD", bitsPerCoord), new IntParameter("IDSPACESIZE", idSpaceSize) }); 
		this.bitsPerCoord = bitsPerCoord;
		this.idSpaceSize = idSpaceSize;
	}

	
	/* (non-Javadoc)
	 * @see gtna.transformation.Transformation#transform(gtna.graph.Graph)
	 */
	@Override
	public Graph transform(Graph g) {
		Node[] nodes = g.getNodes();
		
        SpanningTreeWTraversalOrder st = (SpanningTreeWTraversalOrder) g.getProperty("SPANNINGTREE");
		//SpanningTree st = (SpanningTree) g.getProperty("SPANNINGTREE");
		int rootIndex = st.getSrc();
		
		// compute a new embedding 
		// array contains the coordinates of all nodes; is indexed with the node index from the graph 
		short[][] coordinates = new short[nodes.length][];  	
		
		// root has an empty coordinate vector 
		coordinates[rootIndex] = new short[]{};
		
		Queue<Integer> nodequeue = new LinkedList<Integer>();
		nodequeue.add(rootIndex);
		
		// while currentNode has some child node which has not been visited
		while(!nodequeue.isEmpty()){
			int parent = nodequeue.poll(); 
			short[] parentC = coordinates[parent];
			
			int[] outgoing = st.getChildren(parent);
			
			
			// along the spanning tree assign the children indices
			
			// index of the current child
			int child_index = 0;
			
			for (int n: outgoing){			
				// the new coordinates are based on the coords from the parent plus the additional coordinate
				coordinates[n] = new short[parentC.length + 1];
				System.arraycopy(parentC, 0, coordinates[n], 0, parentC.length);
				
				coordinates[n][parentC.length] = (short) child_index;
				
				child_index++;
				
				if (debug){
					System.out.print("node: " + n + " :");
					for (int i=0; i < coordinates[n].length; i++)
						System.out.print(" " + coordinates[n][i]);
					System.out.println();
				}
				nodequeue.add(n);
			}					
		}
		
		// create the id space, the partitions and assign each partition its identifier/coordinates
		
		PrefixSPartitionSimple[] partitions = new PrefixSPartitionSimple[nodes.length];
		for (int n=0; n < nodes.length; n++){
			partitions[n] = new PrefixSPartitionSimple(new PrefixSIdentifier(coordinates[n]));
		}
		PrefixSIdentiferSpaceSimple idSpace = new PrefixSIdentiferSpaceSimple(partitions, this.bitsPerCoord,this.idSpaceSize,false);

		g.addProperty(g.getNextKey("ID_SPACE"), idSpace);
		return g;
	}

	/* 
	 * Dummy method
	 */
	@Override
	public boolean applicable(Graph g) {
		// TODO: check for spanning tree and whether the 
		return true;
	}
}
