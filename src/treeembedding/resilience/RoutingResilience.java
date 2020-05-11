package treeembedding.resilience;


import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.partition.Partition;
import gtna.graph.sorting.NodeSorter;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.transformation.partition.WeakConnectivityPartition;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public abstract class RoutingResilience extends Metric {
	NodeSorter sort;
	int[] steps;
	int trials; 
	Random rand;
	double[] avHops;
	double[] avMess ;
	double[] avMessFail;
	double[] avMessSucc;
	double[] avContained;
	double[][] failType;
    int type;
	boolean debug = false;
	
	
	public RoutingResilience(String key, Parameter[] parameters, 
			NodeSorter sort, int[] steps, int trials, int types) {
		super(key, extendParams(parameters,sort,steps.length));
		this.sort = sort;
		this.steps = steps;
		this.trials = trials;
		this.rand = new Random();
		this.type = types;
	}
	
	private static Parameter[] extendParams(Parameter[] parameters, 
			NodeSorter sort, int steps){
		Parameter[] params = new Parameter[parameters.length+2];
		for (int i = 0; i < parameters.length; i++){
			params[i] = parameters[i];
		}
		params[params.length-2] = new StringParameter("NODE_SORTER", sort.getKey());
		params[params.length-1] = new IntParameter("STEPS", steps);
		return params;
	}

	
	

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		init(g);
		Node[] nodes = g.getNodes();
        Node[] order = this.sort.sort(g, rand);
        avHops = new double[steps.length];
        avMess = new double[steps.length];
        avMessFail = new double[steps.length];
        avMessSucc = new double[steps.length];
        avContained = new double[steps.length];
        failType = new double[type][steps.length];
        boolean[] exclude = new boolean[nodes.length];
        for (int j = 0; j < steps.length; j++){
        	System.out.println("Step " + j);
        	int start = j==0?0:steps[j-1];
        	for (int a =start; a < steps[j]; a++){
        		if (a >= order.length) break;
        		exclude[order[a].getIndex()] = true;
        		if(debug)System.out.println("Removed "+ order[a].getIndex());
        	}
        	
        	Partition p = WeakConnectivityPartition.getWeakPartition(g, exclude.clone());
        	int[][] comp= p.getComponents();
        	HashMap<Integer, Integer> cmap = new HashMap<Integer, Integer>(nodes.length);
        	for (int a = 0; a < comp.length; a++){
        		for (int b = 0; b < comp[a].length; b++){
        			cmap.put(comp[a][b], a);
        		}
        	}
//        	try{
//        	BufferedReader br = new BufferedReader(new FileReader("src-dest.txt"));
        	for (int i = 0; i < trials; i++){
//        		if (i == 76){
//        			debug = true;
//        		}
//        		String line = br.readLine();
//        		String[] parts = line.split(" ");
//        		int src = Integer.parseInt(parts[0]);
//        		int dest = Integer.parseInt(parts[1]);
        		int src = rand.nextInt(nodes.length);
        		while (exclude[src]){
        			src = rand.nextInt(nodes.length);
        		}
				int dest = rand.nextInt(nodes.length);
				while (dest == src || exclude[dest]){
					dest = rand.nextInt(nodes.length);
				}
				
				if(debug)System.out.println("src-dest " + src + " " + dest);
				//perform routing and collect stats
				if(debug)System.out.println("get stats " + i);
				int[] stats = this.getRouteStats(src, dest, g, exclude);
				if(debug)System.out.println("Stats " + i + " " + stats[0] + " " + stats[1] + " " + stats[2]);
//				VOUTE.resi[i][0] = stats[0];
//				VOUTE.resi[i][1] = stats[1];
				if (debug)System.out.println("process stats " + i);
				if (stats[0] != -1){
					avHops[j] = avHops[j]+stats[0];
					avContained[j] = avContained[j] + stats[2];
					failType[1][j]++;
					avMessSucc[j] = avMessSucc[j] + stats[1];
				} else {
					int c1 = cmap.get(src);
					int c2 = cmap.get(dest);
					if (c1 != c2){
						failType[0][j]++;
					} 
						failType[stats[3]][j]++;
					
					avMessFail[j] = avMessFail[j] + stats[1];
				}
				avMess[j] = avMess[j] + stats[1];
        	}
        	if (failType[1][j] != 0){
        	avHops[j] = avHops[j]/failType[1][j];
        	avContained[j] = avContained[j]/failType[1][j];
        	avMessSucc[j] = avMessSucc[j]/failType[1][j];
        	}
        	if (trials - failType[1][j] != 0){
        		avMessFail[j] = avMessFail[j]/(trials - failType[1][j]);
        	}
        	avMess[j] = avMess[j]/trials;
        	failType[2][j] = failType[1][j];	
    		failType[1][j] = failType[1][j]/(trials-failType[0][j]);
    		failType[2][j] = failType[2][j]/trials;
        	for (int i =3; i < failType.length; i=i+2){
        		failType[i+1][j] = failType[i][j];	
        		failType[i][j] = (failType[i][j]-failType[0][j])/(trials-failType[0][j]);
        		failType[i+1][j] = failType[i+1][j]/trials;
        	}
        	failType[0][j] = failType[0][j]/trials;
//        	}catch (IOException e){
//            	e.printStackTrace();
//            }
        }
        
	}
	
	protected abstract void init(Graph g);
	
	/**
	 * return stats of routing: 
	 * entry 0: hops of -1 if not successful,
	 * entry 1: messages 
	 * entry 2: #nodes contained in all routes if successful, 0 otherwise
	 * entry 3: type: 0:success (later taken wrt fraction of lookups within same component), 
	 *                1:success (all lookups)
	 *                2:failure due to lookups between diff comp. (determined later)
	 *                3-X: type of failure (ttl expired etc.) <- depends on routing algo
	 * @param src
	 * @param dest
	 * @param g
	 * @param exclude
	 * @return
	 */
	protected abstract int[] getRouteStats(int src, int dest, Graph g, boolean[] exclude);

	@Override
	public boolean writeData(String folder) {
		boolean success = true;
		success &= DataWriter.writeWithIndex(
				this.avHops,
				this.key+"_HOPS", folder);
		success &= DataWriter.writeWithIndex(
				this.avMess,
				this.key+"_MESSAGES", folder);
		success &= DataWriter.writeWithIndex(
				this.avMessSucc,
				this.key+"_MESSAGES_SUCCESS", folder);
		success &= DataWriter.writeWithIndex(
				this.avMessFail,
				this.key+"_MESSAGES_FAILURE", folder);
		success &= DataWriter.writeWithIndex(
				this.avContained,
				this.key+"_CONTAINED", folder);
		for (int i = 0; i < failType.length; i++){
		success &= DataWriter.writeWithIndex(
				this.failType[i],
				this.key+"_FAILTYPE_"+i, folder);
		}		
		return success;
	}

	@Override
	public Single[] getSingles() {
		return new Single[]{};
	}



}
