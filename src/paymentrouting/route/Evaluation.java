package paymentrouting.route;

import gtna.data.Series;
import gtna.metrics.Metric;
import gtna.metrics.basic.DegreeDistribution;
import gtna.metrics.basic.ShortestPaths;
import gtna.networks.Network;
import gtna.networks.model.BarabasiAlbert;
import gtna.networks.model.ErdosRenyi;
import gtna.networks.util.ReadableFile;
import gtna.transformation.Transformation;
import gtna.transformation.partition.LargestWeaklyConnectedComponent;
import gtna.util.Config;
import paymentrouting.datasets.InitCapacities;
import paymentrouting.datasets.InitCapacities.BalDist;
import paymentrouting.datasets.TransactionStats;
import paymentrouting.datasets.Transactions;
import paymentrouting.datasets.Transactions.TransDist;
import paymentrouting.route.attack.ColludingDropSplits;
import paymentrouting.route.attack.NonColludingDropSplits;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;

public class Evaluation {
	/**
	 * Eval for paper 'Splitting payments locally while routing interdimensionally'
	 * @param args
	 */

	public static void main(String[] args) {
		
	}
	
	
	/**
	 * dynamic Eval, Figure 2
	 */
	public static void dynamicEval() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/dynComp/");
		int init = 200; 
		int[] trval = {100};
		int runs = 10;
		int trs = 1000000;
		int[] trees = {5}; 
		TransDist td = TransDist.EXP;
		BalDist bd = BalDist.EXP;
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		for (int i = 0; i < trval.length; i++) {
			dynamic(init, trval[i], 20, 
					trs, trees, td, bd, file); 
		}
	}
	
	
	/**
	 * dynamic with concurrency, Table 1
	 */
	public static void dynamicConcurrentEval() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/con-lightning/");
		int init = 200; 
		int[] trval = {100, 25};
		int runs = 10;
		int trs = 1000000;
		int[] trees = {5}; 
		double[] lat = {0.1};
		double[] trh = {100}; 
		TransDist td = TransDist.EXP;
		BalDist bd = BalDist.EXP;
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		for (int j = 0; j < lat.length; j++) {
			for (int k =0; k < trh.length; k++) {
				for (int i = 0; i < trval.length; i++) {
			        dynamicConcurrent(init, trval[i], runs, 
					trs, trees, td, bd, file, lat[j], trh[k]);
				}
			}
		}
	}
	
	/**
	 * evaluation of attack: Figure 6 
	 */
	public static void attackEval() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/attack-lightning/");
		int init = 200; 
		int trval = 100; 
		int runs = 20;
		int trs = 10000;
		int[] trees = {5}; 
		TransDist td = TransDist.EXP;
		BalDist bd = BalDist.EXP;
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		int maxdelay1 = 12; 
		int maxdelay2 = 0;
		
		Transformation[] trans = new Transformation[] {
				new InitCapacities(init,0.05*init, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		int trials = 1;
		boolean up = false; 
		Metric[] m = new Metric[88*trees.length+1]; 
		int index = 0; 
		for (int i = 0; i < 11; i++){
			m[index++] =  new RoutePayment(new ColludingDropSplits(new SplitClosest(speedyMulti[0]), 0.1*i,maxdelay1),trials, up);
			m[index++] =  new RoutePayment(new ColludingDropSplits(new SplitIfNecessary(speedyMulti[0]), 0.1*i,maxdelay1),trials, up);	
			m[index++] =  new RoutePayment(new ColludingDropSplits(new SplitClosest(speedyMulti[0]), 0.1*i,maxdelay2),trials, up);
			m[index++] =  new RoutePayment(new ColludingDropSplits(new SplitIfNecessary(speedyMulti[0]), 0.1*i,maxdelay2),trials, up);
			m[index++] =  new RoutePayment(new NonColludingDropSplits(new SplitClosest(speedyMulti[0]), 0.1*i,maxdelay1),trials, up);
			m[index++] =  new RoutePayment(new NonColludingDropSplits(new SplitIfNecessary(speedyMulti[0]), 0.1*i,maxdelay1),trials, up);	
			m[index++] =  new RoutePayment(new NonColludingDropSplits(new SplitClosest(speedyMulti[0]), 0.1*i,maxdelay2),trials, up);
			m[index++] =  new RoutePayment(new NonColludingDropSplits(new SplitIfNecessary(speedyMulti[0]), 0.1*i,maxdelay2),trials, up);
		}	
		m[index++] = new TransactionStats();
		Series.generate(net, m, runs);
	}
	
	/**
	 * Impact of topology, Figure 7b 
	 */
	public static void topologyEval() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/check-changes/");
		int init = 200; 
		int trval = 100;
		int runs = 20;
		int nodes = 6329;
		int trs = 10000;
		int[] trees = {1,2,3,4,5,6,7,8,9,10}; 
		TransDist td = TransDist.EXP;
		BalDist bd = BalDist.EXP;
		double deg = 10.31;
		runTopologyNoRand(init, trval, runs, nodes, trs, trees, td, bd,true,deg); 
		runTopologyNoRand(init, trval, runs, nodes, trs, trees, td, bd,false,deg);
	}
	
	/**
	 * impact of number of trees/embeddings, distribution and payment amount; Figure 7a + 8 + 9
	 */
	public static void evalValTrees() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/lighting-nopadding/");
        
        int[] trees = {1,2,3,4,5,6,7,8,9,10}; 
        int[] vals = {1, 5, 20, 50, 100, 200, 300};
		int trs = 10000; 

		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		for (int i = 0; i < vals.length; i++) {
                 if (vals[i] == 1 || vals[i] == 50 || vals[i] == 200) {
                	 runLightningIntDim(200, vals[i], 20, 
              				trs, trees, TransDist.EXP, BalDist.EXP, file); 
                 } else {
                	 runLightningIntDimNoRand(200, vals[i], 20, 
               				trs, trees, TransDist.EXP, BalDist.EXP, file);  
                 }
			     if (vals[i] == 100) {
            	 runLightningIntDimNoRand(200, vals[i], 20, 
         				trs, trees, TransDist.EXP, BalDist.NORMAL, file); 
            	 runLightningIntDimNoRand(200, vals[i], 20, 
          				trs, trees, TransDist.NORMAL, BalDist.EXP, file); 
            	 runLightningIntDimNoRand(200, vals[i], 20, 
           				trs, trees, TransDist.NORMAL, BalDist.NORMAL, file); 
			     }
             
		}
	}
	
	/**
	 * evaluation of timeouts, Figure 10 
	 */
	public static void locksEval() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/locks-nopadding/");
		int init = 200; 
		int trval = 100;
		int runs = 20;
		int trs = 10000;
		int[] trees = {1,2,3,4,5,6,7,8,9,10}; 
		TransDist td = TransDist.EXP;
		BalDist bd = BalDist.EXP;
		lightningLocks(init, trval, runs, trs, trees, td, bd,"lightning/lngraph_2020_03_01__04_00.graph"); 
	}
	
	/**
	 * Remainder of class: individual configurations called by the eval functions, which generated results for the evaluation 
	 */
	
	public static void dynamic(int initCap, int trval, int runs, 
			int trs, int[] trees, TransDist td, BalDist bd, String file) {
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, false)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		DistanceFunction hop = new HopDistance();
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		int trials = 1;
		boolean up = true; 
		Metric[] m = new Metric[3+3*trees.length+1]; 
		int index = 0;
		m[index++] =  new RoutePayment(new ClosestNeighbor(hop),trials, up);
		m[index++] =  new RoutePayment(new SplitIfNecessary(hop), trials, up);	
		m[index++] =  new RoutePayment(new SplitClosest(hop),trials, up); 
		for (int i = 0; i < trees.length; i++){
			m[index++] =  new RoutePayment(new ClosestNeighbor(speedyMulti[i]),trials, up);
			m[index++] =  new RoutePayment(new SplitIfNecessary(speedyMulti[i]), trials, up);	
			m[index++] =  new RoutePayment(new SplitClosest(speedyMulti[i]),trials, up);
		}	
		m[index++] = new TransactionStats();
		Series.generate(net, m, runs);
	}	
	
	public static void dynamicConcurrent(int initCap, int trval, int runs, 
			int trs, int[] trees, TransDist td, BalDist bd, String file, double latency, double trh) {
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs,  trh, false)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		DistanceFunction hop = new HopDistance();
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		int trials = 1;
		Metric[] m = new Metric[3+3*trees.length+1]; 
		int index = 0;
		m[index++] =  new RoutePaymentConcurrent(new ClosestNeighbor(hop),trials, latency);
		m[index++] =  new RoutePaymentConcurrent(new SplitIfNecessary(hop), trials, latency);	
		m[index++] =  new RoutePaymentConcurrent(new SplitClosest(hop),trials, latency); 
		for (int i = 0; i < trees.length; i++){
			m[index++] =  new RoutePaymentConcurrent(new ClosestNeighbor(speedyMulti[i]),trials, latency);
			m[index++] =  new RoutePaymentConcurrent(new SplitIfNecessary(speedyMulti[i]), trials, latency);
			m[index++] =  new RoutePaymentConcurrent(new SplitClosest(speedyMulti[i]),trials, latency);
		}	
		m[index++] = new TransactionStats();
		Series.generate(net, m, runs);
	}
	

	
	public static void lightningLocks(int initCap, int trval, int runs, 
			int trs, int[] trees, TransDist td, BalDist bd, String file) {
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[5*trees.length];
		int indx = 0;
		for (int i = 0; i < trees.length; i++) {
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i]);
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i], DistanceFunction.Timelock.MIN);
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i], DistanceFunction.Timelock.MAX);
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i], DistanceFunction.Timelock.CONST, 12);
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i], DistanceFunction.Timelock.CONST, 24);
		}
		int trials = 1;
		boolean up = false; 
		Metric[] m = new Metric[3*speedyMulti.length+1];  
		int index = 0;
		for (int i = 0; i < speedyMulti.length; i++){
			m[index++] =  new RoutePayment(new ClosestNeighbor(speedyMulti[i]),trials, up);
			m[index++] =  new RoutePayment(new SplitIfNecessary(speedyMulti[i]), trials, up);	
			m[index++] =  new RoutePayment(new SplitClosest(speedyMulti[i]),trials, up);
		}
		m[index++] = new TransactionStats(); 
		Series.generate(net, m, runs); 		
	}
	
	
	
	public static void runTopologyNoRand(int initCap, int trval, int runs, int nodes, 
			int trs, int[] trees, TransDist td, BalDist bd, boolean ba, double deg) {
		Transformation[] trans = new Transformation[] {new LargestWeaklyConnectedComponent(),
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net; 
		if (ba) {
			int connect = (int) Math.round(deg/2);
		    net = new BarabasiAlbert(nodes,connect, trans);
		} else {
			net = new ErdosRenyi(nodes, deg, true,trans);
		}
		DistanceFunction hop = new HopDistance();
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		int trials = 1;
		boolean up = false; 
		Metric[] m = new Metric[3+3*trees.length+2]; 
		int index = 0;
		m[index++] =  new RoutePayment(new ClosestNeighbor(hop),trials, up);
		m[index++] =  new RoutePayment(new SplitIfNecessary(hop), trials, up);	
		m[index++] =  new RoutePayment(new SplitClosest(hop),trials, up); 
		for (int i = 0; i < trees.length; i++){
			m[index++] =  new RoutePayment(new ClosestNeighbor(speedyMulti[i]),trials, up);
			m[index++] =  new RoutePayment(new SplitIfNecessary(speedyMulti[i]), trials, up);	
			m[index++] =  new RoutePayment(new SplitClosest(speedyMulti[i]),trials, up);
		}
		m[index++] = new DegreeDistribution(); 
		m[index++] = new TransactionStats(); 
		Series.generate(net, m, runs); 		
	}
	

	
	public static void runLightningIntDimNoRand(int initCap, int trval, int runs, 
			int trs, int[] trees, TransDist td, BalDist bd, String file) {
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		DistanceFunction hop = new HopDistance();
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		int trials = 1;
		boolean up = false; 
		Metric[] m = new Metric[3+3*trees.length+1]; 
		int index = 0;
		m[index++] =  new RoutePayment(new ClosestNeighbor(hop),trials, up);
		m[index++] =  new RoutePayment(new SplitIfNecessary(hop), trials, up);	
		m[index++] =  new RoutePayment(new SplitClosest(hop),trials, up); 
		for (int i = 0; i < trees.length; i++){
			m[index++] =  new RoutePayment(new ClosestNeighbor(speedyMulti[i]),trials, up);
			m[index++] =  new RoutePayment(new SplitIfNecessary(speedyMulti[i]), trials, up);	
			m[index++] =  new RoutePayment(new SplitClosest(speedyMulti[i]),trials, up);
		}	
		m[index++] = new TransactionStats();
		Series.generate(net, m, runs); 		
	}
	
	public static void runLightningIntDim(int initCap, int trval, int runs, 
			int trs, int[] trees, TransDist td, BalDist bd, String file) {
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		DistanceFunction hop = new HopDistance();
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		int trials = 1;
		boolean up = false; 
		Metric[] m = new Metric[4+3*trees.length+3+1]; 
		int index = 0;
		m[index++] =  new RoutePayment(new ClosestNeighbor(hop),trials, up);
		m[index++] =  new RoutePayment(new SplitIfNecessary(hop), trials, up);	
		m[index++] =  new RoutePayment(new SplitClosest(hop),trials, up); 
		m[index++] =  new RoutePayment(new RandomSplit(hop),trials, up); 
		for (int i = 0; i < trees.length; i++){
			m[index++] =  new RoutePayment(new ClosestNeighbor(speedyMulti[i]),trials, up);
			m[index++] =  new RoutePayment(new SplitIfNecessary(speedyMulti[i]), trials, up);	
			m[index++] =  new RoutePayment(new SplitClosest(speedyMulti[i]),trials, up);
			if (i < 3) {
				m[index++] =  new RoutePayment(new RandomSplit(speedyMulti[i]),trials, up); 
			}
		}
		m[index++] = new TransactionStats();
		Series.generate(net, m, runs); 		
	}
	
	public static void runLightningIntDimOnlyRand(int initCap, int trval, int runs, 
			int trs, int[] trees, TransDist td, BalDist bd, String file) {
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		DistanceFunction hop = new HopDistance();
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		int trials = 1;
		boolean up = false; 
		Metric[] m = new Metric[1+trees.length+1]; 
		m[0] = new RoutePayment(new RandomSplit(hop),trials, up);  
		int index = 1;
		for (int i = 0; i < trees.length; i++){
			m[index++] =  new RoutePayment(new RandomSplit(speedyMulti[i]),trials, up); 
		}		 
		m[index++] = new TransactionStats();
		Series.generate(net, m, runs); 		
	}
	
	
	
	public static void getLightningStats(String name, String file) {
		ReadableFile net = new ReadableFile(name, name, file, null);
		Metric[] m = {new DegreeDistribution(), new ShortestPaths()};
		Series.generate(net, m,1);
	}

}
