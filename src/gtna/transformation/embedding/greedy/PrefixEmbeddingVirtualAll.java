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
 * PrefixEmbeddingVirtual.java
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
import gtna.graph.spanningTree.SpanningTree;
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
 * 
 * Implementation of the prefix embedding algorithm with virtual trees as published in the paper:
 * Höfer, Roos, Strufe: Greedy Embedding, Routing, and Content Addressing For Darknets, NetSys 2013
 
 * gThe spanning tree used for the embedding is passed as SPANNINGTREE property of the graph.
 * .
 * 
 */
public class PrefixEmbeddingVirtualAll extends Transformation {

	private boolean debug = false;
	private int bitsPerCoord;
	private int idSpaceSize;
	//private boolean balanceSubtrees;
	/**
	 * @param bitsPerCoord maximum nr of bits per coordinate (2^bitsPerCoord is an upper limit for the nr of children of a node);
	 * to achieve unlimited degree set bitsPerCoord to Integer.MaxValue
	 * @param idSpaceSize 
	 */
	public PrefixEmbeddingVirtualAll(int bitsPerCoord, int idSpaceSize) {
		super("PREFIX_EMBEDDING_VIRTUAL", new Parameter[] {new IntParameter("BITSPERCOORD", bitsPerCoord), new IntParameter("IDSPACESIZE", idSpaceSize) });
		this.bitsPerCoord = bitsPerCoord;
		this.idSpaceSize = idSpaceSize;
	}

	
	/* (non-Javadoc)
	 * @see gtna.transformation.Transformation#transform(gtna.graph.Graph)
	 */
	@Override
	public Graph transform(Graph g) {
		Node[] nodes = g.getNodes();
		SpanningTree st = (SpanningTree) g.getProperty("SPANNINGTREE");
		int rootIndex = st.getSrc();	
		// array contains the coordinates of all nodes; is indexed with the node index from the graph 
		short[][] coordinates = new short[nodes.length][];  	
		short[][] coordinatesS = new short[nodes.length][];  
		// root has an empty coordinate vector 
		coordinates[rootIndex] = new short[]{};
		
		Queue<Integer> nodequeue = new LinkedList<Integer>();
		nodequeue.add(rootIndex);
		
		// while currentNode has some child node which has not been visited
		while(!nodequeue.isEmpty()){
			int parent = nodequeue.poll(); 
			short[] parentC = coordinates[parent];
			
			int[] outgoing = st.getChildren(parent);
			if (outgoing.length == 0){
				continue;
			}
			
			// along the spanning tree assign the children indices
			
			// index of the current child
			int child_index = 0;
			
			// maximum nr of additional coords
			int maxNrCoord = (int) Math.ceil(Math.log(outgoing.length+1)/Math.log(2));
			// number of nodes on lowest level
			int levelChange = 2*outgoing.length+2 - ((int)Math.pow(2, maxNrCoord));
			
			// handle the special case of a node with only one child
			if (maxNrCoord == 0){
				maxNrCoord = 1;
				levelChange = 1;
			}
			
			for (int n: outgoing){			
				// the coordinates of n are the coords from the parent plus the additional coordinates
				int nr_of_additional_coordinates = child_index<levelChange? maxNrCoord:(maxNrCoord-1);
				coordinates[n] = new short[parentC.length + nr_of_additional_coordinates];
				System.arraycopy(parentC, 0, coordinates[n], 0, parentC.length);
				
				//compute 0-1 array of bit coordinates
				short[] newCoord;
				if (child_index < levelChange) {
					newCoord = this.intToBitString(child_index, maxNrCoord);
				} else {
					int newIndex = levelChange/2 + (child_index-levelChange);
					newCoord = this.intToBitString(newIndex, maxNrCoord-1);
				}
				
				for (int i = parentC.length; i < coordinates[n].length; i++){
					coordinates[n][i] = newCoord[i-parentC.length];
				}
				nodequeue.add(n);
				child_index++;
				
			}	
			int nr_of_additional_coordinates = child_index<levelChange? maxNrCoord:(maxNrCoord-1);
			coordinatesS[parent] = new short[parentC.length + nr_of_additional_coordinates];
			System.arraycopy(parentC, 0, coordinatesS[parent], 0, parentC.length);
			short[] newCoord;
			if (child_index < levelChange) {
				newCoord = this.intToBitString(child_index, maxNrCoord);
			} else {
				int newIndex = levelChange/2 + (child_index-levelChange);
				newCoord = this.intToBitString(newIndex, maxNrCoord-1);
			}
			
			for (int i = parentC.length; i < coordinatesS[parent].length; i++){
				coordinatesS[parent][i] = newCoord[i-parentC.length];
			}
		}
		
		// create the id spaces, the partitions and assign each partition its identifier/coordinates
		
		PrefixSPartitionSimple[] partitions = new PrefixSPartitionSimple[nodes.length];
		for (int n=0; n < nodes.length; n++){
			partitions[n] = new PrefixSPartitionSimple(new PrefixSIdentifier(coordinates[n]));
		}
		PrefixSIdentiferSpaceSimple idSpace = new PrefixSIdentiferSpaceSimple(partitions, this.bitsPerCoord,this.idSpaceSize,
				true);
		g.addProperty("ID_SPACE_ROUTING", idSpace);
		PrefixSPartitionSimple[] partitionsS = new PrefixSPartitionSimple[nodes.length];
		for (int n=0; n < nodes.length; n++){
			if (coordinatesS[n] != null){
			partitionsS[n] = new PrefixSPartitionSimple(new PrefixSIdentifier(coordinatesS[n]));
			} else {
				partitionsS[n] = new PrefixSPartitionSimple(new PrefixSIdentifier(coordinates[n]));
			}
		}
		PrefixSIdentiferSpaceSimple idSpaceS = new PrefixSIdentiferSpaceSimple(partitionsS, this.bitsPerCoord,this.idSpaceSize,
				true);
		g.addProperty("ID_SPACE_STORAGE", idSpaceS);
		return g;
	}

	/* 
	 * Dummy method
	 */
	@Override
	public boolean applicable(Graph g) {
		return true;
	}

	/*
	 * compute binary representation of child index
	 */
	private short[] intToBitString(int index, int nrOfBits){
		short[] binary_index = new short[nrOfBits];
		int temp = index;
		// the binary conversion computation is done from lowest digit to the highest digit
		for (int i=0; i < nrOfBits; i++){
			binary_index[nrOfBits - i -1] = (temp % 2 == 1) ? (short)1 : 0;
			temp = temp / 2;
			if (temp == 0) break;
		}
		return binary_index;
	}
}
