package paymentrouting.datasets;

import java.util.HashMap;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.metrics.Metric;
import gtna.networks.Network;

public class TransactionStats extends Metric {
	private int rec;
	private int fl; 

	public TransactionStats() {
		super("TRANSACTION_STATS");
		
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		TransactionList tlist = (TransactionList) g.getProperty("TRANSACTION_LIST");
		this.rec = tlist.recomputed;
		this.fl = tlist.eqFlow;
		
	}

	@Override
	public boolean writeData(String folder) {
		return true;
	}

	@Override
	public Single[] getSingles() {
		
		return new Single[] {
				new Single("TRANSACTION_STATS_RECOMPUTED", this.rec),
				new Single("TRANSACTION_STATS_EQUALS_FLOW", this.fl)
		};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("TRANSACTION_LIST");
	}

}
