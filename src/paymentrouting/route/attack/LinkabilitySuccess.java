package paymentrouting.route.attack;

import java.util.HashMap;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

public class LinkabilitySuccess extends Metric {
	LinkedPayments linkpay;
	
	public LinkabilitySuccess(LinkedPayments l) {
		super("LINKABILITY", new Parameter[]{new IntParameter("DELAY",l.delay), new BooleanParameter("COLLUSION", l.colluding),
				new DoubleParameter("FRACTION",l.fraction), new StringParameter("SPLITTING", l.sel.getName())});
		this.linkpay = l;
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		// nothing to do as part of running the attack with route payment 
		
	}

	@Override
	public boolean writeData(String folder) {
		// no distributions 
		return true;
	}

	@Override
	public Single[] getSingles() {
		double di = (this.linkpay.correctAttack + this.linkpay.incorrectAttack);
		Single cor = new Single("INCORRECT_ATTACK", di==0?0:(double)this.linkpay.incorrectAttack/(this.linkpay.correctAttack + this.linkpay.incorrectAttack));
		double diM = (this.linkpay.missedAttack + this.linkpay.notrelated);
		Single missed = new Single("MISSED_ATTACK", diM==0?0:(double)this.linkpay.missedAttack/(this.linkpay.missedAttack + this.linkpay.notrelated));
		Single incorTotal = new Single("INCORRECT_ATTACK_TOTAL", this.linkpay.incorrectAttack);
		Single missedTotal = new Single("MISSED_ATTACK_TATAL", this.linkpay.missedAttack);
		Single corr = new Single("CORRELATE_ATTACK", this.linkpay.missedAttack*this.linkpay.incorrectAttack);
		//Fscore
		double f = 1;
		if (this.linkpay.correctAttack+0.5*(this.linkpay.incorrectAttack+this.linkpay.missedAttack) > 0) {
		   f = this.linkpay.correctAttack/(this.linkpay.correctAttack+0.5*(this.linkpay.incorrectAttack+this.linkpay.missedAttack));
		}   
		double attType = 0.5;
		if (this.linkpay.incorrectAttack+this.linkpay.missedAttack>0) {
			attType = this.linkpay.incorrectAttack/(this.linkpay.incorrectAttack+this.linkpay.missedAttack);
		}
		Single fscore = new Single("F_SCORE", f);
		Single attT = new Single("ATTACK_FRAC", attType);
		return new Single[] {cor, missed,corr, incorTotal, missedTotal, fscore, attT};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("CREDIT_LINKS") && g.hasProperty("TRANSACTION_LIST");
	}

}
