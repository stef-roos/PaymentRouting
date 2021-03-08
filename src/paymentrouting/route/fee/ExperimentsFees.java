package paymentrouting.route.fee;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import gtna.data.Series;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.model.BarabasiAlbert;
import gtna.networks.util.ReadableFile;
import gtna.transformation.Transformation;
import gtna.util.Config;
import paymentrouting.datasets.InitCapacities;
import paymentrouting.datasets.InitCapacities.BalDist;
import paymentrouting.datasets.Transactions;
import paymentrouting.datasets.Transactions.TransDist;
import paymentrouting.route.ClosestNeighbor;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.SpeedyMurmurs;
import paymentrouting.sourcerouting.ShortestPath;
import paymentrouting.sourcerouting.SourceRouting;

public class ExperimentsFees {
	
	public static void main(String[] args) {
//	   summary1("./data/LNfee-2147483647/READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-2400000.0-EXP--TRANSACTIONS-120000.0-EXP-false-100000-false-false/",
//				"./data/LNfee-2147483647/5percent-SM.txt", "./data/LNfee-2147483647/5percent-All.txt", Integer.MAX_VALUE, "ROUTE_PAYMENT_SUCCESS");
//		summary1("./data/LNfee-2147483647/READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-2400000.0-EXP--TRANSACTIONS-1200000.0-EXP-false-100000-false-false//",
//				"./data/LNfee-2147483647/50percent-SM.txt", "./data/LNfee-2147483647/50percent-All.txt", Integer.MAX_VALUE, "ROUTE_PAYMENT_SUCCESS");
//        System.exit(0); 
		
		int tr = 100000; 
		int initCap = 2400000;
		LNrouting(initCap, (int)(0.05*initCap), 5000,5,TransDist.EXP, BalDist.EXP,tr, Integer.MAX_VALUE, "lightning/lngraph_2020_03_01__04_00.graph"); 

		
	}
	
	public static void oldSetting() {
		int nodes = 5000;
		int connect = 5;
		int[] trees = new int[10];
		int[][] combis = new int[10][];
		for (int i =1; i <= 10; i++) {
			trees[i-1] = i;
			combis[i-1] = new int[] {10,10,i};
		}
//		int[] trees = {};
//		int[][] combis = {{10,10,6}}; 
		int tr = 100000; 
		int initCap = 2400000;
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		//int[] vals = {(int) (0.01*initCap), (int) (0.05*initCap),(int) (0.1*initCap), (int) (0.25*initCap),(int)(0.5*initCap), (int) (initCap)};
		int[] vals = { (int)(0.5*initCap)};
		int[] tints = {Integer.MAX_VALUE, 500,1000,5000,10000};
		for (int i = 0; i < 1; i++) {
			//for (int j = 0; j < tints.length; j++) {
		 Config.overwrite("MAIN_DATA_FOLDER", "./data/LNfee-"+tints[0]+"/");
	     Lightning(initCap,vals[i],nodes,connect,trees, combis, TransDist.EXP, BalDist.EXP, tr, tints[0], file); 
			//}
	    }
	}
	
	
	
	public static void summary1(String folder, String fileRW, String fileNew, int epoch, String metric) {
		try {
		     BufferedWriter bw = new BufferedWriter(new FileWriter(fileRW)); 
		     for (int i = 1; i <= 10; i++) {
		    	 String f = "ROUTE_PAYMENT-1-true-SPEEDYMURMURS_"+i+"-CLOSEST_NEIGHBOR-"+epoch+"-LIGHTNING_FEES_1.0_1.0E-6_false-";
		    	 double[] vals = getSingleVar(folder + f +i+"-"+i+"-false/_singles.txt" , metric+"="); 
		    	 bw.write(i + "	" + vals[0] + "	" + vals[1]);
		    	 bw.newLine();
		     }
		     bw.flush();
		     bw.close(); 
		
		     String[] feesNames = {
		    	"LIGHTNING_FEES_1.0_1.0E-6_false",
		    	//"LIGHTNING_FEES_1.0_1.0E-6_true", 
				"ABSOLUTE_DIFF_1.0_2.0_false", 
				"ABSOLUTE_DIFF_1.0_2.0_true",
				//"RATIO_DIFF_0.05_2.0_false",
				//"RATIO_DIFF_0.05_2.0_true",
				"DI_STASI_1.0_0.01_0.03"
		    };
		    bw = new BufferedWriter(new FileWriter(fileNew)); 
		     for (int i = 1; i <= 10; i++) {
		    	 String f = "ROUTE_PAYMENT-1-true-SPEEDYMURMURS_10-CLOSEST_NEIGHBOR-"+epoch+"-";
		    	 String line = ""+i;
		    	 for (int j = 0; j < feesNames.length; j++) {
		    		 String ff = f + feesNames[j] + "-10-" + i + "-false/"; 
		    		 double[] vals = getSingleVar(folder + ff +"/_singles.txt" , metric+"=");
		    		 line = line + "	" + vals[0] + "	" + vals[1];
		    	 }  
		    	 bw.write(line);
		    	 bw.newLine();
		     }
		     bw.flush();
		     bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static double[] getSingleVar(String file, String single) {
		double[] res = null;
		try { 
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if (line.contains(single)) {
		    		String[] numbers = (line.split("=")[1]).split("	");
		    		res = new double[]{Double.parseDouble(numbers[0]), 
		    				Double.parseDouble(numbers[4])};
		    		break;
		    	}
		    }
		    br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res; 
	}
	
	public static void BAPassive(int initCap, int trval, 
			int nodes, int connect, int[]  treesSM, int[][] combis,
			TransDist td, BalDist bd, int trs) {
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false)};
		Network net = new BarabasiAlbert(nodes,connect, trans);
		DistanceFunction[] speedy = new SpeedyMurmurs[treesSM.length];
		DistanceFunction[] speedyFees = new SpeedyMurmurs[combis.length];	
		for (int i = 0; i < speedy.length; i++) {
			speedy[i] = new SpeedyMurmurs(treesSM[i]);
		}
		for (int i = 0; i < speedyFees.length; i++) {
			speedyFees[i] = new SpeedyMurmurs(combis[i][0]);
		}
		
		int trials = 1;
		Metric[] m = new Metric[treesSM.length+7*combis.length];
		double base = 1;
		double rate = 0.000001; 
		FeeComputation lightning = new LightningFees(base,rate,false); 
		FeeComputation adf = new AbsoluteDiffFee(1,2*base, false);
		FeeComputation rdf = new RatioDiffFee(0.05,2*base, false);
		FeeComputation lightningZero = new LightningFees(base,rate,true); 
		FeeComputation adfZero = new AbsoluteDiffFee(1,2*base, true);
		FeeComputation rdfZero = new RatioDiffFee(0.05,2*base, true);
		FeeComputation distasi = new DiStasi(base,0.01,0.03); 
		FeeComputation[] fc = new FeeComputation[]{
				lightning, adf, rdf, lightningZero, adfZero, rdfZero, distasi
		}; 
		for (int i = 0; i < speedy.length; i++) {
			m[i] = new RoutePaymentFees(new ClosestNeighbor(speedy[i]),trials, true,
					lightning,treesSM[i], treesSM[i],false); 			
		}
		for (int i = 0; i < speedyFees.length; i++) {
			for (int j = 0; j < fc.length; j++) {
			   m[speedy.length+i*7+j] = new RoutePaymentFees(new ClosestNeighbor(speedyFees[i]),
					trials, true, fc[j],combis[i][1], combis[i][2], false); 
			}
		}
		Series.generate(net, m, 20);
		
	}
	
    public static void LNrouting(int initCap, int trval, 
			int nodes, int connect, 
			TransDist td, BalDist bd, int trs, int epoch, String file) {
    	Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false)};
    	Network net;
    	if (file == null) {
    		net = new BarabasiAlbert(nodes,connect, trans);
    	}else {
    		net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
    	}
    	SourceRouting source = new SourceRouting(new ShortestPath(), 1, true);
    	Metric[] m = new Metric[] {source};
    	Series.generate(net, m, 20);
	}
	
	public static void Lightning(int initCap, int trval, 
			int nodes, int connect, int[]  treesSM, int[][] combis,
			TransDist td, BalDist bd, int trs, int epoch, String file) {
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		DistanceFunction[] speedy = new SpeedyMurmurs[treesSM.length];
		DistanceFunction[] speedyFees = new SpeedyMurmurs[combis.length];	
		for (int i = 0; i < speedy.length; i++) {
			speedy[i] = new SpeedyMurmurs(treesSM[i]);
		}
		for (int i = 0; i < speedyFees.length; i++) {
			speedyFees[i] = new SpeedyMurmurs(combis[i][0]);
		}
		
		int trials = 1;
		Metric[] m = new Metric[treesSM.length+7*combis.length];
		double base = 1;
		double rate = 0.000001; 
		FeeComputation lightning = new LightningFees(base,rate,false); 
		FeeComputation adf = new AbsoluteDiffFee(1,2*base, false);
		FeeComputation rdf = new RatioDiffFee(0.05,2*base, false);
		FeeComputation lightningZero = new LightningFees(base,rate,true); 
		FeeComputation adfZero = new AbsoluteDiffFee(1,2*base, true);
		FeeComputation rdfZero = new RatioDiffFee(0.05,2*base, true);
		FeeComputation distasi = new DiStasi(base,0.01,0.03); 
		FeeComputation[] fc = new FeeComputation[]{
				lightning, adf, rdf, lightningZero, adfZero, rdfZero, distasi
		}; 
		for (int i = 0; i < speedy.length; i++) {
			m[i] = new RoutePaymentFees(new ClosestNeighbor(speedy[i]),trials, true,
					lightning,treesSM[i], treesSM[i],false); 			
		}
		for (int i = 0; i < speedyFees.length; i++) {
			for (int j = 0; j < fc.length; j++) {
			   m[speedy.length+i*7+j] = new RoutePaymentFees(new ClosestNeighbor(speedyFees[i]),
					trials, true, fc[j],combis[i][1], combis[i][2], false); 
			}
		}
		Series.generate(net, m, 20);
		
	}
	
	public static void BAPassiveOnlyAbs(int initCap, int trval, 
			int nodes, int connect, int[]  treesSM, int[][] combis,
			TransDist td, BalDist bd, int trs, int epoch) {
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false)};
		Network net = new BarabasiAlbert(nodes,connect, trans);
		DistanceFunction[] speedy = new SpeedyMurmurs[treesSM.length];
		DistanceFunction[] speedyFees = new SpeedyMurmurs[combis.length];	
		for (int i = 0; i < speedy.length; i++) {
			speedy[i] = new SpeedyMurmurs(treesSM[i]);
		}
		for (int i = 0; i < speedyFees.length; i++) {
			speedyFees[i] = new SpeedyMurmurs(combis[i][0]);
		}
		
		int trials = 1;
		Metric[] m = new Metric[treesSM.length+4*combis.length];
		double base = 1;
		double rate = 0.000001; 
		FeeComputation lightning = new LightningFees(base,rate,false); 
		FeeComputation adf = new AbsoluteDiffFee(1,2*base, false);
		FeeComputation adfZero = new AbsoluteDiffFee(1,2*base, true);
		FeeComputation distasi = new DiStasi(base,0.01,0.03); 
		FeeComputation[] fc = new FeeComputation[]{
				lightning, adf, adfZero,  distasi
		}; 
		for (int i = 0; i < speedy.length; i++) {
			m[i] = new RoutePaymentFees(new ClosestNeighbor(speedy[i]),trials, true,
					lightning,treesSM[i], treesSM[i],false, epoch); 			
		}
		for (int i = 0; i < speedyFees.length; i++) {
			for (int j = 0; j < fc.length; j++) {
			   m[speedy.length+i*4+j] = new RoutePaymentFees(new ClosestNeighbor(speedyFees[i]),
					trials, true, fc[j],combis[i][1], combis[i][2], false, epoch); 
			}
		}
		Series.generate(net, m, 20);
		
	}
	
	

}
