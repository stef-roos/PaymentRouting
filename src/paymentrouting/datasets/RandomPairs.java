package paymentrouting.datasets;

import java.util.Random;

import gtna.graph.Graph;

public class RandomPairs extends SourceReceiverPairs {

	public RandomPairs() {
		super("RANDOM_PAIRS");
	}

	@Override
	public void setup(Random rand, Graph g) {
		// nothing to do
		
	}

	@Override
	public int[] select(Random rand, Graph g) {
		int nodes = g.getNodeCount();
		int src = rand.nextInt(nodes);
		int rcv = rand.nextInt(nodes);
		while (rcv == src) {
			rcv = rand.nextInt(nodes);
		}
		return new int[] {src,rcv};
	}

}
