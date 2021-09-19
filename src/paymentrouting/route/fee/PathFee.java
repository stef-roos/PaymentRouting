package paymentrouting.route.fee;

public class PathFee {
	int[] path;
	double fee;
	double total;
	
	public PathFee(int[] p, double f, double t) {
		this.path = p;
		this.fee = f;
		this.total = t;
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
	

}
