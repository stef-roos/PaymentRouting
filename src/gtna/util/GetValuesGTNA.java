package gtna.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

/**
 * get Values from GTNA files
 * @author stef
 *
 */
public class GetValuesGTNA {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (int i = 592; i < 695; i++){
			System.out.println(i + " " + 142095);
		}
		
		
//		String folder = "/home/stef/workspace/stef/data/stat3/";
//		
//		String[] keys = {"CREDIT_NETWORK_SUCCESS", "CREDIT_NETWORK_DELAY_AV", "CREDIT_NETWORK_MES_RE_AV", 
//				"CREDIT_NETWORK_PATH_SINGLE_FOUND_AV", "CREDIT_NETWORK_STAB_AV"};
//		try{
//		BufferedWriter bw = new BufferedWriter(new FileWriter("/home/stef/svns/credit/images/treesSW.txt"));
//		for (int j = 1; j < 8; j++){
//			String line = (j) + "";
//			for (int i = 0; i < keys.length; i++){
//				
//				
//				double[] res = getSingleValue(folder, "CREDIT_NETWORK-SW-T-"+j+"-1000.0-" +
//						"TREE_ROUTE_SILENTW-false-true-2000.0-RANDOM_PARTITIONER-1", keys[i]);
//				line = line + "  " + res[0] + "  " + res[1] + " " + res[2];
//			}
//			bw.write(line);
//			bw.newLine();
//		}
//		bw.flush();
//		bw.close();
//		}catch (IOException e){
//			e.printStackTrace();
//		}
		
		String folder = "/home/stef/workspace/stef/data/stat3/";
		String[] metrics = {
//				"CREDIT_NETWORK-LM-MUL-PER-RAND-1000.0-TREE_ROUTE_SILENTW-false-true-2000.0-RANDOM_PARTITIONER-1",
//				"CREDIT_NETWORK-LM-MUL-DYN-RAND-1000.0-TREE_ROUTE_SILENTW-true-true-2000.0-RANDOM_PARTITIONER-1",
//				"CREDIT_NETWORK-LM-PUSH-PER-RAND-1000.0-TREE_ROUTE_SILENTW-false-false-2000.0-RANDOM_PARTITIONER-1",
//				"CREDIT_NETWORK-LM-PUSH-DYN-RAND-1000.0-TREE_ROUTE_SILENTW-true-false-2000.0-RANDOM_PARTITIONER-1",
//				"CREDIT_NETWORK-GE-MUL-PER-RAND-1000.0-TREE_ROUTE_TDRAP-false-true-2000.0-RANDOM_PARTITIONER-1",
//				"CREDIT_NETWORK-GE-MUL-DYN-RAND-1000.0-TREE_ROUTE_TDRAP-true-true-2000.0-RANDOM_PARTITIONER-1",
//				"CREDIT_NETWORK-GE-PUSH-PER-RAND-1000.0-TREE_ROUTE_TDRAP-false-false-2000.0-RANDOM_PARTITIONER-1",
//				"CREDIT_NETWORK-GE-PUSH-DYN-RAND-1000.0-TREE_ROUTE_TDRAP-true-false-2000.0-RANDOM_PARTITIONER-1"
				"CREDIT_NETWORK-CANAL-SW-1000.0-TREE_ROUTE_ONLY-false-true-2000.0-RANDOM_PARTITIONER-1",
				"CREDIT_NETWORK-CANAL-SM-1000.0-TREE_ROUTE_ONLY-true-false-2000.0-RANDOM_PARTITIONER-1"
				};
		
		String[] keys = {"CREDIT_NETWORK_SUCCESS", "CREDIT_NETWORK_DELAY_AV", "CREDIT_NETWORK_MES_RE_AV", 
				"CREDIT_NETWORK_PATH_SINGLE_FOUND_AV", "CREDIT_NETWORK_STAB_AV"};
		String[] names = {
//				          "LM-MUL-PER (SilentWhispers)",
//				          "LM-MUL-OND",
//				          "LM-RAND-PER", 
//				          "LM-RAND-OND",
//				          "GE-MUL-PER",
//				          "GE-MUL-OND", 
//				          "GE-RAND-PER",
//				          "GE-RAND-OND (SpeedyMurmurs)",
				          "CANAL-SW",
				          "CANAL-SM"};
		try{
		BufferedWriter bw = new BufferedWriter(new FileWriter("/home/stef/svns/credit/images/tableCanal.txt"));
		for (int j = 0; j < metrics.length; j++){
			String line = names[j] + "";
			for (int i = 0; i < keys.length; i++){
				String[] res = getSingnificantDigits(folder, metrics[j], keys[i]);
				line = line + " & $" + res[0] + "$ & $\\pm " + res[1]+"$";
			}
			bw.write(line + "\\\\");
			bw.newLine();
		}
		bw.flush();
		bw.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		//41688
//		String[] keys = {"CREDIT_NETWORK_SUCCESS", "CREDIT_NETWORK_MES_RE_AV", "CREDIT_NETWORK_PATH_SINGLE_FOUND_AV",
//				"CREDIT_NETWORK_STAB_AV"};
//		int[] rd = {2,2,2,2};
//		String folder = "../data/resSum1/";
//		String[] files = (new File(folder)).list();
//		for (int i = 0; i < files.length; i++){
//			if (!files[i].startsWith("READ")) continue;
//			String[] subF = (new File(folder+files[i])).list();
//			for (int j = 0; j < subF.length; j++){
//				if (subF[j].contains("RAND-DEG")){
//					String line = files[i].replace("READABLE_FILE_", "").replace("-67149", "");
//					for (int k = 0; k < keys.length; k++){
//					    double[] res;
//					    if (files[i].contains("PER") && k == 3){
//					    	res = new double[]{199574, 199574, 199574};
//					    } else {
//					    	res = getSingleValue(folder+files[i] + "/", subF[j], keys[k]);
//					    	 if ( k == 3){
//							    	res[0] = res[0]/1000;
//							    	res[1] = res[1]/1000;
//							    	res[2] = res[2]/1000;
//							    }
//					    }
//					   
//					    res[1] = (res[2]-res[1])/2;
//					    String[] formRes = new String[2]; 
//					    for (int l = 0; l < formRes.length; l++){
//					    	res[l] = round(rd[k],res[l]);
//					    	formRes[l] = res[l] +"";
//					    	if (rd[k] != 0){
//					    		double rem = res[l] % 1;
//					    		int post = (""+rem).length()-2;
//					    		while (post < rd[k]){
//					    			formRes[l] = formRes[l] + "0";
//					    			post++;
//					    		}
//					    	}
//					    }
//					    line = line + " & $" + formRes[0] + "$ & $\\pm "+formRes[1]+ "$";
//					}
//					System.out.println(line + "\\\\ \\hline");
//				}
//			}
//			
//		}
		
	}
	
	private static double round(int digit, double num){
		if (digit > 0){
		int fac = (int)Math.pow(10, digit);
		return Math.round(num*fac)/(double)fac;
		} else {
			return (int)Math.round(num);
		}
	}
	
	
	
	
	/**
	 * 
	 * @param folder: network folder
	 * @param metrics: metrics that should be retrieved
	 * @param keys: singles per metric
	 * @return
	 */
	public static double[] getSingleValue(String folder, String[] metrics, String[][] keys){
		int c = 0;
		for (int i = 0; i < keys.length; i++){
			c = c + keys[i].length;
		}
		double[] result = new double[c];
		c = 0;
		for (int i = 0; i < keys.length; i++){
			for (int j = 0; j < keys[i].length; j++){
				try {
					
					BufferedReader br = new BufferedReader(new FileReader(folder + metrics[i] + "/_singles.txt"));
					String line;
					while ((line=br.readLine())!= null){
						if (line.contains(keys[i][j])){
							//System.out.println(c + " " + line);
							String[] parts = (line.split("=")[1]).split("	");
							result[c] = Double.parseDouble(parts[0]);
							c++;
						}
					}
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param folder: network folder
	 * @param metrics: metrics that should be retrieved
	 * @param keys: singles per metric
	 * @return
	 */
	public static double[] getSingleValue(String folder, String metric, String key){
		double[] result = new double[3];
		try {
//					if (!(new File(folder)).exists()){
//						System.out.println("uhm");
//					}
					BufferedReader br = new BufferedReader(new FileReader(folder + metric + "/_singles.txt"));
					String line;
					while ((line=br.readLine())!= null){
						if (line.contains(key)){
							//System.out.println(c + " " + line);
							String[] parts = (line.split("=")[1]).split("	");
							result[0] = Double.parseDouble(parts[0]);
							result[1] = Double.parseDouble(parts[parts.length-2]);
							result[2] = Double.parseDouble(parts[parts.length-1]);
						}
					}
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
		
		return result;
	}
	
	/**
	 * 
	 * @param folder: network folder
	 * @param metrics: metrics that should be retrieved
	 * @param keys: singles per metric
	 * @return
	 */
	public static String[] getSingnificantDigits(String folder, String metric, String key){
		String[] result = new String[2];
		try {
					
					BufferedReader br = new BufferedReader(new FileReader(folder + metric + "/_singles.txt"));
					String line;
					while ((line=br.readLine())!= null){
						if (line.contains(key)){
							//System.out.println(c + " " + line);
							String[] parts = (line.split("=")[1]).split("	");
							double av = Double.parseDouble(parts[0]);
							double std = Math.sqrt(Double.parseDouble(parts[4]));
							result = turnSigDif(av,std);
							
						}
					}
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
		
		return result;
	}
	
	private static String[] turnSigDif(double av, double std){
		int digit = (int) Math.floor(Math.log10(std));
		double fac = Math.pow(10, -digit);
		double avN = Math.round(av*fac)/fac;
		double stdN = Math.round(fac*std)/fac;
		String avS; 
		String stdS;
		if (digit < -1){
			//below zero => check that zeros are printed
			double a = avN % 1;
			int n = (int)Math.floor(avN);
			String end = (a+"");
			end = end.substring(1,end.length());
			while (end.length() < -(digit)+1){
				end = end + "0";
			}
			avS = n + end;
			stdS = stdN + "";
		} else {
			if (digit > 0){
				int p = (int)Math.pow(10, digit);
				double pre = Math.round(avN/p);
				avS = pre + " x 10^"+digit;
				double preS = Math.round(stdN/p);
				stdS = preS + " x 10^"+digit; 
			} else{
			   avS = ""+avN;
			   stdS = ""+stdN;
			}
		}
		return new String[]{avS, stdS};
	}
	
	
	
	/**
	 * get variance 
	 * @param file
	 * @param metric
	 * @return
	 */
	public static double getVarianceGTNASingle(String file, String metric){
		double var = 0;
		try {
			double mean = 0;
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			Vector<Double> vec = new Vector<Double>();
			boolean start = false;
			while ((line = br.readLine()) != null){
				if (line.contains(metric)){
					start = true;
					line = br.readLine();
					line = br.readLine();
					line = br.readLine();
					line = br.readLine();
				}
				if (start){
					if (line.contains("-")){
						start = false; break;
					}
				String[] parts = line.split(" ");
				vec.add(Double.parseDouble(parts[1]));
				}
			}
			br.close();
			for (int i = 0; i < vec.size(); i++){
				mean = mean + vec.get(i);
			}
			mean = mean/vec.size();
			for (int i = 0; i < vec.size(); i++){
				var = var + (vec.get(i)-mean)*(vec.get(i)-mean);
			}
			var = Math.sqrt(var/vec.size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return var;
	}
	
	/**
	 * compute variance and mean for GTNA distribution
	 * @param file
	 * @param metric
	 * @return
	 */
	public static double[] getVarianceMeanGTNASingle(String file, String metric){
		double var = 0;
		double mean = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			Vector<Double> vec = new Vector<Double>();
			boolean start = false;
			while ((line = br.readLine()) != null){
				if (line.contains(metric)){
					start = true;
					line = br.readLine();
					line = br.readLine();
					line = br.readLine();
					line = br.readLine();
				}
				if (start){
					if (line.contains("-")){
						start = false; break;
					}
				String[] parts = line.split(" ");
				vec.add(Double.parseDouble(parts[1]));
				}
			}
			br.close();
			for (int i = 0; i < vec.size(); i++){
				mean = mean + vec.get(i);
			}
			mean = mean/vec.size();
			for (int i = 0; i < vec.size(); i++){
				var = var + (vec.get(i)-mean)*(vec.get(i)-mean);
			}
			var = Math.sqrt(var/vec.size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new double[]{mean,var};
	}

}
