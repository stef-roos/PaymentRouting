package paymentrouting.route.bailout;

public abstract class PaymentReaction {
	
	public PaymentReaction() {
		
	}
	
	//accept a payment
	public abstract boolean acceptLock(int node, double time, double val);
	
	//forward hash on return; returns time until reaction, 0 for 'normal' peers  
	public abstract double forwardHash(int node, double timenow, double timeLock);
	
	//resolve failed payment before timeout; time until reaction  
	public abstract double resolve(int node, double timenow, double timeLock);
	
	//react as receiver
	public abstract double reactReceiver(int node, double timenow, double timeLock);
	

}
