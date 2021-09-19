package paymentrouting.route.bailout;

import paymentrouting.route.PathSelection;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;

public class RoutePaymentBailout extends RoutePaymentConcurrent{
	PaymentReaction react;

	public RoutePaymentBailout(PathSelection ps, int trials, double latency, PaymentReaction react) {
		super(ps, trials, latency);
		this.react = react; 
	}
	
	@Override 
	protected boolean agree(int curN, double val, double curTime) {
		return this.react.acceptLock(curN, curTime, val);
	}
	
	

}
