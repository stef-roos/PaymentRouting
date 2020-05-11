package treeembedding.credit.partioner;

import gtna.graph.Graph;

import java.util.Arrays;
import java.util.Random;

public class NormalDistPartitioner extends Partitioner {
	Random rand;
	double m;
	double sig;

	public NormalDistPartitioner(double mean, double sigma) {
		super("NORMALDIST_PARTITIONER-"+mean+"-"+sigma);
		rand = new Random();
		this.m = mean;
		this.sig = sigma;
	}

	@Override
	public double[] partition(Graph g, int src, int dst, double val, int trees) {
           //randomly divide val on   trees
		   double[] res = new double[trees];
		   //get trees random gaussians and normalize
		   double[] rs = new double[trees];
		   double sum = 0;
		   for (int i = 0; i < rs.length-1; i++){
			   rs[i] = rand.nextGaussian()*this.sig+this.m;
			   sum = sum + rs[i];
		   }
		   for (int i = 1; i < rs.length; i++){
			   rs[i] = rs[i]/sum;
		   }
		   //multiple each random value with val
		   for (int i = 1; i < res.length; i++){
			   res[i] = rs[i]*val;
		   } 
		   return res;
	}
	
	
	
	

}
