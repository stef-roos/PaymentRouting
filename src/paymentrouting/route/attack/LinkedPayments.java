package paymentrouting.route.attack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import gtna.graph.Graph;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;

public class LinkedPayments extends AttackPathSelection {
	HashMap<Integer, HashMap<Integer,HashMap<Integer,HashSet<int[]>>>> caught; //transactions seen per sender and receiver and attacker 
	                                                         //(characterized by nr, and time it has been hold,whether it should be failed) 
	double fraction;
	int delay; 
	boolean colluding;
	
	int correctAttack;
	int incorrectAttack;
	int missedAttack;
	int notrelated; 

	public LinkedPayments(PathSelection select, double fraction, int delay, boolean c) {
		super("LINKED_PAYMENTS_"+fraction+"_"+delay+"_"+c, select);
		this.fraction = fraction;
		this.delay = delay; 
		this.colluding = c; 
				
		this.caught = new HashMap<Integer, HashMap<Integer,HashMap<Integer,HashSet<int[]>>>>(); 
	}

	@Override
	public void selectAtt(Graph g, Random rand) {
		int nodes = g.getNodeCount();
		this.att = new boolean[nodes];
		int choose = fraction<=0.5?(int)Math.round(this.fraction*nodes):(int)Math.round((1-this.fraction)*nodes);
		for (int i = 0; i < choose; i++) {
			int j = rand.nextInt(nodes);
			while (att[j]) {
				j = rand.nextInt(nodes);
			}
			this.att[j] = true;
		}
		
		//if more than 50% attackers, negate
		if (this.fraction > 0.5) {
			for (int k = 0; k < this.att.length; k++) {
				this.att[k] = !this.att[k]; 
			}
		}
		this.correctAttack = 0;
		this.incorrectAttack = 0;
		this.missedAttack = 0;
		this.notrelated = 0; 
		
	}

	@Override
	public double[] performAttack(Graph g, int cur, int dst, int pre, boolean[] excluded, RoutePayment rp,
			double curVal, Random rand, int reality) {
		if (!(rp instanceof RoutePaymentConcurrent)) {
			throw new IllegalArgumentException("Linking payments only makes sense when there are concurrent payments, use RoutePaymentConcurrent");
		}
		RoutePaymentConcurrent rpc = (RoutePaymentConcurrent)rp;
		
		// Case 1: is a new payment rather than one delayed at the node
		if (pre != cur) {
			//retrieve set of transactions with same sender and receiver 
			//if null: transaction put on hold
			int[] data = {rpc.curT.getNr(),0,0};//nr only used to compute statistics on whether linking correct, not for attack (as unknown to atatcker)
			boolean putonhold = false;
			HashMap<Integer,HashMap<Integer,HashSet<int[]>>> trS = this.caught.get(rpc.curT.getSrc());
			if (trS == null) {
				trS = new HashMap<Integer,HashMap<Integer,HashSet<int[]>>>();
				this.caught.put(rpc.curT.getSrc(), trS); 
			}
			HashMap<Integer,HashSet<int[]>> trSR = trS.get(dst);
			if (trSR == null) {
				trSR = new HashMap<Integer,HashSet<int[]>>(); 
				trS.put(dst,trSR);
				putonhold = true;  
			}
			if (!putonhold) {
				putonhold = true;
				//iterate over attackers to get all transactions when colluding 
				Iterator<Integer> itAtt = trSR.keySet().iterator();
				while (itAtt.hasNext()) {
					int att = itAtt.next();
					//only consider nodes other than the current one if there is collusion 
				    if (this.colluding || att == cur) {
				    	//go over all of the transaction with same sender and receiver 
				    	HashSet<int[]> tx = trSR.get(att);
				    	if (tx == null) continue; 
				    	Iterator<int[]> itTr = tx.iterator();
				    	while (itTr.hasNext()) {
				    		int[] tr = itTr.next();
				    		//System.out.println("Found transaction with number " + tr[0] + "(observed at attackr"+ att +") at " + rpc.curT.getTime()+
				    		//		" when observing transaction " + data[0] + " by attacker " + cur); 
				    		//check whether transaction tr still on-hold
				    		if (tr[1] <= delay) {
				    			//System.out.println("Still on hold transaction with number " + tr[0] + "(observed at attackr"+ att +") at " + rpc.curT.getTime()+ 
				    				//	" when observing transaction " + data[0] + " by attacker " + cur);
				    			//there is a tr that is a potential split -> drop both 
				    			putonhold = false;
				    			tr[2] = 1; //marks as dropped 
				    			//check whether it was a correct linking
				    			if (tr[0] == data[0]) {
				    				this.correctAttack++;
				    			} else {
				    				this.incorrectAttack++; 
				    			} 
				    		} else {
				    			
				    			//transaction has already been forwarded, check if link was missed 
				    			if (tr[0] == rpc.curT.getNr()) {
				    				//System.out.println("Missed " + tr[0] + " at " + rpc.curT.getTime()+
						    		//		" when observing transaction " + data[0]); 
				    				if (!excluded[att]) {
				    					this.missedAttack++; 
				    				} else {
				    					this.notrelated++;
				    				}
				    			} else {
				    				this.notrelated++;
				    			}
				    		}
				    	}
				    }
				}
			}
			//put on hold or fail
			if (putonhold) {
				HashSet<int[]> tx = trSR.get(cur);
				if (tx == null) {
					tx = new HashSet<int[]>();
					trSR.put(cur,tx);
				}
				tx.add(data);
				return this.getResZero(g, cur);
			} else {
				return null;
			}
		} else { //Case 2: delayed payment 
			//retrieve payment info
			HashSet<int[]> curTX = this.caught.get(rpc.curT.getSrc()).get(dst).get(cur); 
			Iterator<int[]> it = curTX.iterator();
			int[] data=null; 
			while (it.hasNext()) {
				int[] dat = it.next();
				if (dat[0] == rpc.curT.getNr() && dat[1] <= delay) {
					data = dat;
					break;
				}
			}
			//check if it has been marked failed
			
			//increment time payment has been delayed 
			data[1]++;
				if (data[1] > delay) {
					//maximum reached: no more delaying -> dropping or normal behavior
					if (data[2] == 1) {
						//System.out.println("Failed from hold " + data[0] +" at time "+ rpc.curT.getTime());
					   return null;
					} else {	
						//System.out.println("Release from hold " + data[0] +" at time "+ rpc.curT.getTime());
					   return this.sel.getNextsVals(g, cur, dst, pre, excluded, rp, curVal, rand, reality);
					}
				} else {
					return this.getResZero(g, cur); 
				}
			
			
			 
		}
	}
	
	/**
	 * assign all 0s to indicate staying at the same node 
	 * @param g
	 * @param cur
	 * @return
	 */
	public double[] getResZero(Graph g, int cur) {
		int l = g.getNodes()[cur].getOutDegree();
		double[] res = new double[l];
		return res; 
	}

	@Override
	public void prepareAttack(Graph g, int next, double curVal, Random rand) {
		//no prep needed 
		
	} 
	



}

