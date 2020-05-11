package treeembedding;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.id.IdentifierSpace;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.routing.Route;
import gtna.routing.RoutingAlgorithm;
import gtna.util.Distribution;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.ParameterListParameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class RoutingGeneral extends Metric{
	int trials;
	double traffic_max;
	double tonce_max;
	double[] traffic;
	double[] t_once;
	private Distribution hopDistribution;
	double avHops = 0;
    RoutingAlgorithm ra;
	Random rand;

	
	public RoutingGeneral(int trials, RoutingAlgorithm ra) {
		super("ROUTING_GENERAL", new Parameter[] { new ParameterListParameter(
				"ROUTING_ALGORITHM", ra) });
		this.ra = ra;
		this.trials = trials;
	}
	




	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		//get coordinates and root, init distributions
		this.traffic = new double[g.getNodeCount()];
		this.t_once = new double[g.getNodeCount()];
		long[] hops = new long[1];
		rand = new Random();
		Node[] nodes = g.getNodes();
		IdentifierSpace idSpace = (IdentifierSpace) g.getProperty("ID_SPACE_0");
		ra.preprocess(g);
		//route trail times
		for (int i = 0; i < trials; i++){
			int src = rand.nextInt(nodes.length);
			int dest = rand.nextInt(nodes.length);
			while (dest == src){
				dest = rand.nextInt(nodes.length);
			}
			
			Route r = this.ra.routeToTarget(g, src, idSpace.getPartition(dest).getRepresentativeIdentifier(), rand);
			int[] route = r.getRoute();
			for (int j = 0; j < r.getHops(); j++){
				boolean contained = false;
				for (int k = 0; k < j; k++){
					if (route[k] == route[j]){
						contained = true;
					}
				}
			   traffic[route[j]]++;
			   if (!contained) t_once[route[j]]++;
			}
			hops = this.inc(hops, r.getHops());
		}
		this.hopDistribution = new Distribution(hops,trials);
		this.avHops = this.hopDistribution.getAverage();
		this.traffic_max = 0;
		this.tonce_max = 0;
		for (int i = 0; i < traffic.length; i++){
			traffic[i] = traffic[i]/trials;
			if (traffic[i] > traffic_max){
				traffic_max = traffic[i];
			}
			t_once[i] = t_once[i]/trials;
			if (t_once[i] > tonce_max){
				tonce_max = t_once[i];
			}
		}
	}

	@Override
	public boolean writeData(String folder) {
		boolean success = true;
		success &= DataWriter.writeWithIndex(
				this.hopDistribution.getDistribution(),
				this.key+"_HOP_DISTRIBUTION", folder);
		success &= DataWriter.writeWithIndex(
				this.hopDistribution.getCdf(),
				this.key+"_HOP_DISTRIBUTION_CDF", folder);
		success &= DataWriter.writeWithIndex(
				this.traffic,
				this.key+"_TRAFFIC", folder);
		Arrays.sort(traffic);
		success &= DataWriter.writeWithIndex(
				this.traffic,
				this.key+"_TRAFFIC_SORTED", folder);
		success &= DataWriter.writeWithIndex(
				this.t_once,
				this.key+"_CONTAINED", folder);
		Arrays.sort(t_once);
		success &= DataWriter.writeWithIndex(
				this.t_once,
				this.key+"_CONTAINED_SORTED", folder);
		return success;
	}

	@Override
	public Single[] getSingles() {
		Single mt = new Single(this.key+"_MAX_TRAFFIC", this.traffic_max);
		Single mc = new Single(this.key+"_MAX_CONTAINED", this.tonce_max);
		Single av = new Single(this.key+"_HOPS_AVERAGE", this.avHops);
		return new Single[]{mt,mc,av};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("ID_SPACE_0");
	}

	private long[] inc(long[] values, int index) {
		try {
			values[index]++;
			return values;
		} catch (ArrayIndexOutOfBoundsException e) {
			long[] valuesNew = new long[index + 1];
			System.arraycopy(values, 0, valuesNew, 0, values.length);
			valuesNew[index] = 1;
			return valuesNew;
		}
	}
	
	

}
