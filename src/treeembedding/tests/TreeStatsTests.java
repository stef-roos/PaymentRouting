package treeembedding.tests;

import gtna.data.Series;
import gtna.graph.Graph;
import gtna.io.graphReader.GtnaGraphReader;
import gtna.io.graphWriter.GtnaGraphWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.model.BarabasiAlbert;
import gtna.transformation.Transformation;
import gtna.transformation.partition.LargestStronglyConnectedComponent;
import gtna.util.Config;
import treeembedding.treerouting.TreeStats;
import treeembedding.vouteoverlay.Treeembedding;

public class TreeStatsTests {
	
	public static void main(String[] args) {
		testTrees(3); 
	}
	
	public static void testTrees(int trees) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("SERIES_GRAPH_WRITE", "true");
		Transformation[] trans = new Transformation[]{new LargestStronglyConnectedComponent(),
				                                      new Treeembedding("TR", 128,trees)};
		//construct network 
		Network net = new BarabasiAlbert(100,3,trans);
		
		//metrics: execute tree stats metric 
		Metric[] m = new Metric[]{new TreeStats()};
		
		//run test for 3 trials
		Series.generate(net, m, 3);
	}
	
	

}
