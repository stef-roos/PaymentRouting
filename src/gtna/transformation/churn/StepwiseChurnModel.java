package gtna.transformation.churn;

import gtna.graph.Graph;
import gtna.graph.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Vector;


public class StepwiseChurnModel extends StepwiseTraces{
	double[][] sess;
	double[][] inter;
	double fac;
	Random rand;
	TopologyChange[] next;

	public StepwiseChurnModel(String session, String intersession, String sep, int seed) {
		super("STEPWISE_CHURN", session, sep, true, 0, seed);
		this.sess = this.initDist(session, sep);
		this.inter = this.initDist(intersession, sep);
		double meanSess = this.getMean(this.sess);
		double meanInter = this.getMean(this.inter);
		this.fac = meanSess/(meanSess+meanInter);
		rand = new Random(seed);
	}

	@Override
	public Graph transform(Graph g) {
		if (on == null){
			Node[] nodes = g.getNodes(); 
			on = new boolean[nodes.length];
			this.count = new int[nodes.length];
			queue = new PriorityQueue<TopologyChange>(nodes.length);
			for (int i = 0; i < nodes.length; i++){
		        	if (rand.nextDouble() < fac){
		        		this.on[i] = true;
		        		double time = this.getRandomLength(this.sess)* rand.nextDouble();
		        		TopologyChange top = new TopologyChange(time,false, i);
		        		queue.add(top);
		        	} else {
		        		this.on[i] = false;
		        		double time = this.getRandomLength(this.inter)* rand.nextDouble();
		        		TopologyChange top = new TopologyChange(time,true, i);
		        		queue.add(top);
		        	}
		        } 
		      
		} else {
			TopologyChange top = queue.poll();
			int n = top.node;
			if (top.isOn()){
				this.on[n] = true;
				double time = top.time+this.getRandomLength(this.sess);
				TopologyChange next = new TopologyChange(time,false,n);
				queue.add(next);
			} else {
				this.on[n] = false;
				double time = top.time+this.getRandomLength(this.inter);
				TopologyChange next = new TopologyChange(time,true,n);
				queue.add(next);
			}
			recent = top;
		}
		
		return g;
	}

	
	
	private double[][] initDist(String file, String sep){
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			Vector<double[]> vec = new Vector<double[]>();
			while ((line=br.readLine()) != null){
				String[] parts = line.split(sep,2);
				double[] val = new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
				vec.add(val);
			}
			br.close();
			double[][] res = new double[vec.size()][];
			for (int i = 0; i < res.length; i++){
				res[i] = vec.get(i);
			}
			return res;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private double getMean(double[][] vals){
		double old = 0;
		double e = 0;
		for (int i = 0; i < vals.length; i++){
			e = e + vals[i][0]*(vals[i][1]-old);
			old = vals[i][1];
		}
		return e;
	}
	
	private double getRandomLength(double[][] vals){
		double r = rand.nextDouble();
		for (int i = 0; i < vals.length; i++){
			if (vals[i][1]> r){
				return vals[i][0];
			}
		}
		return vals[vals.length-1][0];
	}

}
