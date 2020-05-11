package paymentrouting.sourcerouting;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

import gtna.data.Single;
import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.Distribution;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;
import paymentrouting.datasets.TransactionList;
import treeembedding.credit.CreditLinks;
import treeembedding.credit.Transaction;

public class SourceRouting extends Metric{
	protected int trials;
	protected Distribution hopDistribution;
	protected double avHops;
	protected Distribution messageDistribution;
	protected double avMess;
	protected Distribution hopDistributionSucc;
	protected double avHopsSucc;
	protected Distribution messageDistributionSucc;
	protected double avMessSucc;
	protected Distribution trysDistribution;
	protected double success;
	protected double successFirst;
	protected double[] succTime;
	Random rand;
	protected boolean update; 
	protected Transaction[] transactions;
	//protected DistanceFunction dist;
	protected boolean log = false;
	protected CreditLinks edgeweights; 
	int tInterval = 1000;
	private double fee_av;
	private double fee_med;
	private double fee_q1;
	private double fee_q2;
	SourcePathSelection sps;

	

	public SourceRouting(SourcePathSelection ps, int trials, boolean up) {
		super("SOURCE_ROUTING", new Parameter[] {new StringParameter("SELECTION", ps.getName()), new IntParameter("TRIALS", trials),
				new BooleanParameter("UPDATE", up)});
		this.sps = ps; 
		this.trials = trials;
		this.update = up; 
		
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		// init values
		rand = new Random();
		edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		this.sps.init(this.update);
		this.transactions = ((TransactionList) g.getProperty("TRANSACTION_LIST")).getTransactions();
		Node[] nodes = g.getNodes();

		this.avHops = 0;
		this.avHopsSucc = 0;
		this.avMess = 0;
		this.avMessSucc = 0;
		this.successFirst = 0;
		this.success = 0;
		long[] trys = new long[2];
		long[] path = new long[2];
		long[] pathSucc = new long[2];
		long[] mes = new long[2];
		long[] mesSucc = new long[2];
		int count = this.transactions.length;
		int len = this.transactions.length / this.tInterval;
		int rest = this.transactions.length % this.tInterval;
		if (rest == 0) {
			this.succTime = new double[len];
		} else {
			this.succTime = new double[len + 1];
		}
		int slot = 0;
		Vector<Double> totalFees = new Vector<Double>(count);
		
		for (int i = 0; i < count; i++) {
			//retrieve info
			Transaction tr = this.transactions[i];
			int src = tr.getSrc();
			int dst = tr.getDst();
			if (log) System.out.println("Src-dst " + src + "," + dst); 
			double val = tr.getVal();
			
			//get paths + last row stats
			RoutingResult res = this.sps.getPaths(edgeweights, g, src, dst, val, this.trials, this.update); 
			
			//update stats 
			if (res.isSuccess()) {
				this.success++;
				if (res.getTries() == 1) {
					this.successFirst++;
				}
				pathSucc = inc(pathSucc, res.getHops());
		    	mesSucc = inc(mesSucc, res.getMes());
		    	totalFees.add(res.getFees()); 
		    	this.succTime[slot]++;
	    	}
			trys = inc(trys,res.getTries());
			path = inc(path, res.getHops());
	    	mes = inc(mes, res.getMes());
		}
		
		// compute final stats
		this.hopDistribution = new Distribution(path, count);
		this.messageDistribution = new Distribution(mes, count);
		this.hopDistributionSucc = new Distribution(pathSucc, (int) this.success);
		this.messageDistributionSucc = new Distribution(mesSucc, (int) this.success);
		this.trysDistribution = new Distribution(trys, count);
		this.avHops = this.hopDistribution.getAverage();
		this.avHopsSucc = this.hopDistributionSucc.getAverage();
		this.avMess = this.messageDistribution.getAverage();
		this.avMessSucc = this.messageDistributionSucc.getAverage();
		this.success = this.success / this.transactions.length;
		this.successFirst = this.successFirst / this.transactions.length;
		double[] tf = new double[totalFees.size()];
		this.fee_av = 0;
		if (tf.length > 0) {
			for (int i = 0; i < tf.length; i++) {
				tf[i] = totalFees.get(i);
				this.fee_av = this.fee_av + tf[i];
			}
			this.fee_av = this.fee_av / tf.length;
			Arrays.sort(tf);
			int mid = tf.length / 2;
			if (tf.length % 2 == 0) {
				this.fee_med = tf[mid - 1] + tf[mid];
			} else {
				this.fee_med = tf[mid];
			}
			int q = tf.length / 4;
			this.fee_q1 = tf[q];
			this.fee_q2 = tf[mid + q];
		}
		if (rest > 0) {
			this.succTime[this.succTime.length - 1] = this.succTime[this.succTime.length - 1] / rest;
		}

		// reset weights for further metrics using them
		if (this.update) {
			this.sps.updateAll(edgeweights);
		}
	}
	
	void weightUpdate(CreditLinks edgeweights, HashMap<Edge, Double> updateWeight){
		Iterator<Entry<Edge, Double>> it = updateWeight.entrySet().iterator();
		while (it.hasNext()){
			Entry<Edge, Double> entry = it.next();
			edgeweights.setWeight(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public boolean writeData(String folder) {
		boolean succ = true;
		succ &= DataWriter.writeWithIndex(this.messageDistribution.getDistribution(),
				this.key+"_MESSAGES", folder);
		succ &= DataWriter.writeWithIndex(this.messageDistributionSucc.getDistribution(),
				this.key+"_MESSAGES_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.hopDistribution.getDistribution(),
				this.key+"_HOPS", folder);
		succ &= DataWriter.writeWithIndex(this.hopDistributionSucc.getDistribution(),
				this.key+"_HOPS_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.trysDistribution.getDistribution(),
				this.key+"_TRYS", folder);
		succ &= DataWriter.writeWithIndex(this.succTime, this.key+"_SUCCESS_TEMPORAL", folder);
		
		return succ;
	}

	@Override
	public Single[] getSingles() {
		Single m_av = new Single(this.key + "_MES_AV", this.avMess);
		Single m_av_succ = new Single(this.key + "_MES_AV_SUCC", this.avMessSucc);
		Single h_av = new Single(this.key + "_HOPS_AV", this.avHops);
		Single h_av_succ = new Single(this.key + "_HOPS_AV_SUCC", this.avHopsSucc);
		
		Single s1 = new Single(this.key + "_SUCCESS_DIRECT", this.successFirst);
		Single s = new Single(this.key + "_SUCCESS", this.success);
		
		Single f1 = new Single(this.key + "_FEE_AV", this.fee_av);
		Single f2 = new Single(this.key + "_FEE_MED", this.fee_med);
		Single f3 = new Single(this.key + "_FEE_Q1", this.fee_q1);
		Single f4 = new Single(this.key + "_FEE_Q3", this.fee_q2);

		return new Single[]{m_av, m_av_succ, h_av, h_av_succ, s1, s, f1, f2, f3, f4};
	}
	

	
	long[] inc(long[] values, int index) {
		try {
			values[index]++;
			return values;
		} catch (ArrayIndexOutOfBoundsException e) {
			long[] valuesNew = new long[index + 1];
			System.arraycopy(values, 0, valuesNew, 0, values.length);
			valuesNew[index] = 1;
			return valuesNew;
		}
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("CREDIT_LINKS") && g.hasProperty("TRANSACTION_LIST");
	}

}
