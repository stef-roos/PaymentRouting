package paymentrouting.sourcerouting;

import paymentrouting.datasets.LNParams;
import treeembedding.credit.CreditLinks;

/**
 * based on Dan Andreescu's work 
 * https://github.com/dandreescu/PaymentRouting/blob/lightning/src/paymentrouting/route/costfunction/CostFunction.java
 *
 */

public interface CostFunction {
	
	  double compute(int src, int dst, double amt, CreditLinks edgeweights, LNParams params,
              boolean direct);
	  
	  double computeFee(int src, int dst, double amt, CreditLinks edgeweights, LNParams params,
              boolean direct);

}
