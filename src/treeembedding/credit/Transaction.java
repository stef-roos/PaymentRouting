package treeembedding.credit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class Transaction {

	int src;
	int dst;
	double val;
	double time;
	int requeue = 0;
	int mes = 0;
	int path = 0;
	
	public Transaction(double t, double v, int s, int d){
		this.src = s;
		this.dst = d;
		this.val = v;
		this.time = t;
	}
	
	public void incRequeue(double nexttime){
		this.requeue++;
		this.time = nexttime;
	}
	
	public void addPath(int p){
		this.path = this.path+p;
	}
	
	public void addMes(int p){
		this.mes = this.mes+p;
	}
	
	public static Vector<Transaction> readTransactions(String file) {
		Vector<Transaction> vec = new Vector<Transaction>();
		try{ 
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    int count = 0;
		    while ((line =br.readLine()) != null){
		    	String[] parts = line.split(" ");
		    	if (parts.length == 4){
		    		Transaction ta = new Transaction(Double.parseDouble(parts[0]),
		    				Double.parseDouble(parts[1]),
		    				Integer.parseInt(parts[2]),
		    				Integer.parseInt(parts[3]));
		    		vec.add(ta);
		    	}
		    	if (parts.length == 3){
		    		Transaction ta = new Transaction(count, 
		    				Double.parseDouble(parts[0]),
		    				Integer.parseInt(parts[1]),
		    				Integer.parseInt(parts[2]));
		    		vec.add(ta);
		    		count++;
		    	} 
		    }
		    br.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		 return vec;
	}

	public int getSrc() {
		return src;
	}

	public void setSrc(int src) {
		this.src = src;
	}

	public int getDst() {
		return dst;
	}

	public void setDst(int dst) {
		this.dst = dst;
	}

	public double getVal() {
		return val;
	}

	public void setVal(double val) {
		this.val = val;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

}
