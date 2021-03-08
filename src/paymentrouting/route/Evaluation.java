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

public class Evaluation {
	/**
	 * Eval for paper 'Splitting payments locally while routing interdimensionally'
	 * @param args
	 */

	public static void main(String[] args) {
		attackEval();  
	}
	
	/**
	 * attack evaluation on the lightning graph;
	 * Figure 5 in paper  
	 */
	public static void attackEval() {
		//Storage settings
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/attack-lightning/");
		//Network parameters
		int init = 200; 
		int trval = 100; 
		int trs = 10000;
		int[] trees = {5}; 
		TransDist td = TransDist.EXP;
		BalDist bd = BalDist.EXP;
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {
				new InitCapacities(init,0.05*init, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		//Attack parameters: maximal delay introduced by attacker, we try two values 
		int maxdelay1 = 12; 
		int maxdelay2 = 0;
	
		//Routing parameters 
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		int trials = 1;
		boolean up = false; 
		Metric[] m = new Metric[88*trees.length+1]; 
		int runs = 20;
		
		//execute simulation: colluding and non-colluding with the two splitting methods explored in the paper and the two delays defined above
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
	 * evaluation with dynamic adjustments of weights (general setup), Figure 6 in paper  
	 */
	public static void dynamicEval() {
		//storage setup
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/dyn-lightning/");
		//network parameters 
		int init = 200; 
		int[] trval = {10,100};
		int trs = 1000000;
		TransDist td = TransDist.EXP;
		BalDist bd = BalDist.EXP;
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		//routing parameters: trees for speedymurmurs
		int[] trees = {1,3,5}; 
		
		//execute all experiments 
		int runs = 20;
		for (int i = 0; i < trval.length; i++) {
			dynamic(init, trval[i], 20, 
					trs, trees, td, bd, file); 
		}
	}
	
	/**
	 * dynamic experiments for one network
	 * @param initCap: average capacity
	 * @param trval: average transaction value
	 * @param runs: number of runs
	 * @param trs: number of transactions
	 * @param trees: number of trees for Interdimensional SpeedyMurmurs (can be multiple) 
	 * @param td: type of transaction distribution
	 * @param bd: type of capacity distribution 
	 * @param file: topology file for graph 
	 */
	public static void dynamic(int initCap, int trval, int runs, 
			int trs, int[] trees, TransDist td, BalDist bd, String file) {
		//generate network
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, false)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		//instantiate routing 
		DistanceFunction hop = new HopDistance();
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		int trials = 1;
		boolean up = true; 
		Metric[] m = new Metric[3+3*trees.length+1]; 
		int index = 0; 
		//HopDistance routing for three splitting protocols 
		m[index++] =  new RoutePayment(new ClosestNeighbor(hop),trials, up);
		m[index++] =  new RoutePayment(new SplitIfNecessary(hop), trials, up);	
		m[index++] =  new RoutePayment(new SplitClosest(hop),trials, up); 
		//Interdimensional SpeedyMurmurs with varying number of trees, routing for three splitting protocols 
		for (int i = 0; i < trees.length; i++){
			m[index++] =  new RoutePayment(new ClosestNeighbor(speedyMulti[i]),trials, up);
			m[index++] =  new RoutePayment(new SplitIfNecessary(speedyMulti[i]), trials, up);	
			m[index++] =  new RoutePayment(new SplitClosest(speedyMulti[i]),trials, up);
		}	
		//compute stats about transaction set 
		m[index++] = new TransactionStats();
		Series.generate(net, m, runs);
	}	
	
	/**
	 * evaluate different timelocks (= maximal path lengths), Figure  7 in paper
	 */
	public static void locksEval() {
		//storage settings 
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/locks-nopadding/");
		//network settings 
		int init = 200; 
		int trval = 100;
		TransDist td = TransDist.EXP;
		BalDist bd = BalDist.EXP;
		int trs = 10000;
		//routing settings 
		int[] trees = {1,2,3,4,5,6,7,8,9,10}; 
		//execute 
		int runs = 20;
		lightningLocks(init, trval, runs, trs, trees, td, bd,"lightning/lngraph_2020_03_01__04_00.graph"); 
	}
	
	/**
	 * evaluate locks for one network 
	 * @param initCap: average capacity
	 * @param trval: average transaction value
	 * @param runs: number of runs
	 * @param trs: number of transactions
	 * @param trees: number of trees for Interdimensional SpeedyMurmurs (can be multiple) 
	 * @param td: type of transaction distribution
	 * @param bd: type of capacity distribution 
	 * @param file: topology file for graph
	 */
	public static void lightningLocks(int initCap, int trval, int runs, 
			int trs, int[] trees, TransDist td, BalDist bd, String file) {
		//generate network 
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		//generate distance functions, one for each tree number and timelock 
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[5*trees.length];
		int indx = 0;
		for (int i = 0; i < trees.length; i++) {
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i]);
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i], DistanceFunction.Timelock.MIN);
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i], DistanceFunction.Timelock.MAX);
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i], DistanceFunction.Timelock.CONST, 12);
			speedyMulti[indx++] = new SpeedyMurmursMulti(trees[i], DistanceFunction.Timelock.CONST, 24);
		}
		//instantiate routing algorithms for three splitting methods and each distance function 
		int trials = 1;
		boolean up = false; 
		Metric[] m = new Metric[3*speedyMulti.length+1];  
		int index = 0;
		for (int i = 0; i < speedyMulti.length; i++){
			m[index++] =  new RoutePayment(new ClosestNeighbor(speedyMulti[i]),trials, up);
			m[index++] =  new RoutePayment(new SplitIfNecessary(speedyMulti[i]), trials, up);	
			m[index++] =  new RoutePayment(new SplitClosest(speedyMulti[i]),trials, up);
		}
		//stats about transactions 
		m[index++] = new TransactionStats(); 
		//run 
		Series.generate(net, m, runs); 		
	}
	
	/**
	 * evaluate routing on random and scalefree graphs, Figure 6b 
	 */
	public static void topologyEval() {
		//storage settings 
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/topology/");
		//network parameters 
		int init = 200; 
		int trval = 100;
		int nodes = 6329; //nodecount chosen to match Lightning graph 
		double deg = 10.31; // degree chosen to match lightning graph 
		int trs = 10000; 
		TransDist td = TransDist.EXP;
		BalDist bd = BalDist.EXP;
		//routing parameters 
		int[] trees = {1,2,3,4,5,6,7,8,9,10};
		//execute 
		int runs = 20;
		runTopologyNoRand(init, trval, runs, trs, trees, td, bd,true,nodes,deg); //run barabasi albert (=scalefree)
		runTopologyNoRand(init, trval, runs, trs, trees, td, bd,false,nodes,deg); // run erdos-renyi (=random)
	}
	
	/**
	 * execute experiments on topology for one network 
	 * @param initCap: average capacity
	 * @param trval: average transaction value
	 * @param runs: number of runs
	 * @param trs: number of transactions
	 * @param trees: number of trees for Interdimensional SpeedyMurmurs (can be multiple) 
	 * @param td: type of transaction distribution
	 * @param bd: type of capacity distribution 
	 * @param ba: true= barabasi albert (BA) graph, false = erdos renyi graph 
	 * @param nodes: nodecount
	 * @param deg: average degree of nodes 
	 */
	public static void runTopologyNoRand(int initCap, int trval, int runs,  
			int trs, int[] trees, TransDist td, BalDist bd, boolean ba, int nodes, double deg) {
		//generate network 
		Transformation[] trans = new Transformation[] {new LargestWeaklyConnectedComponent(),
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net; 
		if (ba) {
			//BA: needs an integer as each joining nodes adds certain number of links  
			int connect = (int) Math.round(deg/2);
		    net = new BarabasiAlbert(nodes,connect, trans);
		} else {
			net = new ErdosRenyi(nodes, deg, true,trans);
		}
		//generate distance functions 
		DistanceFunction hop = new HopDistance();
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		//instantiate routing 
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
		//computer network and transaction statistics 
		m[index++] = new DegreeDistribution(); 
		m[index++] = new TransactionStats(); 
		Series.generate(net, m, runs); 		
	}
	
	/**
	 * run routing with varying transaction values/distributions and number of trees, Figure 3+4+6a
	 */
	public static void evalValTrees() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/lighting-nopadding/");
        //network parameters
        int[] vals = {1, 5, 20, 50, 100, 200, 300}; //various transaction values considered 
		int trs = 10000; 
        String file  = "lightning/lngraph_2020_03_01__04_00.graph";
        //routing parameters 
        int[] trees = {1,2,3,4,5,6,7,8,9,10}; 
        //run 
		for (int i = 0; i < vals.length; i++) {
			     //random split only for values of 1, 50, and 200 and exponential distributions 
                 if (vals[i] == 1 || vals[i] == 50 || vals[i] == 200) {
                	 runLightningIntDim(200, vals[i], 20, 
              				trs, trees, TransDist.EXP, BalDist.EXP, file); 
                 } else {
                	 runLightningIntDimNoRand(200, vals[i], 20, 
               				trs, trees, TransDist.EXP, BalDist.EXP, file);  
                 }
			     if (vals[i] == 100) {
            	 runLightningIntDimNoRand(200, vals[i], 20, 
         				trs, trees, TransDist.EXP, BalDist.NORMAL, file); //transactions exponential, capacities normal 
            	 runLightningIntDimNoRand(200, vals[i], 20, 
          				trs, trees, TransDist.NORMAL, BalDist.EXP, file); //transactions normal, capacities exponential
            	 runLightningIntDimNoRand(200, vals[i], 20, 
           				trs, trees, TransDist.NORMAL, BalDist.NORMAL, file); //transactions normal, capacities normal
			     }
             
		}
	}
	
	/**
	 * execute routing for one specific network, no concurrecny/dynamics/locktimes, Lightning snapshot 
	 * three splitting methods: No Split, Split by Dist, Split If Necessary 
	 * @param initCap: average capacity
	 * @param trval: average transaction value
	 * @param runs: number of runs
	 * @param trs: number of transactions
	 * @param trees: number of trees for Interdimensional SpeedyMurmurs (can be multiple) 
	 * @param td: type of transaction distribution
	 * @param bd: type of capacity distribution 
	 * @param file: lightning snapshot topology file 
	 */
	public static void runLightningIntDimNoRand(int initCap, int trval, int runs, 
			int trs, int[] trees, TransDist td, BalDist bd, String file) {
		//generate network 
		Transformation[] trans = new Transformation[] {
				new InitCapacities(initCap,0.05*initCap, bd), 
				new Transactions(trval, 0.1*trval, td, false, trs, false, true)};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		//generate distance functions 
		DistanceFunction hop = new HopDistance();
		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
		for (int i = 0; i < speedyMulti.length; i++) {
			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
		}
		//instantiate routing 
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
		//transaction stats 
		m[index++] = new TransactionStats();
		//run 
		Series.generate(net, m, runs); 		
	}
	
	/**
	 * execute routing for one specific network, no concurrecny/dynamics/locktimes, Lightning snapshot 
	 * four splitting methods: No Split, Split by Dist, Split If Necessary, random split (only for first 3 runs due to high overhead) 
	 * @param initCap: average capacity
	 * @param trval: average transaction value
	 * @param runs: number of runs
	 * @param trs: number of transactions
	 * @param trees: number of trees for Interdimensional SpeedyMurmurs (can be multiple) 
	 * @param td: type of transaction distribution
	 * @param bd: type of capacity distribution 
	 * @param file: lightning snapshot topology file 
	 */
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
	
	/**
	 * execute routing for one specific network, no concurrecny/dynamics/locktimes, Lightning snapshot 
	 * only random splitting  
	 * @param initCap: average capacity
	 * @param trval: average transaction value
	 * @param runs: number of runs
	 * @param trs: number of transactions
	 * @param trees: number of trees for Interdimensional SpeedyMurmurs (can be multiple) 
	 * @param td: type of transaction distribution
	 * @param bd: type of capacity distribution 
	 * @param file: lightning snapshot topology file 
	 */
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
	
	
	/**
	 * get degree distribution and distribution of shortest path lengths for a lightning graph 
	 * @param name: name of graph
	 * @param file: topology file 
	 */
	public static void getLightningStats(String name, String file) {
		ReadableFile net = new ReadableFile(name, name, file, null);
		Metric[] m = {new DegreeDistribution(), new ShortestPaths()};
		Series.generate(net, m,1);
	}

}
