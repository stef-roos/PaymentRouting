package paymentrouting.route.mimo;

import gtna.data.Series;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.canonical.Ring;
import gtna.networks.util.ReadableFile;
import gtna.transformation.Transformation;
import gtna.util.Config;
import paymentrouting.datasets.InitCapacities;
import paymentrouting.datasets.InitCapacities.BalDist;
import paymentrouting.datasets.InitLNParams;
import paymentrouting.datasets.Transactions;
import paymentrouting.datasets.Transactions.TransDist;
import paymentrouting.route.HopDistance;
import paymentrouting.sourcerouting.CheapestPath;
import paymentrouting.sourcerouting.ShortestPath;

public class TestCase {
	
	public static void main(String[] args) {
		ringMimoRun(); 
		//ringMimoFileConversion();  
	}
	
	public static void gengraph() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/testMimo/");
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		
		Network net = new Ring(10, new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new InitLNParams(), new Transactions(400000, -1, TransDist.EXP, false, 4,10)});
		Metric[] m = new Metric[] {};
		Series.generate(net, m, 1); 
		
	}
	
	public static void ringMimoFileConversion() {
		String folderOri = "data/testMimo/ring-original/"; 
		String folderMimo = "data/testMimo/ring-mimo/"; 
		FileFormats.addTimes(folderOri+"tx-original.txt", folderMimo+"tx-mimo.txt", folderOri+"graph.txt_TRANSACTION_LIST", 
				folderMimo+"graph.txt_TRANSACTION_LIST", 100, 2, 4, 5);
		FileFormats.makeMappingFiles(folderMimo+"tx-mimo.txt", folderMimo + "graph.txt_ATOMIC_MAPPING", folderMimo+"mapping.txt", 
				folderMimo + "graph.txt_MIMO_MAPPING");
	}
	
	public static void ringMimoRun() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/testMimo/");
		Config.overwrite("SERIES_GRAPH_WRITE", ""+false);
		
		Network net = new ReadableFile("RING-MIMO", "RING-MIMO", "data/testMimo/ring-mimo/graph.txt", null);
		Metric[] m = new Metric[] {new RoutePaymentMimo(new CheapestPath(new HopDistance()), 1, 0.2), 
				new RoutePaymentMimo(new ShortestPath(new HopDistance()), 1, 0.2)};
		Series.generate(net, m, 1); 
	}
	

}
