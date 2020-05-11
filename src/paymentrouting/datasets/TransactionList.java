package paymentrouting.datasets;

import gtna.graph.GraphProperty;
import gtna.io.Filereader;
import gtna.io.Filewriter;
import treeembedding.credit.Transaction;

public class TransactionList extends GraphProperty {
	int recomputed; 
	int eqFlow;
	Transaction[] transactions;
	boolean time;
	
	public TransactionList(Transaction[] t, boolean time, int rec, int flow) {
		this.transactions = t;
		this.time = time;
		this.recomputed = rec;
		this.eqFlow = flow; 
	}
	
	public TransactionList() {
	}
	
	public Transaction[] getTransactions() {
		return this.transactions;
	}

	@Override
	public boolean write(String filename, String key) {
		Filewriter fw = new Filewriter(filename);

		this.writeHeader(fw, this.getClass(), key);
		fw.writeln("TRANSACTIONS: "+this.transactions.length);
		
		for (int i = 0; i < this.transactions.length; i++) {
			String line = "";
			Transaction tr = this.transactions[i];
			if (this.time) {
				line = line + tr.getTime()+ " ";
			}
			line = line +tr.getVal() + " " + tr.getSrc() + " " + tr.getDst();
			fw.writeln(line);
		}
		return fw.close();
		
	}

	@Override
	public String read(String filename) {
		Filereader fr = new Filereader(filename);
		String key = this.readHeader(fr);
		int t = Integer.parseInt(fr.readLine().split(": ")[1]);
		this.transactions = new Transaction[t];
		String[] parts =readString(fr).split(" ");
		if (parts.length == 3) {
			this.time = false;
		} else {
			this.time = true;
		}
		for (int i = 0; i < t; i++){
			Transaction tx;
			double ti,value;
			int src, dst;
			if (this.time) {
				ti = Double.parseDouble(parts[0]);
				value = Double.parseDouble(parts[1]);
				src = Integer.parseInt(parts[2]);
				dst = Integer.parseInt(parts[3]); 
			} else {
				ti = i; 
				value = Double.parseDouble(parts[0]);
				src = Integer.parseInt(parts[1]);
				dst = Integer.parseInt(parts[2]); 
			}
			tx = new Transaction(ti,value,src,dst);
			this.transactions[i] = tx; 
			String line = readString(fr);
			if (line != null) {
				parts = line.split(" "); 
			}
		}
		return key;
	}

}
