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
	
	
	/**
	 * construct transaction files for GTNA, add times not given by mimo computation 
	 * @param original: original transactions (lines in the form epoch paymentVal source receiver)
	 * @param mimo: transactions after mimo (lines in the form epoch-orbitNr paymentVal source receiver)
	 * @param newOriginal: reformatted original, has time instead of epoch + GTNA header
	 * @param newMimo: reformatted mimo, has time instead of epoch-orbit + GTNA header 
	 * @param lambda: lambda for poisson distribution that determines inter-arrival time of original transactions 
	 * @param compute: time to compute mimo after last transaction of epoch, i.e., until mimo transaction start  
	 * @param trxOri: number of original transactions
	 * @param trxMimo: number of mimo transactions 
	 */
	public static void addTimes(String original, String mimo, String newOriginal, String newMimo, double lambda, double compute,
			int trxOri, int trxMimo) {
		try {
			Random rand = new Random(); 
			//original file 
			BufferedReader brOri = new BufferedReader(new FileReader(original));
			BufferedWriter bwOri = new BufferedWriter(new FileWriter(newOriginal));
			bwOri.write("# Graph Property Class \npaymentrouting.datasets.TransactionList\n# Key\nTRANSACTION_LIST \n" + 
					"TRANSACTIONS: " + trxOri +"\n"); //header 
			String line = brOri.readLine();
			double time = 0;
			int epoch = 0; 
			Vector<Double> times = new Vector<Double>();
			while (line != null) {
				//replace epoch with time 
				String[] parts = line.split(sep, 2); 
				int curEpoch = Integer.parseInt(parts[0]);
				if (curEpoch != epoch) {
					epoch = curEpoch;
					times.add(time); //add end of epoch, i.e., when last transaction submitted 
				}
				String out = time+sep+parts[1];
				bwOri.write(out);		
				line = brOri.readLine();
				//increase time using poisson distribution 
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
			
			//mimo file 
			BufferedReader brMimo = new BufferedReader(new FileReader(mimo));
			BufferedWriter bwMimo = new BufferedWriter(new FileWriter(newMimo));
			bwMimo.write("# Graph Property Class \npaymentrouting.datasets.TransactionList\n# Key\nTRANSACTION_LIST\n" + 
					"TRANSACTIONS: " + trxMimo +"\n"); //header 
			line = brMimo.readLine();
			while (line != null) {
				//get epoch and number 
				String[] parts = line.split(sep,2);
				String[] numbs = line.split(sepTx); 
				int ep = Integer.parseInt(numbs[0]);
				double t = times.get(ep)+compute; //compute start time of transactions of the epoch = time last original transaction + compute
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
	/**
	 * format files about mapping both mimo and original payments to orbits for the two graph properties AtomicMapping and MimoMapping
	 * @param mimo: transactions after mimo (lines in the form epoch-orbitNr paymentVal source receiver) -> same as for addTimes 
	 * @param atomic: GraphProperty file for AtomicMapping, maps transaction in file Mimo to numbers associated with orbits  
	 * @param oriMapping: mapping original transactions to the orbits they affect, file of form transactionNumber 
	 * (in file original for addTimes) and list of epoch-orbit pairs 
	 * @param mimoMapping: GraphProperty for oriMapping, turn epoch-orbit pairs into numbers as numbers works for arrays used in code 
	 */
	public static void makeMappingFiles(String mimo, String atomic, String oriMapping, String mimoMapping) {
		try {  
		     BufferedReader br = new BufferedReader(new FileReader(mimo));
		     BufferedWriter bw = new BufferedWriter(new FileWriter(atomic));
		     bw.write("# Graph Property Class \npaymentrouting.route.mimo.AtomicMapping\n# Key\nATOMIC_MAPPING\n");//header 
		     HashMap<String,Integer> map = new HashMap<String,Integer>();//maps epoch-orbit to number, counting increasingly by occurrence  
		     
		     String line = br.readLine();
		     int countTx = 0;
		     int countAt = 0; 
		     while (line != null) {
		    	 //get mapping for epoch-orbit in line 
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
		     bw.write("# Graph Property Class \npaymentrouting.route.mimo.MimoMapping\n# Key\nMIMO_MAPPING\n"); //header
		     line = br.readLine();
		     while (line != null) {
		    	 String[] parts = line.split(sep);
		    	 String out = parts[0];
		    	 for (int j = 1; j < parts.length; j++) {
		    		 Integer a = map.get(parts[j]); //get number for epoch-orbit 
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
