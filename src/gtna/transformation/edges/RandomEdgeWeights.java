package gtna.transformation.edges;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.weights.EdgeWeights;
import gtna.transformation.Transformation;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.Parameter;

import java.util.ArrayList;
import java.util.Random;



public class RandomEdgeWeights extends Transformation {
	double low;
	double high;

	public RandomEdgeWeights(double low, double high) {
		super("RANDOM_WEIGHTS", new Parameter[]{new DoubleParameter("LOW",low), new DoubleParameter("HIGH",high)});
		this.low = low;
		this.high = high;
	}

	@Override
	public Graph transform(Graph g) {
		EdgeWeights weights = new EdgeWeights();
		ArrayList<Edge> edges = g.getEdges().getEdges();
		Random rand = new Random();
		for (Edge e: edges){
			weights.setWeight(e, low + (high-low)*rand.nextDouble());
		}
		g.addProperty("EDGE_WEIGHTS", weights);
		return g;
	}

	@Override
	public boolean applicable(Graph g) {
		return true;
	}



}
