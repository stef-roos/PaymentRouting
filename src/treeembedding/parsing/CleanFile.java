package treeembedding.parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class CleanFile {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		sumUp("parse1.txt",
				"/home/sroos/svns/data/ripple-data-nov7/all-in-USD-trust-lines-2016-nov-7-cleaned.txt"
				);

	}
	
	
	public static void removeInvalidLines(String file, String parsed){
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			BufferedWriter bw = new BufferedWriter(new FileWriter(parsed));
			String line;
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				double low = Double.parseDouble(parts[2]);
				double cur = Double.parseDouble(parts[3]);
				double up = Double.parseDouble(parts[4]);
				if ((-low <= cur && cur < up) || (-low < cur && cur == up)){
					bw.write(line);
					bw.newLine();
				}
			}
			br.close();
			bw.flush();
			bw.close();
		} catch (IOException e){
			
		}
	}
	
	public static void sumUp(String file, String parsed){
		HashMap<String, double[]> vals = new HashMap<String, double[]>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				if (parts.length != 5) break;
				double low = Double.parseDouble(parts[2]);
				double cur = Double.parseDouble(parts[3]);
				double up = Double.parseDouble(parts[4]);
				String nodes = parts[0]+" " + parts[1];
				double[] bl = vals.get(nodes);
				if (bl == null){
					bl = new double[3];
					vals.put(nodes, bl);
				}
				bl[0] = bl[0] + low;
				bl[1] = bl[1] + cur;
				bl[2] = bl[2] + up;
			}
			br.close();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(parsed));
			Iterator<Entry<String,double[]>> it = vals.entrySet().iterator();
			while (it.hasNext()){
				Entry<String,double[]> entry = it.next();
				String l = entry.getKey();
				double[] a = entry.getValue();
				for (int j = 0; j < a.length; j++){
					l = l + " " + a[j];
				}
				bw.write(l);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}

}
