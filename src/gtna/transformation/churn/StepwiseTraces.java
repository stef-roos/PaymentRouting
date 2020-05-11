package gtna.transformation.churn;

import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.transformation.Transformation;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.util.PriorityQueue;

public class StepwiseTraces extends Transformation{
	String folder;
	String sep;
	boolean r; 
	double minus;
	boolean[] on;
	int[] count;
	double[][][] traces;
	PriorityQueue<TopologyChange> queue;
	TopologyChange recent;
	int seed;
	String fileMap=null;
	
	




	public StepwiseTraces(String name, String folder, String s, boolean random, double minus, int seed) {
		super("STEP_TRACES", new Parameter[]{new StringParameter("DESC", name), new BooleanParameter("RANDOM",random )});
		this.folder = folder;
		this.sep = s;
		this.r = random;
		this.minus = minus;
		this.seed = seed;
	}
	
	public StepwiseTraces(String name, String folder, String map, String s, boolean random, double minus, int seed) {
		super("STEP_TRACES", new Parameter[]{new StringParameter("DESC", name), new BooleanParameter("RANDOM",random )});
		this.folder = folder;
		this.sep = s;
		this.r = random;
		this.minus = minus;
		this.seed = seed;
		this.fileMap = map;
	}
	



	@Override
	public Graph transform(Graph g) {
		if (on == null){
			g = (new GenerateTraces("TRACE",folder, this.fileMap,this.sep, this.r, this.minus,this.seed)).transform(g);
			Node[] nodes = g.getNodes(); 
			on = new boolean[nodes.length];
			this.count = new int[nodes.length];
			traces = ((Trace)g.getProperty("TRACES")).getTraces();
			  queue = new PriorityQueue<TopologyChange>(nodes.length);
		      for (int i = 0; i < nodes.length; i++){
		        	if (traces[i].length > 0){
		        		queue.add(new TopologyChange(traces[i][0][0], true,i));
		        	}
		        } 
		      while (!queue.isEmpty() && queue.peek().getTime() <= 1){
	      		TopologyChange top = queue.poll();
	      		this.processTop(top);
	          }
		} else {
			if (queue.isEmpty()){
				System.out.println("no more events");
			}
			TopologyChange top = queue.poll();
			this.processTop(top);
			recent = top;
		}
		
		return g;
	}
	

    
    private void processTop(TopologyChange top) {
		int n = top.getNode();
		if (top.isOn()){
			queue.add(new TopologyChange(traces[n][count[n]][1], false,n));
			//next[n] = traces[n][count[n]][1];
			on[n] = true;
		} else {
			count[n]++;
			on[n] = false;
			if (traces[n].length > count[n]){
				queue.add(new TopologyChange(traces[n][count[n]][0], true,n));
			}
			//next[n] = traces[n][count[n]][0];
		}
		
	}
    
    public int getOnNodes(){
    	int c = 0;
    	for (int i = 0; i < on.length; i++){
    		if (this.isOn(i)){
    			c++;
    		}
    	}
    	return c;
    }
    
    public TopologyChange getRecent() {
		return recent;
	}

    public boolean isOn(int i){
    	return this.on[i];
    }

	@Override
	public boolean applicable(Graph g) {
		return true;
	}

}
