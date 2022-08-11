package paymentrouting.route.mimo;

import java.util.HashMap;

import gtna.graph.GraphProperty;

public class MimoMapping extends GraphProperty {
	HashMap<Integer, int[]> segmentMapping;
	
	public int[] getSegmentIds(int tx) {
		return this.getSegmentIds(tx); 
	}

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
