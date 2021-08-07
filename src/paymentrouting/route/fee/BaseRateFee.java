package paymentrouting.route.fee;

import java.util.HashMap;

import gtna.graph.Edge;
import gtna.graph.GraphProperty;
import gtna.io.Filereader;
import gtna.io.Filewriter;
import treeembedding.credit.Transaction;

public class BaseRateFee extends GraphProperty {
	double[][] fees;
	
	public BaseRateFee() {
		
	}
	
    public BaseRateFee(double[][] f) {
		this.fees = f; 
	}
    
    public double[] getFees(int i) {
    	return this.getFees(i);
    }
    
    public void makeConstant(double valBase, double valRate, int nodes) {
    	this.fees = new double[nodes][2];
    	for (int i = 0; i < this.fees.length; i++) {
    		this.fees[i][0] = valBase;  
    		this.fees[i][1] = valRate; 
    	}
    }

	@Override
	public boolean write(String filename, String key) {
		Filewriter fw = new Filewriter(filename);

		this.writeHeader(fw, this.getClass(), key);
		fw.writeln("NODES: "+this.fees.length);
		
		for (int i = 0; i < this.fees.length; i++) {
			fw.writeln(i + " " + this.fees[i][0] + this.fees[i][1]);
		}
		
		return fw.close(); 
	}

	@Override
	public String read(String filename) {
		Filereader fr = new Filereader(filename);

		String key = this.readHeader(fr);
		int t = Integer.parseInt(fr.readLine().split(": ")[1]);
		this.fees = new double[t][2];
		
		String line = null;
		while ((line = fr.readLine()) != null) {
			String[] parts = line.split(" ");
			if (parts.length < 3) continue;
			int nr = Integer.parseInt(parts[0]);
			this.fees[nr][0] = Double.parseDouble(parts[1]); 
			this.fees[nr][1] = Double.parseDouble(parts[2]); 
		}

		fr.close();

		return key;
	}

}
