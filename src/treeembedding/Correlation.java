package treeembedding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Correlation {
	
	public static void writeCoCPerNetwork(String bw, String deg, String folder, int nr, String res){
		double[][][] c = new double[nr][3][3]; 
		for (int i = 0; i < c.length; i++){
			c[i] = readCoCPerRun(folder+i+"/coc.txt");
		}
		double[][] av = new double[3][3];
		for (int i = 0; i < c.length; i++){
			for (int j = 0; j < 3; j++){
				for (int k = 0; k < 3; k++){
					av[j][k] = av[j][k]+c[i][j][k];
				}
			}
		}
		for (int j = 0; j < 3; j++){
			for (int k = 0; k < 3; k++){
				av[j][k] = av[j][k]/c.length;
			}
		}
		double[][] std = new double[3][3];
		for (int i = 0; i < c.length; i++){
			for (int j = 0; j < 3; j++){
				for (int k = 0; k < 3; k++){
					std[j][k] = std[j][k]+(c[i][j][k]-av[j][k])*(c[i][j][k]-av[j][k]);
				}
			}
		}
		for (int j = 0; j < 3; j++){
			for (int k = 0; k < 3; k++){
				std[j][k] = Math.sqrt(std[j][k]/c.length);
			}
		}
		try {
			BufferedWriter fw = new BufferedWriter(new FileWriter(folder+"coc.txt"));
			String[] labels = {"level" , "degree", "betweenness"};
			fw.write("NORMAL	HABE	ONLY");
			for (int i = 0; i < 3; i++){
				fw.newLine();
				String l = labels[i];
				for (int j = 0; j < 3; j++){
					l = l + "	" + av[i][j]+"+"+std[i][j];
				}
				fw.write(l);
			}
			fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public static double[][] readCoCPerRun(String file){
		double[][] c = new double[3][3];
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			br.readLine();
			for (int i = 0; i < 3; i++){
				String[] parts = br.readLine().split("	");
				for (int j = 0; j < 3; j++){
					c[i][j] = Double.parseDouble(parts[j+1]);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return c;
	}
	
	public static void writeCoCPerRun(String bw, String deg, String folder){
		try {
		BufferedWriter fw = new BufferedWriter(new FileWriter(folder+"coc.txt"));
		double[][] c = new double[3][];
		c[0] = getCorTrees(folder+"LEVEL_LIST/ll-list.txt",folder);
		c[1] = getCorTrees(deg,folder);
		c[2] = getCorTrees(bw,folder);
		String[] labels = {"level" , "degree", "betweenness"};
		fw.write("NORMAL	HABE	ONLY");
		for (int i = 0; i < 3; i++){
			fw.newLine();
			String l = labels[i];
			for (int j = 0; j < 3; j++){
				l = l + "	" + c[i][j];
			}
			fw.write(l);
		}
		fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static double[] getCorTrees(String list, String folder){
		double[] cors = new double[3];
		cors[0] = getCor(list, folder+"TREE_ROUTE_NORMAL/trn-traffic.txt");
		cors[1] = getCor(list, folder+"TREE_ROUTE_HABE/trh-traffic.txt");
		cors[2] = getCor(list, folder+"TREE_ROUTE_ONLY/tro-traffic.txt");
		return cors;
	}

	public static double getCor(String list1, String list2){
		//get arrays and check if of the same length
		Double[] x = getVals(list1);
		Double[] y = getVals(list2);
		if (x.length != y.length){
			System.out.println(list1 + " " + list2);
			throw new IllegalArgumentException("same number of values needed");
		}
		//compute means
		double mean_x = 0;
		double mean_y = 0;
		for (int i = 0; i < x.length; i++){
			mean_x = mean_x + x[i];
			mean_y = mean_y + y[i];
		}
		mean_x = mean_x/x.length;
		mean_y = mean_y/y.length;
		double cov = 0;
		double var_x = 0;
		double var_y = 0;
		for (int i = 0; i < x.length; i++){
			cov = cov + (x[i]-mean_x)*(y[i]-mean_y);
			var_x = var_x + (x[i]-mean_x)*(x[i]-mean_x);
			var_y = var_y + (y[i]-mean_y)*(y[i]-mean_y);
		}
		return cov/Math.sqrt(var_x*var_y);
	}
	
	private static Double[] getVals(String list){
		ArrayList<Double> x = new ArrayList<Double>();
		try{
		BufferedReader br = new BufferedReader(new FileReader(list));
		String line;
		while ((line = br.readLine()) != null){
			String[] parts = line.split("	");
			if (parts.length > 1){
				double val = Double.parseDouble(parts[1].split("	")[0]);
				x.add(val);
			}
		}
		br.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		return x.toArray(new Double[x.size()]);
	}

}
