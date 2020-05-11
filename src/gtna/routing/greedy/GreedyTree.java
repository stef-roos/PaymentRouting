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
 * Greedy.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: benni;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna.routing.greedy;

import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;
import gtna.id.Identifier;
import gtna.id.prefix.PrefixSIdentiferSpaceSimple;
import gtna.routing.Route;
import gtna.routing.RoutingAlgorithm;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;

import java.util.ArrayList;
import java.util.Random;

/**
 * @author Andreas Höfer, Stefanie Roos
 * Simple Extension of Greedy Routing for Virtual Tree Prefix Embedding:
 * In case of a local minimum forward the message to the parent in the tree  
 */

public class GreedyTree extends RoutingAlgorithm {

	boolean debug = true;

	private SpanningTree st;
	private int ttl;

	public GreedyTree() {
		super("GREEDYTREE");
		this.ttl = Integer.MAX_VALUE;
	}

	public GreedyTree(int ttl) {
		super("GREEDYTREE", new Parameter[] { new IntParameter("TTL", ttl) });
		this.ttl = ttl;
	}

	@Override
	public Route routeToTarget(Graph graph, int start, Identifier target,
			Random rand) {
		this.st = (SpanningTree) graph.getProperty("SPANNINGTREE"); 
		return this.route(new ArrayList<Integer>(), start, target, rand,
				graph.getNodes());
	}
	
	private Route route(ArrayList<Integer> route, int current,
			Identifier target, Random rand, Node[] nodes) {
		route.add(current);
		
		if (this.isEndPoint(current, target)) {
			return new Route(route, true);
		}
		if (route.size() > this.ttl) {
			return new Route(route, false);
		}

		int closest = target.getClosestNode(nodes[current].getOutgoingEdges(),
				this.identifierSpace.getPartitions());
		if (!target.isCloser(this.identifierSpace.getPartition(closest), this.identifierSpace.getPartition(current))) {
			// if the routing gets stuck, go to the parent
			closest = st.getParent(current);
			// if there is no parent the routing fails
			if (closest == -1)
				return new Route(route, false);
		}

		return this.route(route, closest, target, rand, nodes);
	}
	
	
	
	

/*	private Route routeToRandomTargetPE(Graph graph, int start, Random rand) {
		if (!((PrefixSIdentifier)this.pPE[start].getRepresentativeID()).isSet()){
			return new RouteImpl(new ArrayList<Integer>(), false);
		}
		PrefixSIdentifier target = (PrefixSIdentifier) this.idSpacePE.randomID(rand);
		while (!target.isSet() || this.pPE[start].contains(target) ) {
			target = (PrefixSIdentifier) this.idSpacePE.randomID(rand);
		}
		return this.routePE(new ArrayList<Integer>(), start, target, rand,
				graph.getNodes());
	}
		
	private Route routePE(ArrayList<Integer> route, int current,
			PrefixSIdentifier target, Random rand, Node[] nodes) {
		route.add(current);
		if (this.idSpacePE.getPartitions()[current].contains(target)) {
			return new RouteImpl(route, true);
		}
		if (route.size() > this.ttl) {
			return new RouteImpl(route, false);
		}
		int currentDist = this.idSpacePE.getPartitions()[current]
				.distance(target);
		int minDist = currentDist;
		int minNode = -1;
		for (int out : nodes[current].getOutgoingEdges()) {
			int dist = this.pPE[out].distance(target);
			if (dist < minDist) {
				minDist = dist;
				minNode = out;
			}
		}
		if (minNode == -1) {
			// if the routing gets stuck, go the parent
			minNode = st.getParent(current);
			// if there is no parent the routing fails
			if (minNode == -1)
				return new RouteImpl(route, false);
		}
		return this.routePE(route, minNode, target, rand, nodes);
	}*/
	
	
	@Override
	public boolean applicable(Graph graph) {
		return graph.hasProperty("ID_SPACE_0", PrefixSIdentiferSpaceSimple.class) && graph.hasProperty("SPANNINGTREE");
		// return graph.hasProperty("ID_SPACE_0") && (graph.getProperty("ID_SPACE_0") instanceof PrefixSIdentiferSpaceSimple) && graph.hasProperty("SPANNINGTREE"); 
	}

}
