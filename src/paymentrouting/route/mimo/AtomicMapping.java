package paymentrouting.route.mimo;

import java.util.HashMap;

import gtna.graph.GraphProperty;

public class AtomicMapping extends GraphProperty {
	protected HashMap<Integer, Integer> atomicsetIndex;
	protected HashMap<Integer, int[]> atomicSet;

	@Override
	public boolean write(String filename, String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String read(String filename) {
		// TODO Auto-generated method stub
		return null;
	}

}
