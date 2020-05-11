package gtna.transformation.churn;

import gtna.graph.Graph;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.util.HashMap;

public abstract class DynamicMetric extends Metric {
	protected StepwiseTraces step;
	protected int it;
	double[] nCount;

	public DynamicMetric(String key, Parameter[] parameters, String name, String folder, String m, String s, boolean random, double minus, int it, int seed) {
		super(key, extendParams(parameters,new Parameter[]{new StringParameter("TRACE", name), new BooleanParameter("RANDOM", random)}));
		this.step = new StepwiseTraces(name,folder,m,s,random,minus,seed+1);
		this.it = it;
		nCount = new double[it+1];
	}
	
	public DynamicMetric(String key, Parameter[] parameters, String name, String folder, String s, boolean random, double minus, int it, int seed) {
		this(key,parameters,name,folder,null,s,random,minus,it,seed);
	}
	
	public DynamicMetric(String key, Parameter[] parameters, String name, String sess, String inter, String s, int it, int seed) {
		super(key, extendParams(parameters,new Parameter[]{new StringParameter("TRACE", name)}));
		this.step = new StepwiseChurnModel(sess,inter,s,seed+1);
		this.it = it;
		nCount = new double[it+1];
	}
	
	public static Parameter[] extendParams(Parameter[] parameters, Parameter[] parameters2){
		Parameter[] params = new Parameter[parameters.length+parameters2.length];
		for (int i = 0; i < parameters.length; i++){
			params[i] = parameters[i];
		}
		for (int i = 0; i < parameters2.length; i++){
			params[i+parameters.length] = parameters2[i];
		}		
		return params;
	}
	
	

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		g = this.step.transform(g);
        this.initGraph(g, n, m);
        nCount[0]=this.step.getOnNodes();
        for (int j = 1; j <= it; j++){
        	g = this.step.transform(g);
        	TopologyChange top = step.getRecent();
        	if (top.isOn()){
        		//System.out.println("Join " + top.node + " at it " + j); 
        		nCount[j] = nCount[j-1]+1;
        		this.processJoin(g, n, m, top.node);
        	} else {
        		//System.out.println("Leave " + top.node + " at it " + j);  
        		nCount[j] = nCount[j-1]-1;
        		this.processLeave(g, n, m, top.node);
        	}
        }
	}
	
	public boolean writeData(String folder) {
		return DataWriter.writeWithIndex(nCount, this.key+"_ON_NODES", folder);
	}

	public abstract void initGraph(Graph g, Network n, HashMap<String, Metric> m);
	
	public abstract void processJoin(Graph g, Network n, HashMap<String, Metric> m, int node);
	
	public abstract void processLeave(Graph g, Network n, HashMap<String, Metric> m, int node);

	

}
