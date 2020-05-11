package treeembedding.credit.partioner;

import gtna.graph.Graph;

public class EqualPartitioner extends Partitioner {

	public EqualPartitioner() {
		super("EQUAL_PARTITIONER");
	}

	@Override
	public double[] partition(Graph g, int src, int dst, double val, int trees) {
		double[] res = new double[trees];
		for (int i = 0; i < trees; i++){
			res[i] = val/trees;
		}
		return res;
	}

	

}
