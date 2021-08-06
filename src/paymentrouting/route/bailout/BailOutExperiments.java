package paymentrouting.route.bailout;

import gtna.data.Series;
import gtna.graph.Graph;
import gtna.io.graphWriter.GtnaGraphWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.util.ReadableFile;
import gtna.util.Config;
import paymentrouting.util.LightningJsonReader;

public class BailOutExperiments {
	
	public static void main(String[] args) {
		//rewriteGraphWithCap("lightning/lngraph_2020_03_01__04_00.json", "lightning/lngraph_2020_03_01__04_00-cap.graph"); 
		//stats("lightning/lngraph_2021_07_26__14_00.graph", "2021-snapshot.txt"); 
		stats("lightning/lngraph_2020_03_01__04_00-cap.graph", "2020-snapshot");
	}
	
	public static void rewriteGraphWithCap(String file, String output) {
    	LightningJsonReader read = new LightningJsonReader();
    	Graph g = read.read(file);
    	GtnaGraphWriter write = new GtnaGraphWriter();
    	write.writeWithProperties(g, output); 
    }
	
	
	public static void stats(String graph, String name) {
		//read graph file
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/stats/");
		Network net = new ReadableFile("Bailout", "Bailout", graph, null);
		Metric[] m = new Metric[] {new BailoutPotStats("./data/bailout/stats/Stats"+name)};
		Series.generate(net, m, 1); 
	}

}
