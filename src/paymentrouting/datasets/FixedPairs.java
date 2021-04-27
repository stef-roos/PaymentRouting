package paymentrouting.datasets;

import java.util.Random;

import gtna.graph.Graph;

public class FixedPairs extends SourceReceiverPairs {
	int src;
	int rcv; 
	boolean bidirectional;

	public FixedPairs(boolean both) {
		super("FIXED_PAIRS");
		this.bidirectional = both;
	}

	@Override
	public void setup(Random rand, Graph g) {
		// select pair
		int nodes = g.getNodeCount();
		this.src = rand.nextInt(nodes);
		this.rcv = rand.nextInt(nodes);
		
	}

	@Override
	public int[] select(Random rand, Graph g) {
		//return the pair, select order if bidirectional
		if (!this.bidirectional || rand.nextBoolean()) {
		   return new int[] {src,rcv};
		} else {
			return new int[] {rcv,src};	
		}
	}

}
