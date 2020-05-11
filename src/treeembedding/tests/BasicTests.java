package treeembedding.tests;

import gtna.data.Series;
import gtna.graph.Graph;
import gtna.io.graphReader.GtnaGraphReader;
import gtna.io.graphWriter.GtnaGraphWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.model.BarabasiAlbert;
import gtna.transformation.Transformation;
import gtna.transformation.edges.RandomEdgeWeights;
import gtna.transformation.partition.LargestStronglyConnectedComponent;
import gtna.util.Config;
import treeembedding.treerouting.TreerouteCPLRAP;
import treeembedding.treerouting.TreerouteOnly;
import treeembedding.treerouting.TreerouteSilentW;
import treeembedding.treerouting.TreerouteTDRAP;
import treeembedding.vouteoverlay.Treeembedding;

public class BasicTests {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RandomEdgeWeights trans = new RandomEdgeWeights(0,10);
		Graph g = (new BarabasiAlbert(20,3,null)).generate();
		g = trans.transform(g);
		(new GtnaGraphWriter()).writeWithProperties(g, "data/EW.graph");
		(new GtnaGraphReader()).readWithProperties("data/EW.graph");

	}
	
	public static void BAsimple(int nodes, int t, int tau){
		//transformation: use largest component+ construct t trees with random root, pad coordinates to 128  
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Transformation[] trans = new Transformation[]{new LargestStronglyConnectedComponent(),
				                                      new Treeembedding("TR", 128,t)};
		//construct network 
		Network net = new BarabasiAlbert(nodes,3,trans);
		
		//metrics: execute 3 routing algorithms, each on tau <= t of the trees; 100 random source-dest pairs
		Metric[] m = new Metric[]{new TreerouteSilentW(100,t,tau),
				        new TreerouteOnly(100,t,tau), 
						new TreerouteTDRAP(100,t,tau),
						new TreerouteCPLRAP(100,t,tau)};
		
		//run test for 5 trials
		Series.generate(net, m, 5);
	}

}
