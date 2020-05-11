package treeembedding.stepwise;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.transformation.churn.DynamicMetric;
import gtna.transformation.churn.Test;
import gtna.util.parameter.Parameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

public class DynamicChord extends DynamicMetric {
	double[] ids;
	Random rand;
	Vector<Double> f;
	int before;
	int after;
	double flast;
	
	public DynamicChord(String name, String sess, String inter, String s, int it){
		super("DYN_CHORD", new Parameter[0], name, sess, inter,s,it,Test.seeds[Test.ind]);
	}

	@Override
	public void initGraph(Graph g, Network n, HashMap<String, Metric> m) {
		rand = new Random();
        this.ids = new double[g.getNodeCount()];
        for (int i = 0; i < ids.length; i++){
        	ids[i] = rand.nextDouble();
        }
        Arrays.sort(ids);
        f = new Vector<Double>(this.it);
        this.computeBalance();
        
        
	}

	@Override
	public void processJoin(Graph g, Network n, HashMap<String, Metric> m,
			int node) {
		if (node > before && node < after){
			this.computeBalance();
		} else {
			f.add((ids[after]-ids[before])*this.step.getOnNodes());
		}

	}

	@Override
	public void processLeave(Graph g, Network n, HashMap<String, Metric> m,
			int node) {
		if (node == before && node == after){
			this.computeBalance();
		} else {
			int last = node-1;
			while (last > -1 && !this.step.isOn(last)){
				last--;
			}
			int next = node+1;
			while (next < this.ids.length && !this.step.isOn(next)){
				next++;
			}
			double interval = 0;
			if (next < this.ids.length && last > -1){
				interval = this.ids[next]-this.ids[last];
			} else {
				if (last == -1){
					interval = this.ids[next];
				} else {
					interval = 1-this.ids[last]; 
				}
			}
			if(interval > flast){
				flast = interval;	
			} 
			this.f.add(flast*this.step.getOnNodes());
		}

	}
	
	private void computeBalance(){
		int last = 0;
        while (last < this.ids.length && !this.step.isOn(last)){
        	last++;
        }
        double max = this.ids[last];
        while (last < this.ids.length){
        	int next = last+1;
        	while (next < this.ids.length && !this.step.isOn(next)){
        		next++;
        	}
        	double interval = 0;
        	if (next < this.ids.length){
        		interval = this.ids[next]-this.ids[last];
        	} else {
        		interval = 1 - this.ids[last];
        	}
        	if (interval > max){
    			max = interval;
    			this.before = last;
    			this.after = next;
    		}
        	last = next;
        }
        this.flast = max;
        f.add(max*this.step.getOnNodes());
	}

	@Override
	public Single[] getSingles() {
		double[] res = this.getAvMaxD(this.f);
		return new Single[]{new Single(this.key +"AV_F", res[0]), new Single(this.key+"MAX_F", res[1])};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return true;
	}
	
	private double[] getAvMaxD(Vector<Double> vec){
		double sum = 0;
		double max = 0;
		for (int i = 0; i < vec.size(); i++){
			double val = vec.get(i);
			sum = sum + val;
			if (val > max){
				max = val;
			}
		}
		return new double[]{sum/vec.size(),max};
	}

}
