package paymentrouting.datasets;

import java.util.HashMap;
import java.util.Random;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.transformation.Transformation;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;
import treeembedding.credit.CreditLinks;

public class InitCapacities extends Transformation {
	double parameter; //first parameter defining distribution
	double variance; //second parameter defining distribution, set to -1 if not needed
	BalDist bd; // type of distribution (exp, constant, normal implemented) 
	
	public InitCapacities(double expected, double var, BalDist b) {
		super("INIT_CAPACITIES", new Parameter[] {new DoubleParameter("EXPECTED", expected),
				new StringParameter("BAL_DIST",b.name())});
		
		this.bd = b;
		switch (this.bd) {
		case EXP: 
			this.parameter=1/expected; 
		break;
		case CONST: 
			this.parameter = expected;
		break;
		case NORMAL:
			this.parameter = expected;
			this.variance = var;
		break;	
		default: throw new IllegalArgumentException("Unknown distribution type");
		}
	}
	
	public InitCapacities(double expected, BalDist b) {
		this(expected,-1,b); 
	}
	
	public enum BalDist {
		EXP, CONST, NORMAL
	}
	
	
	
	private double getNextVal(Random rand) {
		//do inversion method 
		double r = rand.nextDouble();
		double res; 
		switch (this.bd) {
		case EXP: res = - Math.log(1-r)/this.parameter; 
		break;
		case CONST: res = this.parameter;
		break;
		case NORMAL: res = this.parameter + rand.nextGaussian()*this.variance;
		if (res < 0) {
			res = 0;
		}
		if (res > 2*this.parameter) {
			res = 2*this.parameter;
		}
		break; 
		default: throw new IllegalArgumentException("Unknown distribution type");
		}
		return res;
	}

	@Override
	public Graph transform(Graph g) {
		//init variables 
		Node[] nodes = g.getNodes(); 
		Random rand = new Random();
		HashMap<Edge, double[]> weights = new HashMap<Edge, double[]>(); 
		CreditLinks links = new CreditLinks();
		
		//iterate over links and add 
		for (int i = 0; i < nodes.length; i++) {
			int[] out = nodes[i].getOutgoingEdges();
			
			for (int k = 0; k < out.length; k++) {
				if (out[k] > i) {
					double lower = -1*getNextVal(rand);
					double upper = getNextVal(rand); 
					double[] weight = new double[] {lower,0,upper};
					weights.put(new Edge(i, out[k]), weight);
				}
			}
		}
		links.setWeights(weights);
		g.addProperty("CREDIT_LINKS", links);
		
		return g;
	}

	@Override
	public boolean applicable(Graph g) {
		// TODO Auto-generated method stub
		return true;
	}

}
