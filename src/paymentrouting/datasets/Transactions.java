package paymentrouting.datasets;

import java.util.Random;

import gtna.graph.Graph;
import gtna.transformation.Transformation;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;
import paymentrouting.util.MaxFlow;
import treeembedding.credit.CreditLinks;
import treeembedding.credit.Transaction;

public class Transactions extends Transformation {
	double parameter;//key parameter of distribution
	double variance; //second parameter of distribution, just set to -1 if not needed
	TransDist td; //type distributions for transactions (can be exp, constant, or normal) 
	boolean cutoff;//cutoff a distribution; NOT USED
	int number; // number of transactions 
	boolean time; //should there be a time associated with a transaction? 
	boolean onlyPossible; //limit to transaction that can be satisfied (makes no sense for non-static scenario) 
	int rec; //number of recomputations needed if onlyPossible applied
	int fl; //number of times value was set to maxFlow as no low enough value not found otherwise 
	
	public Transactions(double expected, double var, TransDist d, boolean c, int n, boolean t,
			boolean poss) {
		super("TRANSACTIONS", new Parameter[] {new DoubleParameter("EXPECTED", expected), 
				new StringParameter("TRANS_DIST", d.name()), new BooleanParameter("CUTOFF", c),
				new IntParameter("NUMBER", n), new BooleanParameter("TIME", t),
				new BooleanParameter("ONLY_POSS", poss)}); 
		this.td = d;
		switch (this.td) {
		case EXP: this.parameter= 1/expected; 
		break;
		case CONST: this.parameter = expected;
		break;
		case NORMAL:
			this.parameter = expected;
			this.variance = var;
		break;
		default: throw new IllegalArgumentException("Unknown distribution type");
		}
		this.cutoff = c; 
		this.number = n;
		this.time = t;
		this.onlyPossible = poss;
		this.rec = 0;
		this.fl = 0;
	}
	
	public Transactions(double expected, double var, TransDist d, boolean c, int n, boolean t) {
		this(expected, var, d,c,n,t,false); 
	}
	
	public Transactions(double expected, TransDist d, boolean c, int n, boolean t) {
		this(expected, -1, d,c,n,t, false); 
	}
	
	public Transactions(double expected, TransDist d, boolean c, int n, boolean t, boolean poss) {
		this(expected, -1, d,c,n,t, poss); 
	}
	
	public enum TransDist {
		EXP, CONST, NORMAL 
	}
	

	
	private double getNextVal(Random rand, double flow) {
		//do inversion method 
		boolean done = false;
		double res = 0;
		int it = 0;
		while (!done) {
			double r = rand.nextDouble();
		switch (this.td) {
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
		if (res <= flow) {
			done = true; 
        } else {
			it++;
			if (it == 1000) {
				res = flow;
				done = true;
				this.fl++; 
			}
			if (it == 1) {
				//increase counter for recomputation 
				this.rec++;
			}
		}
		}
		return res;
		 
	}

	@Override
	public Graph transform(Graph g) {
		Random rand = new Random();
		int nodes = g.getNodeCount();
		CreditLinks edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		double sumTime = 0;
		Transaction[] trs = new Transaction[number];
		for (int i = 0; i < number; i++) {
			//select source and destination randomly
			int s = rand.nextInt(nodes);
			int r = rand.nextInt(nodes);
			while (r == s) {
				r = rand.nextInt(nodes);
			}
			
			double flow = Double.MAX_VALUE;
			if (this.onlyPossible) {
				flow = MaxFlow.getMaxFlow(g, edgeweights, s, r);
			}
			
			while (flow == 0) {
				s = rand.nextInt(nodes);
				r = rand.nextInt(nodes);
				while (r == s) {
					r = rand.nextInt(nodes);
				}
				flow = MaxFlow.getMaxFlow(g, edgeweights, s, r);
			}
			double val = getNextVal(rand, flow);
			
			//create transaction 
			Transaction tr; 
			if (!this.time) {
				tr = new Transaction(i,val,s,r);
			} else {
				tr = new Transaction(sumTime,val,s,r);
				double delay = rand.nextDouble();
				sumTime = sumTime + delay; 
			}
			trs[i] = tr;
			
		}
		g.addProperty("TRANSACTION_LIST", new TransactionList(trs, this.time, this.rec, this.fl));
		return g;
	}
	
	

	@Override
	public boolean applicable(Graph g) {
		return g.hasProperty("CREDIT_LINKS");
	}
	

}
