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
 * Tree.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: stef;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna.networks.canonical;

import gtna.graph.Edges;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.networks.Network;
import gtna.transformation.Transformation;
import gtna.transformation.spanningtree.BFS;

/**
 * @author stef
 *
 */
public class Tree extends Network {
	int maxdeg;
	int maxdepth;

	/**
	 * @param key
	 * @param nodes
	 * @param transformations
	 */
	public Tree(int maxdegree, int maxdepth,Transformation[] transformations) {
		super("TREE", (int)Math.pow(maxdegree, maxdepth+1), transformations);
		this.maxdeg = maxdegree;
		this.maxdepth = maxdepth;
		
	}

	/* (non-Javadoc)
	 * @see gtna.networks.Network#generate()
	 */
	@Override
	public Graph generate() {
		Graph graph = new Graph(this.getDescription());
		Node[] nodes = Node.init(this.getNodes(), graph);
		Edges edges = new Edges(nodes, 2*this.getNodes() -2);
		int l = 1;
		int lastdeg = 0;
		int deg = 1;
		int nextdeg = maxdeg+1;
		for (int i = 1; i < nodes.length; i++) {
			if (nextdeg == i){
				l++;
				lastdeg = deg;
				deg = nextdeg;
				nextdeg = nextdeg + (int)Math.pow(maxdeg, l);
			}
			int parentindex = (i-deg)%maxdeg + lastdeg;
			edges.add(i, parentindex);
		}
		edges.fill();
		graph.setNodes(nodes);
		//Add SpanningTreeproperty
		graph = (new BFS()).transform(graph);
		return graph;
	}

}
