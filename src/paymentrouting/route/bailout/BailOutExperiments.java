package paymentrouting.route.bailout;

import gtna.data.Series;
import gtna.graph.Graph;
import gtna.io.graphWriter.GtnaGraphWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.util.ReadableFile;
import gtna.transformation.Transformation;
import gtna.util.Config;
import paymentrouting.datasets.InitLNParams;
import paymentrouting.route.HopDistance;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;
import paymentrouting.sourcerouting.LND;
import paymentrouting.util.LightningJsonReader;

public class BailOutExperiments {
	
	public static void main(String[] args) {
		//rewriteGraphWithCap("lightning/lngraph_2020_03_01__04_00.json", "lightning/lngraph_2020_03_01__04_00-cap.graph"); 
		//stats("lightning/lngraph_2021_07_26__14_00.graph", "2021-snapshot.txt"); 
		testRecords(); 
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
	
	public static void testLNDRouting() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		Network net = new ReadableFile("LND", "LND", "./data/simple/simpleCon_graph.txt", new Transformation[] {new InitLNParams()});
		RoutePaymentConcurrent pay = new RoutePaymentConcurrent(new LND(new HopDistance()),1,0.1);
		Metric[] m = new Metric[] {pay};
		Series.generate(net, m, 1); 
		
		
	}
	
	public static void testRecords() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		Network net = new ReadableFile("LND", "LND", "./data/simple/simpleCon_graph.txt", new Transformation[] {new InitLNParams()});
		RoutePaymentConcurrent pay = new RoutePaymentConcurrent(new LND(new HopDistance()),1,0.1, "data/bailout/record-test-LND4.txt");
		Metric[] m = new Metric[] {pay};
		Series.generate(net, m, 1); 
		
		
	}

}
