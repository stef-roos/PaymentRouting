package treeembedding.credit.partioner;

import gtna.graph.Graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class RandomPartitioner extends Partitioner {
	Random rand;

	public RandomPartitioner() {
		super("RANDOM_PARTITIONER");
		rand = new Random();
	}

	@Override
	public double[] partition(Graph g, int src, int dst, double val, int trees) {
           //randomly divide val on   trees
		   double[] res = new double[trees];
		   //get random values between 0 and 1, marking borders between values
		   double[] rs = new double[trees];
		   for (int i = 0; i < rs.length-1; i++){
			   rs[i] = rand.nextDouble();
		   }
		   rs[rs.length-1] = 1;
		   Arrays.sort(rs);
		   //i-th route is assigned (rs[i]-rs[i-1])*val
		   res[0] = val*rs[0];
		   for (int i = 1; i < res.length; i++){
			   res[i] = val*(rs[i]-rs[i-1]);
		   } 
		   return res;
	}

	

	
	

}
