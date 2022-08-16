package paymentrouting.route.mimo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

public class FileFormats {
	public static String sep=" "; 
	public static String sepTx="-";   
	
	
	public static void addTimes(String original, String mimo, String newOriginal, String newMimo, double lambda, double compute,
			int trxOri, int trxMimo) {
		try {
			Random rand = new Random(); 
			BufferedReader brOri = new BufferedReader(new FileReader(original));
			BufferedWriter bwOri = new BufferedWriter(new FileWriter(newOriginal));
			bwOri.write("# Graph Property Class \npaymentrouting.datasets.TransactionList\n# Key\nTRANSACTION_LIST \n" + 
					"TRANSACTIONS: " + trxOri +"\n");
			String line = brOri.readLine();
			double time = 0;
			int epoch = 0; 
			Vector<Double> times = new Vector<Double>();
			while (line != null) {
				String[] parts = line.split(sep, 2); 
				int curEpoch = Integer.parseInt(parts[0]);
				if (curEpoch != epoch) {
					epoch = curEpoch;
					times.add(time); 
				}
				String out = time+sep+parts[1];
				bwOri.write(out);		
				line = brOri.readLine();
				if (line != null) {
					bwOri.newLine();
					double d = rand.nextDouble();
					//inversion method for d=exp(-lambda*delay) as distribution assumed to be Poisson 
					double delay = -lambda*Math.log(d); 
					time = time + delay;
				} else {
					times.add(time); 
				}
			}
			
			brOri.close();
			bwOri.flush();
			bwOri.close();
			
			
			BufferedReader brMimo = new BufferedReader(new FileReader(mimo));
			BufferedWriter bwMimo = new BufferedWriter(new FileWriter(newMimo));
			bwMimo.write("# Graph Property Class \npaymentrouting.datasets.TransactionList\n# Key\nTRANSACTION_LIST\n" + 
					"TRANSACTIONS: " + trxMimo +"\n");
			line = brMimo.readLine();
			while (line != null) {
				String[] parts = line.split(sep,2);
				String[] numbs = line.split(sepTx); 
				int ep = Integer.parseInt(numbs[0]);
				double t = times.get(ep)+compute;
				String out = t + sep + parts[1];
				bwMimo.write(out);
				line = brMimo.readLine();
				if (line != null) {
					bwMimo.newLine();
				}
			}
			brMimo.close();
			bwMimo.flush();
			bwMimo.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void makeMappingFiles(String mimo, String atomic, String oriMapping, String mimoMapping) {
		try {  
		     BufferedReader br = new BufferedReader(new FileReader(mimo));
		     BufferedWriter bw = new BufferedWriter(new FileWriter(atomic));
		     bw.write("# Graph Property Class \npaymentrouting.route.mimo.AtomicMapping\n# Key\nATOMIC_MAPPING\n");
		     HashMap<String,Integer> map = new HashMap<String,Integer>();
		     
		     String line = br.readLine();
		     int countTx = 0;
		     int countAt = 0; 
		     while (line != null) {
		    	 String[] parts = line.split(" ", 2); 
		    	 Integer a = map.get(parts[0]);
		    	 if (a == null) {
		    		 map.put(parts[0], countAt);
		    		 a = countAt; 
		    		 countAt++; 
		    	 }
		    	 String out = countTx + " " + a;
		    	 bw.write(out);
		    	 line = br.readLine();
		    	 if (line != null) {
		    		 countTx++; 
		    		 bw.newLine();
		    	 }
		     }
		     
		     br.close();
		     bw.flush();
		     bw.close();
		     
		     br = new BufferedReader(new FileReader(oriMapping));
		     bw = new BufferedWriter(new FileWriter(mimoMapping));
		     bw.write("# Graph Property Class \npaymentrouting.route.mimo.MimoMapping\n# Key\nMIMO_MAPPING\n");
		     
		     line = br.readLine();
		     while (line != null) {
		    	 String[] parts = line.split(sep);
		    	 String out = parts[0];
		    	 for (int j = 1; j < parts.length; j++) {
		    		 Integer a = map.get(parts[j]);
		    		 out = out + " " + a; 
		    	 }
		    	 bw.write(out);
		    	 line = br.readLine();
		    	 if (line != null) {
		    		 bw.newLine(); 
		    	 }
		     }
		     
		     br.close();
		     bw.flush();
		     bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
