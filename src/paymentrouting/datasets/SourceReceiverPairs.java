package paymentrouting.datasets;

import java.util.Random;

import gtna.graph.Graph;

public abstract class SourceReceiverPairs {
	String name;
	
	
	public SourceReceiverPairs(String n) {
		this.name = n;
	}

	public abstract void setup(Random rand, Graph g);
	
	public abstract int[] select(Random rand, Graph g);

}
