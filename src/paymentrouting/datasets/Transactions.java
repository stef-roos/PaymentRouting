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
	double parameter;
	double variance; 
	TransDist td;
	boolean cutoff;
	int number;
	double time; //number of transactions per /h per node 
	boolean onlyPossible; 
	int rec;
	int fl;
	SourceReceiverPairs pair; 
	
	public Transactions(double expected, double var, TransDist d, boolean c, int n, double t,
			boolean poss, SourceReceiverPairs srp) {
		super("TRANSACTIONS", new Parameter[] {new DoubleParameter("EXPECTED", expected), 
				new StringParameter("TRANS_DIST", d.name()), new BooleanParameter("CUTOFF", c),
				new IntParameter("NUMBER", n), new DoubleParameter("TIME", t),
				new BooleanParameter("ONLY_POSS", poss), new StringParameter("PAIR_SELECTION", srp.name)}); 
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
		this.pair = srp; 
	}
	
	public Transactions(double expected, TransDist d, boolean c, int n, double t,
			boolean poss, SourceReceiverPairs srp) {
		this(expected, -1, d, c, n, t, poss, srp); 
	}
	
	public Transactions(double expected, double var, TransDist d, boolean c, int n, double t,
			boolean poss) {
		this(expected, var, d, c, n, t, poss, new RandomPairs()); 
	}
	
	public Transactions(double expected, double var, TransDist d, boolean c, int n, double t) {
		this(expected, var, d,c,n,t,false); 
	}
	
	public Transactions(double expected, TransDist d, boolean c, int n, double t) {
		this(expected, -1, d,c,n,t, false); 
	}
	
	public Transactions(double expected, TransDist d, boolean c, int n, double t, boolean poss) {
		this(expected, -1, d,c,n,t, poss); 
	}
	

	/**
	 * for backward compatibility with t as boolean => only used 
	 * @param expected
	 * @param var
	 * @param d
	 * @param c
	 * @param n
	 * @param t
	 * @param poss
	 */
	public Transactions(double expected, double var, TransDist d, boolean c, int n, boolean t, 
			boolean poss) {
		this(expected, var,d,c,n,-1,poss); 
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
				//increse counter for recomputation 
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
		//compute parameters for exponentially distributed arrival intervals 
		double lambdaIA = 0; //(number of transactions per second)^-1 
		if (this.time > -1) {
		    double trsec = nodes*this.time/3600; 
		    lambdaIA = 1/trsec; 
		}
		Transaction[] trs = new Transaction[number];
		this.pair.setup(rand, g);
		for (int i = 0; i < number; i++) {
			//select source and destination randomly
			int[] p = this.pair.select(rand, g);
			int s = p[0];
			int r = p[1]; 
			
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
			if (this.time == -1) {
				tr = new Transaction(i,val,s,r);
			} else {
				tr = new Transaction(sumTime,val,s,r);
				double d = rand.nextDouble();
				//inversion method for d=exp(-lambda*delay) as distribution assumed to be Poisson 
				double delay = -lambdaIA*Math.log(d); 
				sumTime = sumTime + delay; 
			}
			trs[i] = tr;
		}
		g.addProperty("TRANSACTION_LIST", new TransactionList(trs, this.time==-1?false:true, this.rec, this.fl));
		return g;
	}

	@Override
	public boolean applicable(Graph g) {
		return g.hasProperty("CREDIT_LINKS");
	}
	

}
