package paymentrouting.route;

import java.util.Random;

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

public class PaymentTests {

	public static void main(String[] args) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);//run even if results already exist 
		//runSimpleTestSynthetic(); 
	    System.out.println(1524000/(1000*60)); 
	}
	
	/**
	 * test for simple no-concurrency no-dynamic PaymentRoute with existing data set 
	 */
	public static void runSimpleTest() {
		//read test files with 5 nodes, 3 transactions 
		Network net = new ReadableFile("DS", "DS", "data/simple/simple2_graph.txt", null);
		//generate distance functions
		DistanceFunction hop = new HopDistance();
		DistanceFunction speedyMulti = new SpeedyMurmursMulti(2); //Interdimensional SpeedyMurmurs with two trees 
		int trials = 1; // only one attempt
		boolean up = false; //no dymanic updates of balances  
        Metric[] m = new Metric[] {new RoutePayment(new ClosestNeighbor(hop),trials,up), //no splitting, HopDistance 
				                   new RoutePayment(new ClosestNeighbor(speedyMulti),trials,up), //no splitting, Interdimensional SpeedyMurmurs 
				                   new RoutePayment(new SplitIfNecessary(hop),trials,up), //split if necessary, HopDistance 
				                   new RoutePayment(new SplitIfNecessary(speedyMulti),trials,up), //split if necessary, Interdimensional SpeedyMurmurs
				                   new RoutePayment(new SplitClosest(hop),trials,up), //split by dist, HopDistance 
				                   new RoutePayment(new SplitClosest(speedyMulti),trials,up), //split by dist, Interdimensional SpeedyMurmurs
				                   new RoutePayment(new RandomSplit(hop),trials,up), //random splitting, HopDistance 
				                   new RoutePayment(new RandomSplit(speedyMulti),trials,up) //random splitting, Interdimensional SpeedyMurmurs
                                   }; 
		Series.generate(net, m, 3); 
	}
	
	public static void runSimpleTestSynthetic() {
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
        //generate transformations to add i) capacities and ii) transactions 
		Transformation[] trans = new Transformation[] { new InitCapacities(200, -1, BalDist.EXP), 
				//exponentially distributed capacities with average value 200 (middle value is variance, which is not relevant for exponential)
				new Transactions(20, -1, TransDist.EXP, false, 15, true, false) 
				// 15 transactions with expontially distributed values with average 20 (again -1 is variance, not needed for exp), 
				//no cutoff, no concrete timestamp, no restriction to transactions guaranteed to be successful 
		        };
		Network net = new BarabasiAlbert(30, 3, trans);//scale-free barabasi-albert graph with 30 nodes, each new node forming three links to existing nodes
		// generate distance functions
		DistanceFunction hop = new HopDistance();
		DistanceFunction speedyMulti = new SpeedyMurmursMulti(2); // Interdimensional SpeedyMurmurs with two trees
		int trials = 1; // only one attempt
		boolean up = false; // no dymanic updates of balances
		Metric[] m = new Metric[] { new RoutePayment(new ClosestNeighbor(hop), trials, up), // no splitting, HopDistance
				new RoutePayment(new ClosestNeighbor(speedyMulti), trials, up), // no splitting, Interdimensional
																				// SpeedyMurmurs
				new RoutePayment(new SplitIfNecessary(hop), trials, up), // split if necessary, HopDistance
				new RoutePayment(new SplitIfNecessary(speedyMulti), trials, up), // split if necessary, Interdimensional
																					// SpeedyMurmurs
				new RoutePayment(new SplitClosest(hop), trials, up), // split by dist, HopDistance
				new RoutePayment(new SplitClosest(speedyMulti), trials, up), // split by dist, Interdimensional
																				// SpeedyMurmurs
				new RoutePayment(new RandomSplit(hop), trials, up), // random splitting, HopDistance
				new RoutePayment(new RandomSplit(speedyMulti), trials, up) // random splitting, Interdimensional
																			// SpeedyMurmurs
		};
		//run 
		Series.generate(net, m, 10);
   }

	
	
	
	
//	public static void genDataSets() {
//		Transformation[] trans = new Transformation[] {new InitCapacities(200,BalDist.EXP), 
//				new Transactions(5, TransDist.EXP, false, 10, false)};
//		Network net = new BarabasiAlbert(5000,5, trans);
//		Series.generate(net, new Metric[] {}, 1); 
//	}
//	
//	public static void runShortTestBA() {
//		Transformation[] trans = new Transformation[] {new InitCapacities(200,BalDist.EXP), 
//				new Transactions(200, TransDist.NORMAL, false, 1000, false, true)};
//		Network net = new BarabasiAlbert(5000,5, trans);
//		DistanceFunction hop = new HopDistance();
//		DistanceFunction speedy1 = new SpeedyMurmurs(1);
//		DistanceFunction speedy2 = new SpeedyMurmurs(2);
//		DistanceFunction speedy3 = new SpeedyMurmurs(3);
//		DistanceFunction speedy4 = new SpeedyMurmurs(4);
//		DistanceFunction speedyM1 = new SpeedyMurmursMulti(1);
//		DistanceFunction speedyM2 = new SpeedyMurmursMulti(2);
//		DistanceFunction speedyM3 = new SpeedyMurmursMulti(3);
//		DistanceFunction speedyM4 = new SpeedyMurmursMulti(4);
//		int trials = 1;
//		boolean up = false; 
//		Metric[] m = new Metric[] {new RoutePayment(new ClosestNeighbor(hop),trials,up),
//				new RoutePayment(new ClosestNeighbor(speedy1),trials,up),
//				new RoutePayment(new ClosestNeighbor(speedy2),trials,up),
//				new RoutePayment(new ClosestNeighbor(speedy3),trials,up),
//				new RoutePayment(new ClosestNeighbor(speedy4),trials,up),
//				new RoutePayment(new ClosestNeighbor(speedyM1),trials,up),
//				new RoutePayment(new ClosestNeighbor(speedyM2),trials,up),
//				new RoutePayment(new ClosestNeighbor(speedyM3),trials,up),
//				new RoutePayment(new ClosestNeighbor(speedyM4),trials,up)
//				                   }; 
//		Series.generate(net, m, 10); 		
//	}
//	
//
//	
//	public static void runBA(int initCap, int trval, int nodes, int runs, int trs, int connect, int trees) {
//		Transformation[] trans = new Transformation[] {new InitCapacities(initCap,BalDist.EXP), 
//				new Transactions(trval, TransDist.EXP, false, trs, false)};
//		Network net = new BarabasiAlbert(nodes,connect, trans);
//		DistanceFunction hop = new HopDistance();
//		DistanceFunction speedy = new SpeedyMurmurs(trees);
//		DistanceFunction speedyMulti = new SpeedyMurmursMulti(trees);
//		int trials = 1;
//		boolean up = false; 
//		Metric[] m = new Metric[] {new RoutePayment(new ClosestNeighbor(hop),trials,up),
//                new RoutePayment(new ClosestNeighbor(speedy),trials,up),
//                new RoutePayment(new ClosestNeighbor(speedyMulti),trials,up),
//                new RoutePayment(new SplitIfNecessary(hop),trials,up),
//                new RoutePayment(new SplitIfNecessary(speedy),trials,up),
//                new RoutePayment(new SplitIfNecessary(speedyMulti),trials,up),
//                new RoutePayment(new RandomSplit(hop),trials,up),
//                new RoutePayment(new RandomSplit(speedy),trials,up),
//                new RoutePayment(new RandomSplit(speedyMulti),trials,up)
//                };  
//		Series.generate(net, m, runs); 		
//	}
//	
//
//	
//	public static void runBA(int initCap, int trval, int nodes, int runs, 
//			int trs, int connect, int[] trees, TransDist td, BalDist bd) {
//		Transformation[] trans = new Transformation[] {
//				new InitCapacities(initCap,0.05*initCap, bd), 
//				new Transactions(trval, 0.1*trval, td, false, trs, false)};
//		Network net = new BarabasiAlbert(nodes,connect, trans);
//		DistanceFunction hop = new HopDistance();
//		DistanceFunction[] speedy = new SpeedyMurmurs[trees.length];				
//		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
//		for (int i = 0; i < speedy.length; i++) {
//			speedy[i] = new SpeedyMurmurs(trees[i]);
//			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
//		}
//		int trials = 1;
//		boolean up = true; 
//		Metric[] m = new Metric[2+4*trees.length]; 
//		m[0] = 	new RoutePayment(new ClosestNeighbor(hop),trials,up);
//		m[1] =  new RoutePayment(new SplitIfNecessary(hop),trials,up);
//		//m[2] =  new RoutePayment(new RandomSplit(hop),trials,up);
//		//m[3] =  new RoutePayment(new RandomProportionalSplit(hop),trials,up);
//		int index = 2;
//		for (int i = 0; i < trees.length; i++){
//			m[index++] =  new RoutePayment(new ClosestNeighbor(speedy[i]),trials,up);
//			m[index++] =  new RoutePayment(new SplitIfNecessary(speedy[i]),trials,up);
//			//m[index++] =  new RoutePayment(new RandomSplit(speedy[i]),trials,up);
//			//m[index++] =  new RoutePayment(new RandomProportionalSplit(speedy[i]),trials,up); 
//			
//			m[index++] =  new RoutePayment(new ClosestNeighbor(speedyMulti[i]),trials,up);
//			m[index++] =  new RoutePayment(new SplitIfNecessary(speedyMulti[i]),trials,up);
//			//m[index++] =  new RoutePayment(new RandomSplit(speedyMulti[i]),trials,up);
//			//m[index++] =  new RoutePayment(new RandomProportionalSplit(speedyMulti[i]),trials,up); 
//			
//		}			 
//		Series.generate(net, m, runs); 		
//	}
//	
//
//	
//	
//	
//	public static void runER(int initCap, int trval, int nodes, int runs, int trs, int trees, double avDeg) {
//		Transformation[] trans = new Transformation[] {new LargestWeaklyConnectedComponent(),
//				new InitCapacities(initCap,BalDist.EXP), 
//				new Transactions(trval, TransDist.EXP, false, trs, false)};
//		Network net = new ErdosRenyi(nodes, avDeg, true,trans);
//		DistanceFunction hop = new HopDistance();
//		DistanceFunction speedy = new SpeedyMurmurs(trees);
//		DistanceFunction speedyMulti = new SpeedyMurmursMulti(trees);
//		int trials = 1;
//		boolean up = false; 
//		Metric[] m = new Metric[] {new RoutePayment(new ClosestNeighbor(hop),trials,up),
//                new RoutePayment(new ClosestNeighbor(speedy),trials,up),
//                new RoutePayment(new ClosestNeighbor(speedyMulti),trials,up),
//                new RoutePayment(new SplitIfNecessary(hop),trials,up),
//                new RoutePayment(new SplitIfNecessary(speedy),trials,up),
//                new RoutePayment(new SplitIfNecessary(speedyMulti),trials,up),
//                new RoutePayment(new RandomSplit(hop),trials,up),
//                new RoutePayment(new RandomSplit(speedy),trials,up),
//                new RoutePayment(new RandomSplit(speedyMulti),trials,up)
//                }; 
//		Series.generate(net, m, runs); 		
//	}
//	
//	public static void runER(int initCap, int trval, int nodes, int runs, int trs, 
//			int[] trees, double avDeg, TransDist td, BalDist bd) {
//		Transformation[] trans = new Transformation[] {
//				new LargestWeaklyConnectedComponent(),
//				new InitCapacities(initCap,0.05*initCap, bd), 
//				new Transactions(trval, 0.1*trval, td, false, trs, false)};
//		Network net = new ErdosRenyi(nodes, avDeg, true,trans);
//		DistanceFunction hop = new HopDistance();
//		DistanceFunction[] speedy = new SpeedyMurmurs[trees.length];				
//		DistanceFunction[] speedyMulti = new SpeedyMurmursMulti[trees.length];
//		for (int i = 0; i < speedy.length; i++) {
//			speedy[i] = new SpeedyMurmurs(trees[i]);
//			speedyMulti[i] = new SpeedyMurmursMulti(trees[i]);
//		}
//		int trials = 1;
//		boolean up = false; 
//		Metric[] m = new Metric[3+6*trees.length]; 
//		m[0] = 	new RoutePayment(new ClosestNeighbor(hop),trials,up);
//		m[1] =  new RoutePayment(new SplitIfNecessary(hop),trials,up);
//		m[2] =  new RoutePayment(new RandomSplit(hop),trials,up);
//		int index = 3;
//		for (int i = 0; i < trees.length; i++){
//			m[index++] =  new RoutePayment(new ClosestNeighbor(speedy[i]),trials,up);
//			m[index++] =  new RoutePayment(new SplitIfNecessary(speedy[i]),trials,up);
//			m[index++] =  new RoutePayment(new RandomSplit(speedy[i]),trials,up);
//			
//			m[index++] =  new RoutePayment(new ClosestNeighbor(speedyMulti[i]),trials,up);
//			m[index++] =  new RoutePayment(new SplitIfNecessary(speedyMulti[i]),trials,up);
//			m[index++] =  new RoutePayment(new RandomSplit(speedyMulti[i]),trials,up);
//		}		 
//		Series.generate(net, m, runs); 		
//	}
//	
//
//	
//	public static void runSimpleTestAll() {
//		Network net = new ReadableFile("DS", "DS", "data/simple/simple2_graph.txt", null);
//		DistanceFunction hop = new HopDistance();
//		DistanceFunction speedy = new SpeedyMurmurs(2);
//		DistanceFunction speedyMulti = new SpeedyMurmursMulti(2);
//		int trials = 1;
//		boolean up = false; 
//        Metric[] m = new Metric[] {new RoutePayment(new ClosestNeighbor(hop),trials,up),
//				                   new RoutePayment(new ClosestNeighbor(speedy),trials,up),
//				                   new RoutePayment(new ClosestNeighbor(speedyMulti),trials,up),
//				                   new RoutePayment(new SplitIfNecessary(hop),trials,up),
//				                   new RoutePayment(new SplitIfNecessary(speedy),trials,up),
//				                   new RoutePayment(new SplitIfNecessary(speedyMulti),trials,up),
//				                   new RoutePayment(new SplitClosest(hop),trials,up),
//				                   new RoutePayment(new SplitClosest(speedy),trials,up),
//				                   new RoutePayment(new SplitClosest(speedyMulti),trials,up),
//				                   new RoutePayment(new RandomSplit(hop),trials,up),
//				                   new RoutePayment(new RandomSplit(speedy),trials,up),
//				                   new RoutePayment(new RandomSplit(speedyMulti),trials,up),
//				                   new RoutePayment(new SplitIfTooHigh(hop,0.7,0.05),trials,up),
//				                   new RoutePayment(new SplitIfTooHigh(speedy,0.7,0.05),trials,up), 
//				                   new RoutePayment(new SplitIfTooHigh(speedyMulti,0.7,0.05),trials,up)
//                                   }; 
//		Series.generate(net, m, 1); 
//	}
//	
//
//	public static void runSimpleAttack() {
//		Network net = new ReadableFile("DS", "DS", "data/simple/simple2_graph.txt", null);
//		DistanceFunction speedyMulti = new SpeedyMurmursMulti(2);
//		int trials = 1;
//		boolean up = false; 
//        Metric[] m = new Metric[] {
//				                   new RoutePayment(new NonColludingDropSplits(
//				                		   new SplitClosest(speedyMulti), 0.8, 7),trials,up),
//				                   new RoutePayment(new NonColludingDropSplits(
//				                		   new SplitIfNecessary(speedyMulti), 0.8, 7),trials,up),
//				                   new RoutePayment(new ColludingDropSplits(
//				                		   new SplitClosest(speedyMulti), 0.5, 7),trials,up),
//				                   new RoutePayment(new ColludingDropSplits(
//				                		   new SplitIfNecessary(speedyMulti), 0.5, 7),trials,up),
//                                   }; 
//		Series.generate(net, m, 1); 
//	}
//	
//	
//	
//	public static void runSimpleFees() {
//		Network net = new ReadableFile("DS-Att", "DS-Att", "data/simple/simple2_graph.txt", null);
//		DistanceFunction speedy = new SpeedyMurmurs(3);
//		FeeComputation lightning = new LightningFees(1,1,false); 
//		FeeComputation adf = new AbsoluteDiffFee(1,1, false);
//		FeeComputation rdf = new RatioDiffFee(0.05,1, false);
//		FeeComputation lightningZero = new LightningFees(1,1,true); 
//		FeeComputation adfZero = new AbsoluteDiffFee(1,1, true);
//		FeeComputation rdfZero = new RatioDiffFee(0.05,1, true);
//		int trials = 1;
//		boolean up = true; 
//		int con = 3;
//		int need = 1; 
//		Metric[] m = new Metric[] {new RoutePaymentFees
//				                   (new ClosestNeighbor(speedy),trials,up, lightning,con,need,false),
//				                   new RoutePaymentFees
//				                   (new ClosestNeighbor(speedy),trials,up, adf,con,need,false),
//				                   new RoutePaymentFees
//				                   (new ClosestNeighbor(speedy),trials,up, rdf,con,need,false),
//				                   new RoutePaymentFees
//				                   (new ClosestNeighbor(speedy),trials,up, lightningZero,con,need,false),
//				                   new RoutePaymentFees
//				                   (new ClosestNeighbor(speedy),trials,up, adfZero,con,need,false),
//				                   new RoutePaymentFees
//				                   (new ClosestNeighbor(speedy),trials,up, rdfZero,con,need,false)
//				                   }; 
//		Series.generate(net, m, 1); 
//	}
//
//	public static void testOnlyPossible() {
//		Transformation[] trans = new Transformation[] {
//				new Transactions(10, TransDist.EXP, false, 10, false, true)};
//		Network net = new ReadableFile("DS", "DS", "data/simple/simpleNoTrans_graph.txt", trans);
//		DistanceFunction hop = new HopDistance();
//		Metric[] m = new Metric[] {new RoutePayment(new ClosestNeighbor(hop),1, false)};
//		Series.generate(net, m, 1); 
//	}
	
	

	
	


}
