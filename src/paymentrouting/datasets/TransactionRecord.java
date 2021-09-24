package paymentrouting.datasets;

public class TransactionRecord {
	int pre;
	int succ;
	double startT;
	double endT;
	double val;
	double interval;
	boolean success; 
	
	public TransactionRecord(double t, double val, int p, int s) {
		this(t,val,p,s, -1); 
	}
	
	public TransactionRecord(double t, double val, int p, int s, double inter) {
		this.startT = t;
		this.val = val; 
		this.pre = p;
		this.succ = s; 
		this.interval = inter; 
		this.endT = Double.MAX_VALUE; 
	}

	public int getPre() {
		return pre;
	}

	public void setPre(int pre) {
		this.pre = pre;
	}

	public int getSucc() {
		return succ;
	}

	public void setSucc(int succ) {
		this.succ = succ;
	}

	public double getStartT() {
		return startT;
	}

	public void setStartT(double startT) {
		this.startT = startT;
	}

	public double getEndT() {
		return endT;
	}

	public void setEndT(double endT) {
		this.endT = endT;
	}

	public double getVal() {
		return val;
	}

	public void setVal(double val) {
		this.val = val;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuuccess(boolean suuccess) {
		this.success = suuccess;
	}
	
	public double getDuration() {
		return this.endT - this.startT;
	}
	
	public double getInterval() {
		return interval;
	}

	public void setInterval(double interval) {
		this.interval = interval;
	}
	
	@Override 
	public String toString() {
		return this.val + " " + this.pre + " " + this.succ + " " + this.startT + " " + this.endT + " "  + this.success; 
	}

}
