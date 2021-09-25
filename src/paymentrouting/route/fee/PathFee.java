package paymentrouting.route.fee;

public class PathFee {
	int[] path;
	double fee;
	double total;
	double totalLock; 
	



	public PathFee(int[] p, double f, double t, double l) {
		this.path = p;
		this.fee = f;
		this.total = t;
		this.totalLock = l; 
	}
	

	public int[] getPath() {
		return path;
	}

	public void setPath(int[] path) {
		this.path = path;
	}

	public double getFee() {
		return fee;
	}

	public void setFee(double fee) {
		this.fee = fee;
	}

	public double getTotal() {
		return total;
	}

	public void setTotal(double total) {
		this.total = total;
	}
	
	public double getTotalLock() {
		return totalLock;
	}


	public void setTotalLock(double totalLock) {
		this.totalLock = totalLock;
	}

}
