package paymentrouting.route.concurrency;

import treeembedding.credit.Transaction;

public class ConcurrentTransaction extends Transaction implements Comparable<ConcurrentTransaction>{
	int nr; 
	
	public ConcurrentTransaction(int n, double t, int s, int d, double val) {
		super(t,val,s,d); 
		this.nr = n;
	}
	
	public ConcurrentTransaction(int n, Transaction t) {
		this(n, t.getTime(), t.getSrc(), t.getDst(), t.getVal()); 
	}

	public int getNr() {
		return nr;
	}


	@Override
	public int compareTo(ConcurrentTransaction o) {
		return (int) Math.signum(this.getTime()-o.getTime());
	}
	
	

}
