package paymentrouting.route.bailout;

import java.util.Random;

import gtna.graph.Edge;
import gtna.graph.Graph;
import treeembedding.credit.Transaction;

public abstract class PaymentReaction {
	String name; 
	

	public PaymentReaction(String name) {
		this.name = name; 
	}
	
	public String getName() {
		return name;
	}
	
	//init reactions, e.g., decide on adversary 
	public abstract Transaction[] init(Graph g, Random rand, Transaction[] txs); 
	
	//accept a payment
	public abstract boolean acceptLock(Graph g, int node, double time, double val, Random rand);
	
	//forward hash on return; returns time until reaction, 0 for 'normal' peers  
	public abstract double forwardHash(Graph g, Edge e, double timenow, Random rand);
	
	//resolve failed payment before timeout; time until reaction  
	public abstract double resolve(Graph g, Edge e, double timenow, Random rand);
	
	public abstract boolean receiverReaction(int dst);
	

}
