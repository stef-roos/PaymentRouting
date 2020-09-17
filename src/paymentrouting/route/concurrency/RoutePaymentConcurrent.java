package paymentrouting.route.concurrency;

import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;

public class RoutePaymentConcurrent extends RoutePayment {
	int linklatency; //link latency in ms
	int now = 0; 
	
	public RoutePaymentConcurrent(PathSelection ps, int trials, int latency) {
		this(ps,trials,Integer.MAX_VALUE, latency); 
	}
	

	public RoutePaymentConcurrent(PathSelection ps, int trials, int epoch, int latency) {
		super(ps, trials, true, epoch, new Parameter[]{new IntParameter("LINK_LATENCY", latency)});
		this.linklatency = latency; 
	}
	

	
	
	
	

}

