package paymentrouting.route.fee;

import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.Distribution;
import paymentrouting.datasets.TransactionList;
import paymentrouting.route.SpeedyMurmurs;
import treeembedding.credit.CreditLinks;
import treeembedding.credit.Transaction;

public class SpeedyMurmursFees extends SpeedyMurmurs {
	int considered; //trees considered 
	int needed; //trees needed
	boolean active; //active merchant 
	FeeComputation fc;
	
	public SpeedyMurmursFees(int t, int c, int p, boolean a, FeeComputation f) {
		super("SPEEDYMURMURS_FEES_"+f.getName()+"_"+c+"_"+p+"_"+a,t);
		this.considered = c;
		this.needed = p;
		this.active = a; 
		this.fc = f; 
	}
	
	
	
	

}
