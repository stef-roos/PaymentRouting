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
		Single cor = new Single("CORRECT_ATTACK", this.linkpay.correctAttack);
		Single incor = new Single("INCORRECT_ATTACK", this.linkpay.incorrectAttack);
		Single missed = new Single("MISSED_ATTACK", this.linkpay.missedAttack);
		return new Single[] {cor, incor,missed};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("CREDIT_LINKS") && g.hasProperty("TRANSACTION_LIST");
	}

}
