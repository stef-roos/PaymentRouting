package paymentrouting.route.bailout;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import gtna.data.Series;
import gtna.graph.Graph;
import gtna.io.graphWriter.GtnaGraphWriter;
import gtna.metrics.Metric;
import gtna.metrics.basic.DegreeDistribution;
import gtna.metrics.basic.ShortestPaths;
import gtna.networks.Network;
import gtna.networks.model.BarabasiAlbert;
import gtna.networks.util.ReadableFile;
import gtna.transformation.Transformation;
import gtna.util.Config;
import paymentrouting.datasets.InitCapacities;
import paymentrouting.datasets.InitCapacities.BalDist;
import paymentrouting.datasets.InitLNParams;
import paymentrouting.datasets.Transactions;
import paymentrouting.datasets.Transactions.TransDist;
import paymentrouting.route.HopDistance;
import paymentrouting.route.SummarizeResults;
import paymentrouting.route.attack.GriefingAttack;
import paymentrouting.route.bailout.RoutePaymentBailout.AcceptFee;
import paymentrouting.route.bailout.RoutePaymentBailout.BailoutFee;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;
import paymentrouting.sourcerouting.LND;
import paymentrouting.util.LightningJsonReader;

public class BailOutExperiments {
	
	
	/**
	 * run experiments for our paper "Get Me out of This Payment!Bailout: An HTLC Re-routing Protocol"
	 * NOTE: need a lot of memory and days to run, were run on clustering system
	 * for local tests: there are smaller experiments at the end of this file (e.g., 500 node BA) 
	 * @param args
	 */
	public static void main(String[] args) {
		int i = Integer.parseInt(args[0]); //run; varied between 0 and 9 
		double p = Double.parseDouble(args[1]); // probability to delay/not settle/grief
		//lightningDelay(i,p); //experiments with nodes delaying 
		//lightningNoPeacefulSettlement(i,p); //experiments with nodes not settling 
		//lightningGriefing(i); //griefing 
		//lightningMulti(i);  //experiments with more than 1 bailout node 
 
	}
	
	/**
	 * turn lightning json into gtna 
	 * @param file
	 * @param output
	 */
	public static void rewriteGraphWithCap(String file, String output) {
    	LightningJsonReader read = new LightningJsonReader();
    	Graph g = read.read(file);
    	GtnaGraphWriter write = new GtnaGraphWriter();
    	write.writeWithProperties(g, output); 
    }
	
	/**
	 * get stats over potential bailouts available 
	 * @param file
	 * @param output
	 */
	public static void stats(String graph, String name) {
		//read graph file
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/stats/");
		Network net = new ReadableFile("Bailout", "Bailout", graph, null);
		Metric[] m = new Metric[] {new BailoutPotStats("./data/bailout/stats/Stats"+name)};
		Series.generate(net, m, 1); 
	}
	
	
	
	
	/**
	 * summarize results for p=0.1
	 */
	public static void summarizeJan3() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new Transactions(400000, TransDist.EXP, false, 100000, 10, false), new InitLNParams()};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(0.1), new PaymentReactionNoPeaceful(0.1),
				new PaymentReactionGriefingRandom(0.1, new GriefingAttack(100000,10000))};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.NORMAL, BailoutFee.FACTOR, BailoutFee.TOTAL, BailoutFee.TOTALEDGE,
				BailoutFee.BUFFER};
		double[] facs = {1,10,1.2,1,1}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS, AcceptFee.THRESHOLD, 
				AcceptFee.TOTAL, AcceptFee.TOTALEDGE, AcceptFee.NEXT};
		double[] thres = {Double.MAX_VALUE, 100, 1,1,1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
		   m[i] =  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait); 
		} 
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					try {
					m[reacts.length+k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		Series.generate(net, m, 10); 
	
	}
	
	/**
	 * summarize results for varying probability
	 */
	public static void summarizeProb() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		double[] ps = {0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
		for (int p = 0; p < ps.length; p++) {
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailoutProb/"+ps[p]+"/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new Transactions(400000, TransDist.EXP, false, 100000, 10, false), new InitLNParams()};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(ps[p]), new PaymentReactionNoPeaceful(ps[p])};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.BUFFER};
		double[] facs = {1}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.THRESHOLD, AcceptFee.NEXT};
		double[] thres = {100.0,1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
		   m[i] =  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait); 
		} 
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					try {
					m[reacts.length+k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} 
		Series.generate(net, m, 10); 
		}
	}
	

	/**
	 * run all fee strategies when delaying is applied 
	 * @param run run #
	 * @param probability prob to delay
	 */
	public static void lightningDelay(int run, double probability) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new Transactions(400000, TransDist.EXP, false, 100000, 10, false), new InitLNParams()};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(probability)};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.NORMAL, BailoutFee.FACTOR, BailoutFee.TOTAL, BailoutFee.TOTALEDGE,
				BailoutFee.BUFFER};
		double[] facs = {1,10,1.2,1,1}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS, AcceptFee.THRESHOLD, 
				AcceptFee.TOTAL, AcceptFee.TOTALEDGE, AcceptFee.NEXT};
		double[] thres = {Double.MAX_VALUE, 100, 1,1,1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
		   m[i] =  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait); 
		} 
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					try {
					m[reacts.length+k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		Series.generate(net, m, run, run); 
	
	}
	
	/**
	 * run all fee combinations for EXPECTED (did use too much memry 64GB+)
	 * @param run
	 */
	public static void lightningExpected(int run) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new Transactions(400000, TransDist.EXP, false, 100000, 10, false), new InitLNParams()};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(0.1), new PaymentReactionNoPeaceful(0.1),
				new PaymentReactionGriefingRandom(0.1, new GriefingAttack(100000,10000))};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] {BailoutFee.EXPECTED, BailoutFee.EXPECTED, BailoutFee.NORMAL, BailoutFee.FACTOR}; //, };
		double[] facs = {1.2, 1.2, 1.0, 10}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS, AcceptFee.THRESHOLD, AcceptFee.EXPECTED, AcceptFee.EXPECTED};
		double[] thres = {Double.MAX_VALUE, 100, 1, 1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length];
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
					try {
					m[j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/records-Exp-"+i+"-"+j+"-"+run+".txt",reacts[i],
									bfees[j], facs[j], afees[j], thres[j], wait); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				
			}
		}
		Series.generate(net, m, run, run); 
	
	}
	
	/**
	 * print table comparison delay, no settle, griefing for p=0.1 for all metrics  
	 */
	public static void printTable() {
		String[] metrics = new String[] {"ROUTE_PAYMENT_SUCCESS=", "ROUTE_PAYMENT_BAILOUTS", "ROUTE_PAYMENT_BAILOUT_NOT_ACCEPTED", 
				"ROUTE_PAYMENT_BAILOUT_NOT_FOUND", "ROUTE_PAYMENT_BAILOUT_ATTEMPTS", "ROUTE_PAYMENT_FEE_AV", 
				"ROUTE_PAYMENT_FEE_MED", "ROUTE_PAYMENT_FEE_Q1", "ROUTE_PAYMENT_FEE_Q3", "ROUTE_PAYMENT_FEE_MIN", 
				"ROUTE_PAYMENT_FEE_MAX", "ROUTE_PAYMENT_FEE_BAIL_AV", "ROUTE_PAYMENT_FEE_BAIL_MED", "ROUTE_PAYMENT_FEE_BAIL_Q1",
				"ROUTE_PAYMENT_FEE_BAIL_Q3", "ROUTE_PAYMENT_FEE_BAIL_MIN", "ROUTE_PAYMENT_FEE_BAIL_MAX", "ROUTE_PAYMENT_GINI"};
		for (int i = 0; i < metrics.length; i++) {
			System.out.println(metrics[i]);
			String[] att = new String[]{"DELAY_RANDOM", "NO_PEACEFUL_SETTLEMENT", "GRIEF_RANDOM"};
			String[] fee = new String[] {"NEVER_0.0", "NORMAL_1.0", "FACTOR_10.0", "TOTAL_1.2", "TOTALEDGE_1.0", "BUFFER_1.0"};
			String[] acc = new String[] {"ALWAYS", "THRESHOLD100.0", "TOTAL", "TOTALEDGE", "NEXT"};
			for (int j = 0; j < fee.length; j++) {
				for (int k = 0; k < acc.length; k++) {
					if (j == 0 && k>0) continue; 
					String line = fee[j]+"-"+acc[k];
					for (int l = 0; l < att.length; l++) {
						double[] res = SummarizeResults.getSingleVar("data/bailout/READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-"
								+ "4000000.0-EXP--TRANSACTIONS-400000.0-EXP-false-100000-10.0-false--INIT_LN_PARAMS/"
								+ "ROUTE_PAYMENT-1-true-HOP_DISTANCE-LND-"
								+ "2147483647-"+att[l]+"-"+fee[j]+"-"+acc[k]+"-60.0-0.1"
								+ "/_singles.txt",
								metrics[i]); 
						line = line + " & " + "$" + 
							      String.format("%.2f", res[0]) + " \\pm " + String.format("%.3f", res[1]) + "$"; 
					}
					System.out.println(line); 
				}
			}
			System.out.println();
		}
		
				
		}
	
	/**
	 * print files used to generate figure 4 (one per subfigure) 
	 */
	public static void printBailouts() {
		String[] metrics = new String[] {"ROUTE_PAYMENT_BAILOUTS"};
		for (int i = 0; i < metrics.length; i++) {
			String[] att = new String[]{"DELAY_RANDOM", "NO_PEACEFUL_SETTLEMENT", "GRIEF_RANDOM"};
			String[] fee = new String[] {"NORMAL_1.0", "FACTOR_10.0", "TOTAL_1.2", "TOTALEDGE_1.0", "BUFFER_1.0"};
			String[] feeName = new String[] {"NORM", "MULT", "TOTAL", "TOTAL-CH", "BUFFER"}; 
			String[] acc = new String[] {"ALWAYS", "THRESHOLD100.0", "TOTAL", "TOTALEDGE", "NEXT"};
			String[] accName = new String[] {"ALWAYS", "THRESHOLD", "TOTAL", "TOTAL-CH", "BUFFER"};
			for (int j = 0; j < att.length; j++) {
				try {
					BufferedWriter bw = new BufferedWriter(new FileWriter("data/bailout/"+att[j]+".dat"));
					String line = "ACC";
					for (int l = 0; l < fee.length; l++) {
						line = line + " " + feeName[l];
					}
					bw.write(line);
				for (int k = 0; k < acc.length; k++) {
					line = accName[k];
					for (int l = 0; l < fee.length; l++) {
						double[] res = SummarizeResults.getSingleVar("data/bailout/READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-"
								+ "4000000.0-EXP--TRANSACTIONS-400000.0-EXP-false-100000-10.0-false--INIT_LN_PARAMS/"
								+ "ROUTE_PAYMENT-1-true-HOP_DISTANCE-LND-"
								+ "2147483647-"+att[j]+"-"+fee[l]+"-"+acc[k]+"-60.0-0.1"
								+ "/_singles.txt",
								metrics[i]); 
						line = line + " " +
							      String.format("%.2f", res[0]); 
					}
					bw.newLine();
					bw.write(line); 
				}
				bw.flush();
				bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * print files for attempted bailouts, same structure as fig 4 file  
	 */
	public static void printBailoutAtt() {
		String[] metrics = new String[] {"ROUTE_PAYMENT_BAILOUT_ATTEMPTS"};
		for (int i = 0; i < metrics.length; i++) {
			String[] att = new String[]{"DELAY_RANDOM", "NO_PEACEFUL_SETTLEMENT", "GRIEF_RANDOM"};
			String[] fee = new String[] {"NORMAL_1.0", "FACTOR_10.0", "TOTAL_1.2", "TOTALEDGE_1.0", "BUFFER_1.0"};
			String[] feeName = new String[] {"NORM", "MULT", "TOTAL", "TOTAL-CH", "BUFFER"}; 
			String[] acc = new String[] {"ALWAYS", "THRESHOLD100.0", "TOTAL", "TOTALEDGE", "NEXT"};
			String[] accName = new String[] {"ALWAYS", "THRESHOLD", "TOTAL", "TOTAL-CH", "BUFFER"};
			for (int j = 0; j < att.length; j++) {
				try {
					BufferedWriter bw = new BufferedWriter(new FileWriter("data/bailout/"+att[j]+"Attempt.dat"));
					String line = "ACC";
					for (int l = 0; l < fee.length; l++) {
						line = line + " " + feeName[l];
					}
					bw.write(line);
				for (int k = 0; k < acc.length; k++) {
					line = accName[k];
					for (int l = 0; l < fee.length; l++) {
						double[] res = SummarizeResults.getSingleVar("data/bailout/READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-"
								+ "4000000.0-EXP--TRANSACTIONS-400000.0-EXP-false-100000-10.0-false--INIT_LN_PARAMS/"
								+ "ROUTE_PAYMENT-1-true-HOP_DISTANCE-LND-"
								+ "2147483647-"+att[j]+"-"+fee[l]+"-"+acc[k]+"-60.0-0.1"
								+ "/_singles.txt",
								metrics[i]); 
						line = line + " " +
							      String.format("%.2f", res[0]); 
					}
					bw.newLine();
					bw.write(line); 
				}
				bw.flush();
				bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * print files to generate fig 5 
	 */
	public static void printProb() {
		double[] ps = {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
		String[] folders = new String[10];
		folders[0] = "./data/bailout/";
		for (int i = 1; i < ps.length; i++) {
			folders[i] = "./data/bailoutProb/"+ps[i]+"/";
		}
		String netFolder = "READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-4000000.0-EXP--TRANSACTIONS-400000.0-EXP-false-100000-10.0-false--INIT_LN_PARAMS/";
		String[] routingFolder = new String[] {
				"ROUTE_PAYMENT-1-true-HOP_DISTANCE-LND-2147483647-DELAY_RANDOM-BUFFER_1.0-THRESHOLD100.0-60.0-0.1/",
				"ROUTE_PAYMENT-1-true-HOP_DISTANCE-LND-2147483647-DELAY_RANDOM-BUFFER_1.0-NEXT-60.0-0.1/",
				"ROUTE_PAYMENT-1-true-HOP_DISTANCE-LND-2147483647-NO_PEACEFUL_SETTLEMENT-BUFFER_1.0-THRESHOLD100.0-60.0-0.1/",
				"ROUTE_PAYMENT-1-true-HOP_DISTANCE-LND-2147483647-NO_PEACEFUL_SETTLEMENT-BUFFER_1.0-NEXT-60.0-0.1/"};
		String[] metrics = new String[] {"ROUTE_PAYMENT_BAILOUTS", "ROUTE_PAYMENT_BAILOUT_NOT_ACCEPTED", 
				"ROUTE_PAYMENT_BAILOUT_NOT_FOUND", "ROUTE_PAYMENT_BAILOUT_ATTEMPTS"};
		
		for (int i = 0; i < metrics.length; i++) {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter("./data/bailoutProb/"+metrics[i]+".dat"));
				String line = "# DEL-THRES DEL-BUFFER NOSET-THRES NOSET-BUFFER";
				bw.write(line);
				for (int p = 0; p < ps.length; p++) {
					line = ps[p]+"";
					for (int j =0; j < routingFolder.length; j++) {
					double[] res = SummarizeResults.getSingleVar(folders[p] + netFolder + routingFolder[j]
							+ "_singles.txt",
							metrics[i]); 
					line = line + " " +
						      String.format("%.2f", res[0]) + " " + String.format("%.3f", res[1]);
					}
					bw.newLine();
					bw.write(line);
				}
				bw.flush();
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	
	/**
	 * run all fee strategies when no settle is applied 
	 * @param run run #
	 * @param probability prob to delay
	 */
	public static void lightningNoPeacefulSettlement(int run, double p) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new Transactions(400000, TransDist.EXP, false, 100000, 10, false), new InitLNParams()};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionNoPeaceful(p)};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.NORMAL, BailoutFee.FACTOR, BailoutFee.TOTAL, BailoutFee.TOTALEDGE,
		BailoutFee.BUFFER};
        double[] facs = {1,10,1.2,1,1}; 
        RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS, AcceptFee.THRESHOLD, 
		AcceptFee.TOTAL, AcceptFee.TOTALEDGE, AcceptFee.NEXT};
        double[] thres = {Double.MAX_VALUE, 100, 1,1,1}; 

		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
		   m[i] =  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait); 
		} 
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					try {
					m[reacts.length+k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		Series.generate(net, m, run, run); 
	}
	
	
	/**
	 * run experiments for fig 5  
	 * @param run run #
	 * @param probability prob to delay
	 */
	public static void lightningProb(int run, double probability) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailoutProb/"+probability+"/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new Transactions(400000, TransDist.EXP, false, 100000, 10, false), new InitLNParams()};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(probability), new PaymentReactionNoPeaceful(probability)};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.BUFFER};
		double[] facs = {1}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.THRESHOLD, AcceptFee.NEXT};
		double[] thres = {100.0,1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
		   m[i] =  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait); 
		} 
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					try {
					m[reacts.length+k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		Series.generate(net, m, run, run); 
	
	}
	
	/**
	 * run experiments for Table 1
	 * @param run
	 */
	public static void lightningMulti(int run) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailoutMulti/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new Transactions(400000, TransDist.EXP, false, 100000, 10, false), new InitLNParams()};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(0.1), new PaymentReactionNoPeaceful(0.1)};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.BUFFER};
		double[] facs = {1}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.THRESHOLD, AcceptFee.NEXT};
		double[] thres = {100.0,1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length];
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					try {
					m[k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait,true); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		Series.generate(net, m, run, run); 
	}
	
	/**
	 * summarize results for table 1 
	 */
	public static void summarizeMulti() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailoutMulti/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new Transactions(400000, TransDist.EXP, false, 100000, 10, false), new InitLNParams()};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(0.1), new PaymentReactionNoPeaceful(0.1)};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.BUFFER};
		double[] facs = {1}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.THRESHOLD, AcceptFee.NEXT};
		double[] thres = {100.0,1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length];
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					try {
					m[k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait,true); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		Series.generate(net, m, 10); 
	
	}
	
	/**
	 * run all fee strategies for griefing 
	 * @param run
	 */
	public static void lightningGriefing(int run) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new InitCapacities(4000000,BalDist.EXP), 
				new Transactions(400000, TransDist.EXP, false, 100000, 10, false), new InitLNParams()};
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionGriefingRandom(0.1, new GriefingAttack(100000,10000))};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.NORMAL, BailoutFee.FACTOR, BailoutFee.TOTAL, BailoutFee.TOTALEDGE,
				BailoutFee.BUFFER};
		double[] facs = {1,10,1.2,1,1}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS, AcceptFee.THRESHOLD, 
				AcceptFee.TOTAL, AcceptFee.TOTALEDGE, AcceptFee.NEXT};
		double[] thres = {Double.MAX_VALUE, 100, 1,1,1}; 
		double wait = 60;  
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
		   m[i] =  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait); 
		} 
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					try {
					m[reacts.length+k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1,reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		Series.generate(net, m, run, run); 
	
	}
	
	
	
	//testing algorithms on smaller graphs 
	
	public static void testLNDRouting() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		Network net = new ReadableFile("LND", "LND", "./data/simple/simpleCon_graph.txt", new Transformation[] {new InitLNParams()});
		RoutePaymentConcurrent pay = new RoutePaymentConcurrent(new LND(new HopDistance()),1,0.1);
		Metric[] m = new Metric[] {pay};
		Series.generate(net, m, 1); 
	}
	
	
	
	public static void testBA() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		Transformation[] trans = new Transformation[] {new InitCapacities(200,BalDist.EXP), 
				new Transactions(200, TransDist.NORMAL, false, 1000, false, true), new InitLNParams()};
		Network net = new BarabasiAlbert(500,5, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(0.05)};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.NORMAL, BailoutFee.FACTOR, BailoutFee.EXPECTED};
		double[] facs = {1,10,1.2}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS, AcceptFee.THRESHOLD, AcceptFee.EXPECTED};
		double[] thres = {Double.MAX_VALUE, 100, 1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					m[k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/records-BA-"+i+"-"+j+"-"+k+".txt",reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
				}
			}
		}
		for (int i = 0; i < reacts.length; i++) {
		   m[reacts.length*bfees.length*afees.length+i] 
				=  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/records-BA-"+i+".txt",reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait); 
		} 
		Series.generate(net, m, 1); 
	
	}
	
	public static void testBAMulti() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		Transformation[] trans = new Transformation[] {new InitCapacities(200,BalDist.EXP), 
				new Transactions(200, TransDist.NORMAL, false, 10000, false, true), new InitLNParams()};
		Network net = new BarabasiAlbert(500,5, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(0.05)};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.NORMAL};
		double[] facs = {1,10,1.2}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS};
		double[] thres = {Double.MAX_VALUE, 100, 1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					m[k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/records-BA-"+i+"-"+j+"-"+k+".txt",reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait, true); 
				}
			}
		}
		for (int i = 0; i < reacts.length; i++) {
		   m[reacts.length*bfees.length*afees.length+i] 
				=  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/records-BA-"+i+".txt",reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait,true); 
		} 
		Series.generate(net, m, 1); 
	
	}
	
	public static void testBAGriefing() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		Transformation[] trans = new Transformation[] {new InitCapacities(400000,BalDist.EXP), 
				new Transactions(40000, TransDist.EXP, false, 10000, 10, false), new InitLNParams()};
		Network net = new BarabasiAlbert(500,5, trans);
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionGriefingRandom(0.01, new GriefingAttack(100000,3000))};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.NORMAL, BailoutFee.FACTOR, BailoutFee.TOTAL, BailoutFee.TOTALEDGE,
				BailoutFee.BUFFER};
		double[] facs = {1,10,1.2,1,1}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS, AcceptFee.THRESHOLD, 
				AcceptFee.TOTAL, AcceptFee.TOTALEDGE, AcceptFee.NEXT};
		double[] thres = {Double.MAX_VALUE, 100, 1,1,1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					m[k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/records-BA-"+i+"-"+j+"-"+k+".txt",reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
				}
			}
		}
		for (int i = 0; i < reacts.length; i++) {
		   m[reacts.length*bfees.length*afees.length+i] 
				=  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/records-BA-"+i+".txt",reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait); 
		} 
		Series.generate(net, m, 1); 
	
	}
	
	public static void testDAS(int run) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Network net = new ReadableFile("LIGHTNING", "LIGHTNING", file, null);
		Metric[] m = new Metric[] {new DegreeDistribution(), new ShortestPaths()};
		Series.generate(net, m, run, run); 
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void testBailoutSmall() {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/bailout/");
		Network net = new ReadableFile("LND", "LND", "./data/simple/simpleCon_graph.txt", new Transformation[] {new InitLNParams()});
		PaymentReaction[] reacts = new PaymentReaction[] {new PaymentReactionDelayRandom(0.05)};
		RoutePaymentBailout.BailoutFee[] bfees = new RoutePaymentBailout.BailoutFee[] { BailoutFee.NORMAL, BailoutFee.FACTOR, BailoutFee.EXPECTED};
		double[] facs = {1,10,1.2}; 
		RoutePaymentBailout.AcceptFee[] afees = new RoutePaymentBailout.AcceptFee[] {AcceptFee.ALWAYS, AcceptFee.THRESHOLD, AcceptFee.EXPECTED};
		double[] thres = {Double.MAX_VALUE, 100, 1}; 
		double wait = 60; 
		Metric[] m = new Metric[reacts.length*bfees.length*afees.length+reacts.length];
		for (int i = 0; i < reacts.length; i++) {
			for (int j = 0; j < bfees.length; j++) {
				for (int k = 0; k < afees.length; k++) {
					m[k*bfees.length*reacts.length+j*reacts.length+i] = 
							new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/record-test-LND4.txt",reacts[i],
									bfees[j], facs[j], afees[k], thres[k], wait); 
				}
			}
		}
		for (int i = 0; i < reacts.length; i++) {
		   m[reacts.length*bfees.length*afees.length] 
				=  new RoutePaymentBailout(new LND(new HopDistance()),1,0.1, "data/bailout/record-test-LND4.txt",reacts[i],
						BailoutFee.NEVER, 0, AcceptFee.ALWAYS, 0, wait); 
		}
		Series.generate(net, m, 1); 
		
		
	}

}
