package paymentrouting.sourcerouting;

public class RoutingResult {
	private int[][] paths;
	private boolean success;
	private int tries;
	private int hops;
	private int mes; 
	private double fees;
	
	public RoutingResult(int[][] paths, boolean success, int tries, int hops, int mes, double fees) {
		this.paths = paths;
		this.success = success;
		this.tries = tries;
		this.hops = hops;
		this.mes = mes; 
		this.fees = fees; 
	}

	public double getFees() {
		return fees;
	}

	public void setFees(double fees) {
		this.fees = fees;
	}

	public int[][] getPaths() {
		return paths;
	}

	public void setPaths(int[][] paths) {
		this.paths = paths;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getTries() {
		return tries;
	}

	public void setTries(int tries) {
		this.tries = tries;
	}

	public int getHops() {
		return hops;
	}

	public void setHops(int hops) {
		this.hops = hops;
	}

	public int getMes() {
		return mes;
	}

	public void setMes(int mes) {
		this.mes = mes;
	}

}
