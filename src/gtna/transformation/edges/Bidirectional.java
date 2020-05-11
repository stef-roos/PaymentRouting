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
 * Bidirectional.java
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
package gtna.transformation.edges;

import gtna.graph.Edge;
import gtna.graph.Edges;
import gtna.graph.Graph;
import gtna.transformation.Transformation;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;

/**
 * @author benni
 * 
 * if reduce is set to false, for any unidirectional edge without a return edge a return edge is added
 * if reduce is set to true, any unidirectional edge without a return edge is deleted
 */
public class Bidirectional extends Transformation {

	private boolean reduce = false;
	
	public Bidirectional() {
		super("BIDIRECTIONAL", new Parameter[] {new BooleanParameter("REDUCE", false) });
	}
	
	public Bidirectional(boolean reduce) {
		super("BIDIRECTIONAL", new Parameter[] {new BooleanParameter("REDUCE", reduce) });
		this.reduce = reduce;
	}
	
	@Override
	public Graph transform(Graph g) {
		Edges oldEdges = g.getEdges();
		int nrOfEdges = (reduce) ? oldEdges.getEdges().size() : oldEdges.getEdges().size() * 2; 
		Edges edges = new Edges(g.getNodes(), nrOfEdges);
		for (Edge e : oldEdges.getEdges()) {
			// check whether edges should be added or whether the return edge is available as well
			if (!reduce || oldEdges.contains(e.getDst(), e.getSrc())){
				edges.add(e.getSrc(), e.getDst());
				edges.add(e.getDst(), e.getSrc());
			}
		}
		edges.fill();
		return g;
	}

	@Override
	public boolean applicable(Graph g) {
		return true;
	}

}
