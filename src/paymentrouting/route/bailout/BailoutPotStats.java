package paymentrouting.route.bailout;

import java.util.ArrayList;
import java.util.HashMap;

import gtna.data.Single;
import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.DataWriter;
import gtna.io.Filewriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.Distribution;
import paymentrouting.datasets.CapacityList;

public class BailoutPotStats extends Metric {
	String f;
	Distribution degreeDiff; 
	Distribution capDiff; 
	Distribution alternatives; 
	double avDegDiff; 
	double avCapDiff; 
	double avAlt;
	double atLeastOne; 
	double corDC; 

	public BailoutPotStats(String file) {
		super("BAILOUT_STATS");
		this.f = file; 
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		Filewriter fw = new Filewriter(this.f); 
		CapacityList capList = (CapacityList) g.getProperty("CAPACITIES"); 
		HashMap<Edge, Integer> cmap = capList.getCap(); 
		Node[] nodes = g.getNodes(); 
		ArrayList<int[]> degrees = new ArrayList<int[]>();
		ArrayList<int[]> caps = new ArrayList<int[]>();
		ArrayList<Integer> alts = new ArrayList<Integer>();
		
		for (int a = 0; a < nodes.length; a++) { //node A
			int[] outA = nodes[a].getOutgoingEdges();
			int A = a; 
			for (int b = 0; b < outA.length; b++) { //node B
				int B = outA[b]; 
				int[] outB = nodes[B].getOutgoingEdges();
				int[] counts = new int[outB.length]; 
				for (int d = 0; d < outA.length; d++) { //node D
					if (b == d)  continue; 
					int D = outA[d]; 
					//common neighbor of b and d
					for (int c = 0; c < outB.length; c++) {
						if (outB[c] == A) continue; 
						if (nodes[outB[c]].hasNeighbor(D)) {
							//found a square 
							int C = outB[c]; 
							int degreeA = outA.length;
							int degreeB = outB.length;
							int degreeC = nodes[C].getOutDegree();
							int degreeD = nodes[D].getOutDegree();
							int capAB = cmap.get(new Edge(A,B)); 
							int capBC = cmap.get(new Edge(B,C)); 
							int capCD = cmap.get(new Edge(C,D)); 
							int capDA = cmap.get(new Edge(D,A)); 
							counts[c]++;
							degrees.add(new int[] {degreeB, degreeD});
							caps.add(new int[] {capAB, capBC, capCD, capDA}); 
							
							fw.writeln(A + "(" + degreeA + ") -- " + capAB + " -- " + B + "(" + degreeB + ") -- " + capBC + " -- "
									+ C + "(" + degreeC + ") -- " + capCD + " -- " + D + "(" + degreeD + ") -- " + capDA + " -- ");
						}
					}
				}
				for (int l = 0; l < counts.length; l++) {
					alts.add(counts[l]); 
				}
			}
		}
		//compute square numbs
		this.atLeastOne = 0;
		this.avAlt = 0;
		long[] freAlt = new long[1];
		for (int i = 0; i < alts.size(); i++) {
			freAlt = this.inc(freAlt, alts.get(i));
			this.avAlt = this.avAlt + alts.get(i);
			if (alts.get(i) > 0) {
				this.atLeastOne++;
			}
		}
		this.atLeastOne = this.atLeastOne/(double)alts.size();
		this.avAlt = this.avAlt/(double)alts.size();
		this.alternatives = new Distribution(freAlt, alts.size());
		
		
		//compute degree diff, cap diff, correlation
		long[] dd = new long[1]; 
		long[] cd = new long[1];
		int dd2 = 0; 
		int cd2 = 0; 
		this.corDC = 0;
		double testav = 0; 
		for (int i = 0; i < degrees.size(); i++) {
			int[] degs = degrees.get(i);
			int ddiff = degs[0]-degs[1];
			int ddiffabs = Math.abs(ddiff);
			dd = this.inc(dd, ddiffabs);
			
			int[] cap = caps.get(i);  
			int min1 = Math.min(cap[0], cap[1]); 
			int min2 = Math.min(cap[2], cap[3]); 
			int cdiff = (min1-min2)/1000;
			int cdiffabs = Math.abs(cdiff);
			cd = this.inc(cd, cdiffabs);
			
			dd2 = dd2 + ddiff*ddiff;
			cd2 = cd2 + cdiff*cdiff;
			this.corDC = this.corDC + ddiff*cdiff; 
			testav = testav + ddiff;
			
		}
		System.out.println(testav); 
		this.degreeDiff = new Distribution(dd, degrees.size()); 
		this.capDiff = new Distribution(cd, degrees.size());
		this.avDegDiff = this.degreeDiff.getAverage();
		this.avCapDiff = this.capDiff.getAverage();
		this.corDC = this.corDC/(double)(Math.sqrt(dd2*cd2)); 

		
		fw.close();
	}
	
	private long[] inc(long[] values, int index) {
		try {
			values[index]++;
			return values;
		} catch (ArrayIndexOutOfBoundsException e) {
			long[] valuesNew = new long[index + 1];
			System.arraycopy(values, 0, valuesNew, 0, values.length);
			valuesNew[index] = 1;
			return valuesNew;
		}
	}

	@Override
	public boolean writeData(String folder) {
		boolean succ = true;
		succ &= DataWriter.writeWithIndex(this.alternatives.getDistribution(),
				this.key+"_ALTERNATIVES", folder);
		succ &= DataWriter.writeWithIndex(this.degreeDiff.getDistribution(),
				this.key+"_DEGREE_DIFF", folder);
		succ &= DataWriter.writeWithIndex(this.capDiff.getDistribution(),
				this.key+"_CAP_DIFF", folder);
		
		return succ;
	}

	@Override
	public Single[] getSingles() {
		Single avA = new Single("AV_ALTERNATIVES", this.avAlt); 
		Single atLA = new Single("AT_LEAST_ONE_ALT", this.atLeastOne);
		Single avDDiff = new Single("AV_DEG_DIFF", this.avDegDiff); 
		Single avCDiff = new Single("AV_CAP_DIFF", this.avCapDiff); 
		Single cor = new Single("COR_DEG_CAP", this.corDC);
		return new Single[] {avA,atLA,avDDiff, avCDiff, cor};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		// TODO Auto-generated method stub
		return g.hasProperty("CAPACITIES");
	}

}
