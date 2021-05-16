package paymentrouting.route.attack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import gtna.data.Series;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.util.ReadableFile;
import gtna.transformation.Transformation;
import gtna.transformation.partition.LargestWeaklyConnectedComponent;
import gtna.util.Config;
import paymentrouting.datasets.FixedPairs;
import paymentrouting.datasets.InitCapacities;
import paymentrouting.datasets.InitCapacities.BalDist;
import paymentrouting.datasets.Transactions;
import paymentrouting.datasets.Transactions.TransDist;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.SpeedyMurmursMulti;
import paymentrouting.route.SplitClosest;
import paymentrouting.route.SplitIfNecessary;
import paymentrouting.route.SummarizeResults;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;

public class LinkabilityEval {

	public static void main(String[] args) {
		printResults(); System.exit(0);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/linkability/");
		Config.overwrite("SERIES_GRAPH_WRITE", ""+false);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+false);
		int runs = 100;
		int trs = 100;
		double[] trval = {25,100};
		double initCap = 200;
		double[] freq = {120/(double)6329,6*120/(double)6329,60*120/(double)6329,600*120/(double)6329};
		int[] delay = {0,3,12};
		double[] fraction = {0.01, 0.1, 0.5};
		int trees = 5;
		for (int i = 0; i < freq.length; i++) {
					runPerc(runs, trs, trval[0], initCap, freq[i], delay, fraction, trees);
					runPerc(runs, trs, trval[1], initCap, freq[i], delay, fraction, trees);
		}
		

	}
	
	public static void runPerc(int runs, int trs, double trval, double initCap, double freq, int[] delay, double[] fraction, int trees) {
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new LargestWeaklyConnectedComponent(),
				new InitCapacities(initCap,0.05*initCap, BalDist.EXP), 
				new Transactions(trval, 0.1*trval, TransDist.EXP, false, trs, freq, false, new FixedPairs(true))};
		Network net = new ReadableFile("LN", "LN", file, trans);
		DistanceFunction speedyMulti = new SpeedyMurmursMulti(trees);
		Metric[] m = new Metric[8*delay.length*fraction.length];
		int index = 0; 
		for (int j = 0; j < delay.length; j++) {
			for (int k = 0; k < fraction.length; k++) {
				LinkedPayments linknoCollClose = new LinkedPayments(new SplitClosest(speedyMulti), fraction[k], delay[j], false); 
				LinkedPayments linknoCollNecc = new LinkedPayments(new SplitIfNecessary(speedyMulti), fraction[k], delay[j], false); 
				LinkedPayments linkCollClose = new LinkedPayments(new SplitClosest(speedyMulti), fraction[k], delay[j], true); 
				LinkedPayments linkCollNecc = new LinkedPayments(new SplitIfNecessary(speedyMulti), fraction[k], delay[j], true); 
				m[index++] = new RoutePaymentConcurrent(linknoCollClose,1, 0.1);
				m[index++] =  new LinkabilitySuccess(linknoCollClose);
				m[index++] = new RoutePaymentConcurrent(linknoCollNecc,1, 0.1);
				m[index++] =  new LinkabilitySuccess(linknoCollNecc);
				m[index++] = new RoutePaymentConcurrent(linkCollClose,1, 0.1);
				m[index++] =  new LinkabilitySuccess(linkCollClose);
				m[index++] = new RoutePaymentConcurrent(linkCollNecc,1, 0.1);
				m[index++] =  new LinkabilitySuccess(linkCollNecc);
			}
		}	
		
		Series.generate(net, m, runs); 
	}
	
	public static void printResults() {
		String[] coll = new String[] {"false", "true"};
		String[] val = new String[] {"25.0", "100.0"};
		String[] algo = new String[] {"SPLIT_CLOSEST", "SPLIT_IFNECESSARY"};
		String[] metrics = new String[] {"INCORRECT_ATTACK", "MISSED_ATTACK",
				 "CORRELATE_ATTACK", "INCORRECT_ATTACK_TOTAL", "MISSED_ATTACK_TATAL", "F_SCORE", "ATTACK_FRAC"}; 
		
		double[] txs = {1/(double)60, 0.1, 1, 10};
		double[] freq = {120/(double)6329,6*120/(double)6329,60*120/(double)6329,600*120/(double)6329};
		int[] delay = {0,3,12};
		double[] fraction = {0.01, 0.1, 0.5}; 
		
		for (int i = 0; i < coll.length; i++) {
			for (int j = 0; j < val.length; j++) {
				for (int k = 0; k < algo.length; k++) {
					for (int m = 0; m < metrics.length; m++) {
						try {
							BufferedWriter writer = new BufferedWriter(new FileWriter("data/linkability/"+algo[k]+"-val="
						+val[j]+"-coll="+coll[i]+"-"+metrics[m]+".txt"));
						    String line = "#Algo";
						    for (int n =0; n < fraction.length; n++) {
						    	for (int p = 0; p < delay.length; p++) {
						    		line = line + " " + fraction[n]+ "-" + delay[p];
						    	}
						    }
						    writer.write(line);
						    for (int f = 0; f < freq.length; f++) {
						    	line = txs[f]+"";
						    	for (int n =0; n < fraction.length; n++) {
							    	for (int p = 0; p < delay.length; p++) {
							    		String file = "data/linkability/"
							    		+"READABLE_FILE_LN-6329--LARGEST_WEAKLY_CONNECTED_COMPONENT--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-"+
							    			val[j]	+ "-EXP-false-100-"+freq[f]+"-false-FIXED_PAIRS/LINKABILITY-"+delay[p]+"-"+coll[i]+"-"
							    					+ fraction[n]+"-"+algo[k]+"/_singles.txt"; 
							    		double[] avVar = SummarizeResults.getSingleVar(file, metrics[m]); 
							    		line = line + " " + avVar[0];
							    	}
							    }
						    	writer.newLine();
							    writer.write(line);
						    }
						    writer.flush();
						    writer.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
			
	}
	
	public static void getFscore() {
		String[] coll = new String[] {"false", "true"};
		String[] val = new String[] {"25.0", "100.0"};
		String[] algo = new String[] {"SPLIT_CLOSEST", "SPLIT_IFNECESSARY"};
		String[] metrics = new String[] {"INCORRECT_ATTACK", "MISSED_ATTACK",
				 "CORRELATE_ATTACK", "INCORRECT_ATTACK_TOTAL", "MISSED_ATTACK_TATAL"}; 
		
		double[] txs = {1/(double)60, 0.1, 1, 10};
		double[] freq = {120/(double)6329,6*120/(double)6329,60*120/(double)6329,600*120/(double)6329};
		int[] delay = {0,3,12};
		double[] fraction = {0.01, 0.1, 0.5}; 
		
		for (int i = 0; i < coll.length; i++) {
			for (int j = 0; j < val.length; j++) {
				for (int k = 0; k < algo.length; k++) {
					//for (int m = 0; m < metrics.length; m++) {
						try {
							BufferedWriter writer = new BufferedWriter(new FileWriter("data/linkability/"+algo[k]+"-val="
						+val[j]+"-coll="+coll[i]+"-FSCORE.txt"));
						    String line = "#Algo";
						    for (int n =0; n < fraction.length; n++) {
						    	for (int p = 0; p < delay.length; p++) {
						    		line = line + " " + fraction[n]+ "-" + delay[p];
						    	}
						    }
						    writer.write(line);
						    for (int f = 0; f < freq.length; f++) {
						    	line = txs[f]+"";
						    	for (int n =0; n < fraction.length; n++) {
							    	for (int p = 0; p < delay.length; p++) {
							    		double s = 0;
							    		int count = 0;
							    		for (int o = 0; o < 100; o++) {
							    		String file = "data/linkability/"
							    		+"READABLE_FILE_LN-6329--LARGEST_WEAKLY_CONNECTED_COMPONENT--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-"+
							    			val[j]	+ "-EXP-false-100-"+freq[f]+"-false-FIXED_PAIRS/"+o+"/LINKABILITY-"+delay[p]+"-"+coll[i]+"-"
							    					+ fraction[n]+"-"+algo[k]+"/_singles.txt"; 
							    		double incorrect = getSingleAv(file, "INCORRECT_ATTACK_TOTAL");  
							    		double missed = getSingleAv(file, "MISSED_ATTACK_TATAL"); 
							    		double tp = 0;
							    		if (incorrect > 0) {
							    		double incorrectFr = getSingleAv(file, "INCORRECT_ATTACK");
							    		    tp = incorrect/incorrectFr - incorrect;
							    		
							    		double fscore = tp/(tp+0.5*(missed+incorrect)); 
							    		s = s + fscore; 
							    		count++; 
							    		}
							    		}
							    		s = s/count;  
							    		line = line + " " + s;
							    	}
							    }
						    	writer.newLine();
							    writer.write(line);
						    }
						    writer.flush();
						    writer.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				
			}
		}
	}
	
	public static double getSingleAv(String file, String single) {
		//System.out.println(file); 
		double res = -1;
		try { 
			BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if (line.contains(single)) {
		    		String[] numbers = (line.split("=")[1]).split("	");
		    		res = Double.parseDouble(numbers[0]);
		    		break;
		    	}
		    }
		    br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res; 
	}

}
