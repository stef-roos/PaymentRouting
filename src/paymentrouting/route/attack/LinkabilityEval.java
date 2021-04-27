package paymentrouting.route.attack;

import gtna.data.Series;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.util.ReadableFile;
import gtna.transformation.Transformation;
import gtna.transformation.partition.LargestWeaklyConnectedComponent;
import gtna.util.Config;
import paymentrouting.datasets.FixedPairs;
import paymentrouting.datasets.InitCapacities;
import paymentrouting.datasets.InitCapacities.BalDist;
import paymentrouting.datasets.Transactions;
import paymentrouting.datasets.Transactions.TransDist;
import paymentrouting.route.DistanceFunction;
import paymentrouting.route.SpeedyMurmursMulti;
import paymentrouting.route.SplitClosest;
import paymentrouting.route.SplitIfNecessary;
import paymentrouting.route.concurrency.RoutePaymentConcurrent;

public class LinkabilityEval {

	public static void main(String[] args) {
		System.out.println(127*164.0/6400.0 + 1/64.0 + 1);
		System.exit(0);
		Config.overwrite("MAIN_DATA_FOLDER", "./data/linkability/");
		Config.overwrite("SERIES_GRAPH_WRITE", ""+false);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", ""+true);
		int runs = 100;
		int trs = 100;
		double[] trval = {25,100};
		double initCap = 200;
		double[] freq = {120/(double)6329,6*120/(double)6329,60*120/(double)6329,600*120/(double)6329};
		int[] delay = {0,3,12};
		double[] fraction = {0.01, 0.1, 0.5};
		int trees = 5;
		for (int i = 0; i < freq.length; i++) {
					runPerc(runs, trs, trval[0], initCap, freq[i], delay, fraction, trees);
					runPerc(runs, trs, trval[1], initCap, freq[i], delay, fraction, trees);
		}
		

	}
	
	public static void runPerc(int runs, int trs, double trval, double initCap, double freq, int[] delay, double[] fraction, int trees) {
		String file  = "lightning/lngraph_2020_03_01__04_00.graph";
		Transformation[] trans = new Transformation[] {new LargestWeaklyConnectedComponent(),
				new InitCapacities(initCap,0.05*initCap, BalDist.EXP), 
				new Transactions(trval, 0.1*trval, TransDist.EXP, false, trs, freq, false, new FixedPairs(true))};
		Network net = new ReadableFile("LN", "LN", file, trans);
		DistanceFunction speedyMulti = new SpeedyMurmursMulti(trees);
		Metric[] m = new Metric[8*delay.length*fraction.length];
		int index = 0; 
		for (int j = 0; j < delay.length; j++) {
			for (int k = 0; k < fraction.length; k++) {
				LinkedPayments linknoCollClose = new LinkedPayments(new SplitClosest(speedyMulti), fraction[k], delay[j], false); 
				LinkedPayments linknoCollNecc = new LinkedPayments(new SplitIfNecessary(speedyMulti), fraction[k], delay[j], false); 
				LinkedPayments linkCollClose = new LinkedPayments(new SplitClosest(speedyMulti), fraction[k], delay[j], true); 
				LinkedPayments linkCollNecc = new LinkedPayments(new SplitIfNecessary(speedyMulti), fraction[k], delay[j], true); 
				m[index++] = new RoutePaymentConcurrent(linknoCollClose,1, 0.1);
				m[index++] =  new LinkabilitySuccess(linknoCollClose);
				m[index++] = new RoutePaymentConcurrent(linknoCollNecc,1, 0.1);
				m[index++] =  new LinkabilitySuccess(linknoCollNecc);
				m[index++] = new RoutePaymentConcurrent(linkCollClose,1, 0.1);
				m[index++] =  new LinkabilitySuccess(linkCollClose);
				m[index++] = new RoutePaymentConcurrent(linkCollNecc,1, 0.1);
				m[index++] =  new LinkabilitySuccess(linkCollNecc);
			}
		}	
		
		Series.generate(net, m, runs); 
	}

}
