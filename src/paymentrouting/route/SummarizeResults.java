package paymentrouting.route;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class SummarizeResults {
	public static String path = "";
	
	public static void main(String[] args) {
        dynConcurrent("ROUTE_PAYMENT_SUCCESS="); 
//		String[][] singles = {{"SUCCESS=", "succ"},
//				              {"MES_AV=", "mes"},
//		                      {"MES_AV_SUCC=", "mesSucc"}};
//		for (int i = 0; i < singles.length; i++) {
//			resAttacks(singles[i][0], singles[i][1]);
////		          resTopology(singles[i][0], singles[i][1]); 
////		          resCapTrans(singles[i][0], singles[i][1]);
////		          resVals(singles[i][0], singles[i][1]);
////		          resTrees(singles[i][0], singles[i][1]);
//		}
	}
	
	public static void dynConcurrent(String single) {
		String[] vals = {"25.0", "100.0"};
		String[] rates = {"0.1", "2.0", "100.0"};
		String[] algos = new String[] { "CLOSEST_NEIGHBOR", "SPLIT_CLOSEST", "SPLIT_IFNECESSARY"};
		String[] dist = new String[] {
				"HOP_DISTANCE",
				"SPEEDYMURMURS_MULTI_5", 
		};
		for (int j = 0; j < vals.length; j++) {
			for (int r = 0; r < rates.length; r++) {
				String line = vals[j] + " & " + rates[r];
				for (int k = 0; k < dist.length; k++) {
					for (int m = 0; m < algos.length; m++) {
						double[] res = getSingleVar("data/con-lightning/"
								+ "READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-200.0-EXP--"
								+ "TRANSACTIONS-"+vals[j]+"-EXP-false-100000-"+rates[r]+"-false/"
								+ "ROUTE_PAYMENT-1-true-"+dist[k]+"-"+algos[m]+"-2147483647-0.1"
								+ "/_singles.txt",
								single); 
						line = line + " & " + "$" + 
							      String.format("%.2f", res[0]) + " \\pm " + String.format("%.3f", res[1]) + "$"; 
					}
				}
				System.out.println(line + "\\\\"); 
			}
		}
	}
	
	public static void resAttacks(String single, String name) {
		String[] attack = new String[]{"NON_COLLUDING_DROP_SPLITS", "COLLUDING_DROP_SPLITS"}; 
		String[] attackName = new String[]{"non-coll", "coll"};
		String[] algos = new String[] { "SPLIT_CLOSEST", "SPLIT_IFNECESSARY"};
		String[] algosName = { "Dist", "IfN"};
		String[] dels = {"12", "0"};
		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(path + "attack" + name + ".txt"));
			String line = "#";
			for (int a = 0; a < attack.length; a++) {
				for (int i = 0; i < dels.length; i++) {
					for (int j = 0; j < algos.length; j++) {
						line = line + " " + attackName[a]+"-"+algosName[j]+"-"+dels[i];
					}
				}
			}	
			bw.write(line);
			for (int k = 0; k < 11; k++) {
				line = k * 0.1 + "";
				for (int a = 0; a < attack.length; a++) {
					for (int i = 0; i < dels.length; i++) {
						for (int j = 0; j < algos.length; j++) {
							double[] r = getSingleVar("data/attack-lightning/"
									+ "READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-100.0-EXP-false-10000-false-true"
									+ "/ROUTE_PAYMENT-"+attack[a]+"_"+k*0.1+"_"+ dels[i]+"_"+algos[j]+
									"-1-false-SPEEDYMURMURS_MULTI_5-2147483647/_singles.txt",
									single);
							line = line + "	" + r[0] + "	" + r[1];
						}
					}
				}
				bw.newLine();
				bw.write(line);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void resLocks(String single, String name) {
		String[] algos = new String[] { "CLOSEST_NEIGHBOR", "SPLIT_CLOSEST", "SPLIT_IFNECESSARY"};
		String[] algosName = { "No", "Dist", "IfN"};
		String[] dist = new String[] {
				"HOP_DISTANCE",
				"SPEEDYMURMURS_MULTI_1",
				"SPEEDYMURMURS_MULTI_2",
				"SPEEDYMURMURS_MULTI_3",
				"SPEEDYMURMURS_MULTI_4",
				"SPEEDYMURMURS_MULTI_5", 
				"SPEEDYMURMURS_MULTI_6",
				"SPEEDYMURMURS_MULTI_7",
				"SPEEDYMURMURS_MULTI_8",
				"SPEEDYMURMURS_MULTI_9", 
				"SPEEDYMURMURS_MULTI_10",
		};
		String[] distances = { "INT-SM" };
		String[] maxlocks = {"", "_MIN", "_MAX", "_CONST_12", "_CONST_24"}; 
		String[] locknames = {"NONE", "MIN", "MAX", "DIAMETER", "2DIAMETER"};

		for (int i = 0; i < algos.length; i++) {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(path+"locks"+algosName[i]+name+".txt"));
				String line = "#";
				//get locks 
				for (int d = 0; d < distances.length; d++) {
					for (int a = 0; a < maxlocks.length; a++) {
						line = line + "	" + distances[d]+"-"+locknames[a];
					}
				}
				bw.write(line);
				
				//get + write results per tree
				for (int j = 1; j <= 10; j++) {
					//get res per lock 
				    line = j + ""; 
				    for (int a = 0; a < maxlocks.length; a++) {
				    	 double[] r = getSingleVar("data/locks-nopadding/" 
							+ "READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-100.0-EXP-false-10000-false-true"
							+ "/"+algos[i]+"-1-false-"+dist[j]+maxlocks[a]+"/_singles.txt", single);
				         line = line + "	" + r[0] + "	" + r[1];
				    	
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
	
	public static void resTopologyNew(String single, String name) {
		String[] algos = new String[] { "CLOSEST_NEIGHBOR", "SPLIT_CLOSEST", "SPLIT_IFNECESSARY"};
		String[] algosName = { "No", "Dist", "IfN"};
		String[] dist = new String[] { "HOP_DISTANCE", "SPEEDYMURMURS_MULTI_5" };
		String[] distances = { "HOP", "INT-SM" };
		String[] path = {"BARABASI_ALBERT-6329-5--LARGEST_WEAKLY_CONNECTED_COMPONENT--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-100.0-EXP-false-10000-false-true",
				         "ERDOS_RENYI-6329-10.31-true--LARGEST_WEAKLY_CONNECTED_COMPONENT--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-100.0-EXP-false-10000-false-true"
				         };
		String[] graphName = {"BA", "ER", "LN"};

		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter("data/check-changes/Topology" + name + ".txt"));
			String line = "#distri";
			// get names
			for (int d = 0; d < distances.length; d++) {
				for (int a = 0; a < algos.length; a++) {
					line = line + "	" + distances[d] + "-" + algosName[a];
				}
			}
			bw.write(line);
			for (int i = 0; i < path.length; i++) {
				line = graphName[i];
				// get results
				for (int d = 0; d < distances.length; d++) {
					for (int a = 0; a < algos.length; a++) {
						double[] r = getSingleVar("data/check-changes/"
								+ path[i]+ "/ROUTE_PAYMENT-" + algos[a] + "-1-false-" + dist[d]
								+ "-2147483647/_singles.txt", single);
						line = line + "	" + r[0] + "	" + r[1];
					}
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
	
	public static void resTopology(String single, String name) {
		String[] algos = new String[] { "CLOSEST_NEIGHBOR", "SPLIT_CLOSEST", "SPLIT_IFNECESSARY"};
		String[] algosName = { "No", "Dist", "IfN"};
		String[] dist = new String[] { "HOP_DISTANCE", "SPEEDYMURMURS_MULTI_5" };
		String[] distances = { "HOP", "INT-SM" };
		String[] path = {"BARABASI_ALBERT-6329-5--LARGEST_WEAKLY_CONNECTED_COMPONENT--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-100.0-EXP-false-10000-false-true",
				         "ERDOS_RENYI-6329-10.31-true--LARGEST_WEAKLY_CONNECTED_COMPONENT--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-100.0-EXP-false-10000-false-true",
				         "READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-100.0-EXP-false-10000-false-true"};
		String[] graphName = {"BA", "ER", "LN"};

		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(path+"Topology" + name + ".txt"));
			String line = "#distri";
			// get names
			for (int d = 0; d < distances.length; d++) {
				for (int a = 0; a < algos.length; a++) {
					line = line + "	" + distances[d] + "-" + algosName[a];
				}
			}
			bw.write(line);
			for (int i = 0; i < path.length; i++) {
				line = graphName[i];
				// get results
				for (int d = 0; d < distances.length; d++) {
					for (int a = 0; a < algos.length; a++) {
						double[] r = getSingleVar("data/lightning-nopadding/"
								+ path[i]+ "/" + algos[a] + "-1-false-" + dist[d]
								+ "/_singles.txt", single);
						line = line + "	" + r[0] + "	" + r[1];
					}
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
	
	public static void resCapTrans(String single, String name) {
		String[] algos = new String[] { "CLOSEST_NEIGHBOR", "SPLIT_CLOSEST", "SPLIT_IFNECESSARY"};
		String[] algosName = { "No", "Dist", "IfN"};
		String[] dist = new String[] { "HOP_DISTANCE", "SPEEDYMURMURS_MULTI_5" };
		String[] vals = { "100.0"};
		String[] distances = { "HOP", "INT-SM" };
		String[][] distri = {{"EXP","EXP"}, {"EXP","NORMAL"}, {"NORMAL","EXP"}, {"NORMAL","NORMAL"}};

		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(path+"capTransDistri" + name + ".txt"));
			String line = "#distri";
			// get names
			for (int d = 0; d < distances.length; d++) {
				for (int a = 0; a < algos.length; a++) {
					line = line + "	" + distances[d] + "-" + algosName[a];
				}
			}
			bw.write(line);
			for (int i = 0; i < distri.length; i++) {
				line = distri[i][0] + "-"+distri[i][1];
				// get results
				for (int d = 0; d < distances.length; d++) {
					for (int a = 0; a < algos.length; a++) {
						double[] r = getSingleVar("data/lightning-nopadding/"
								+ "READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-200.0-"+distri[i][0]+"--TRANSACTIONS-100.0" 
								+ "-"+distri[i][1]+"-false-10000-false-true" + "/" + algos[a] + "-1-false-" + dist[d]
								+ "/_singles.txt", single);
						line = line + "	" + r[0] + "	" + r[1];
					}
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
	
	public static void resVals(String single, String name) {
		String[] algos = new String[] { "CLOSEST_NEIGHBOR", "SPLIT_CLOSEST", "SPLIT_IFNECESSARY"};
		String[] algosName = { "No", "Dist", "IfN"};
		String[] dist = new String[] { "HOP_DISTANCE", "SPEEDYMURMURS_MULTI_5" };
		String[] vals = { "1.0", "5.0", "20.0", "50.0", "100.0", "200.0", "300.0" };
		String[] distances = { "HOP", "INT-SM" };

		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(path+"vals" + name + ".txt"));
			String line = "#";
			// get names
			for (int d = 0; d < distances.length; d++) {
				for (int a = 0; a < algos.length; a++) {
					line = line + "	" + distances[d] + "-" + algosName[a];
				}
			}
			bw.write(line);
			for (int i = 0; i < vals.length; i++) {
				line = vals[i] + "";
				// get results
				for (int d = 0; d < distances.length; d++) {
					for (int a = 0; a < algos.length; a++) {
						double[] r = getSingleVar("data/lightning-nopadding/"
								+ "READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-" + vals[i]
								+ "-EXP-false-10000-false-true" + "/" + algos[a] + "-1-false-" + dist[d]
								+ "/_singles.txt", single);
						line = line + "	" + r[0] + "	" + r[1];
					}
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
	
	public static void resTrees(String single, String name) {
		String[] algos = new String[] {
				"CLOSEST_NEIGHBOR",
				"SPLIT_CLOSEST",
				"SPLIT_IFNECESSARY",
				"RANDOM_SPLIT"
		}; 
		String[] algosName = {"No", "Dist", "IfN", "Rand"};
		String[] dist = new String[] {
				"HOP_DISTANCE",
				"SPEEDYMURMURS_MULTI_1",
				"SPEEDYMURMURS_MULTI_2",
				"SPEEDYMURMURS_MULTI_3",
				"SPEEDYMURMURS_MULTI_4",
				"SPEEDYMURMURS_MULTI_5", 
				"SPEEDYMURMURS_MULTI_6",
				"SPEEDYMURMURS_MULTI_7",
				"SPEEDYMURMURS_MULTI_8",
				"SPEEDYMURMURS_MULTI_9", 
				"SPEEDYMURMURS_MULTI_10",
		}; 
		String[] vals = {"1.0","50.0","200.0"};
		String[] distances = {"HOP", "INT-SM"};
		for (int i = 0; i < vals.length; i++) {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(path+vals[i]+name+".txt"));
				String line = "#";
				//get names 
				for (int d = 0; d < distances.length; d++) {
					for (int a = 0; a < algos.length; a++) {
						line = line + "	" + distances[d]+"-"+algos[a];
					}
				}
				bw.write(line);
				//get hop results
				double[][] hopRes = new double[4][];
				for (int a = 0; a < algos.length; a++) {
					hopRes[a] =  getSingleVar("data/lightning-nopadding/" 
							+ "READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-"+vals[i]+"-EXP-false-10000-false-true"
							+ "/"+algos[a]+"-1-false-"+dist[0]+"/_singles.txt", single);
				}
				//get + write results per tree
				for (int j = 1; j <= 10; j++) {
					//write hop distance results to get line in plot
				    line = j + "";
				    for (int a = 0; a < hopRes.length; a++) {
				    	line = line + "	" + hopRes[a][0] + "	" + hopRes[a][1];
				    }
				    //get results for INT-SM
				    for (int a = 0; a < hopRes.length; a++) {
				    	if (a < 3 || j < 4) {
				         double[] r = getSingleVar("data/lightning-nopadding/" 
							+ "READABLE_FILE_LIGHTNING-6329--INIT_CAPACITIES-200.0-EXP--TRANSACTIONS-"+vals[i]+"-EXP-false-10000-false-true"
							+ "/"+algos[a]+"-1-false-"+dist[j]+"/_singles.txt", single);
				         line = line + "	" + r[0] + "	" + r[1];
				    	} else {
				    		//line = line + "	-1	-1";
				    	}
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
	
	public static void resOld() {
		String[] algos = new String[] {
				"CLOSEST_NEIGHBOR",
				"SPLIT_IFNECESSARY",
				"RANDOM_SPLIT",
				"RANDOM_PROPORTIONAL_SPLIT"
		}; 
		String[] algosName = {"No Split", "Necessary", "Random", "Random Proportional"};
		String[] dist = new String[] {
				"HOP_DISTANCE",
				"SPEEDYMURMURS_1",
				"SPEEDYMURMURS_2",
				"SPEEDYMURMURS_3",
				"SPEEDYMURMURS_4",
				"SPEEDYMURMURS_5",
				"SPEEDYMURMURS_MULTI_2",
				"SPEEDYMURMURS_MULTI_3",
				"SPEEDYMURMURS_MULTI_4",
				"SPEEDYMURMURS_MULTI_5"
		}; 
		String[] nets = (new File("data/merged/")).list();
		Arrays.sort(nets); 
		for (int i = 0; i < nets.length; i++) {
			String[] parts =  nets[i].split("-");
			String net = parts[0].replace("_", "-"); 
			if (parts.length < 10) continue; 
			String init;
			String tr;
			String val;
			if (net.contentEquals("BARABASI-ALBERT")){
				init = parts[6];
				tr = parts[10];
				val = parts[9];
			} else {
				init = parts[9];
				tr = parts[13];
				val = parts[12];
			}
			 
			String nameAll = net + ", average transaction value " + val + ", init capacity distribution " + init +
					", transaction value distribution " + tr;
			String nameShort = net + "-" + val + "-" + init +
					"-" + tr;
			System.out.println("\\begin{table} "); 
			System.out.println("\\begin{tabular}{|r|l|l|l|l|} ");
			String line = " ";
			for (int l = 0; l < algos.length; l++) {
				line = line + " & " + algosName[l]; 
			}
			System.out.println(line + " \\\\");
			for (int j = 0; j < dist.length; j++) {
				line = dist[j].replace("_", "-");;
				for (int l = 0; l < algos.length; l++) {
					double[] res = getSingleVar("data/merged/" + nets[i]
							+"/"+algos[l]+"-1-false-"+dist[j]+"/_singles.txt", "MES_AV_SUCC=");
					if (res != null) {
					    line = line + " & " + "$" + 
					      String.format("%.4f", res[0]) + " \\pm " + String.format("%.5f", res[1]) + "$"; 
					}  else {
						line = line + " & "; 
					}
				}
				System.out.println(line + " \\\\");
			}
			System.out.println("\\end{tabular}");
			System.out.println("\\caption{"+nameAll+"} ");
			System.out.println("\\label{tab:"+nameShort+"} ");
			System.out.println("\\end{table}");
			System.out.println("");
		}
	}
	
	
	
	public static double[] getSingleVar(String file, String single) {
		//System.out.println(file); 
		double[] res = null;
		try { 
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if (line.contains(single)) {
		    		String[] numbers = (line.split("=")[1]).split("	");
		    		res = new double[]{Double.parseDouble(numbers[0]), 
		    				Math.sqrt(Double.parseDouble(numbers[4]))};
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
