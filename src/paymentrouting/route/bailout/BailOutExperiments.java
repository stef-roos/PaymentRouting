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
import paymentrouting.route.bailout.RoutePaymentBailout.AcceptFee;
import paymentrouting.route.bailout.RoutePaymentBailout.BailoutFee;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;
import paymentrouting.sourcerouting.LND;
import paymentrouting.util.LightningJsonReader;

public class BailOutExperiments {
	
	public static void main(String[] args) {
		//rewriteGraphWithCap("lightning/lngraph_2020_03_01__04_00.json", "lightning/lngraph_2020_03_01__04_00-cap.graph"); 
		//stats("lightning/lngraph_2021_07_26__14_00.graph", "2021-snapshot.txt"); 
		testBailoutSmall(); 
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
	
	public static void testBailoutSmall() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		Network net = new ReadableFile("LND", "LND", "./data/simple/simpleCon_graph.txt", new Transformation[] {new InitLNParams()});
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(1), new PaymentReactionGriefingRandom(0.5)};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] {BailoutFee.NORMAL, BailoutFee.FACTOR, BailoutFee.EXPECTED};
		double[] facs = {1,10,1.2}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS, AcceptFee.THRESHOLD, AcceptFee.EXPECTED};
		double[] thres = {Double.MAX_VALUE, 100, 1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length];
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					m[k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/record-test-LND4.txt",reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
				}
			}
		}
		Series.generate(net, m, 1); 
		
		
	}

}
