package paymentrouting.route.fee;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

import gtna.data.Single;
import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.Distribution;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;
import paymentrouting.datasets.TransactionList;
import paymentrouting.route.PartialPath;
import paymentrouting.route.PathSelection;
import paymentrouting.route.RoutePayment;
import treeembedding.credit.CreditLinks;
import treeembedding.credit.Transaction;

public class RoutePaymentFees extends RoutePayment {
	FeeComputation fc;  
	int considered; //trees considered 
	int needed; //trees needed
	boolean active; //active merchant 
	
	private double fee_av;
	private double fee_med;
	private double fee_q1;
	private double fee_q2;
	
	HashMap<Edge, double[]> minusPots; 
	HashMap<Edge, Double> originalWeight;

	public RoutePaymentFees(PathSelection ps, int trials, boolean up, 
			FeeComputation fee, int c, int p, boolean a) {
		super(ps, trials, up, 
				new Parameter[] {new StringParameter("FEE", fee.getName()),
						new IntParameter("CONSIDERED", c),
						new IntParameter("NEEDED", p),
						new BooleanParameter("ACTIVE", a)});
		this.considered = c;
		this.needed = p;
		this.active = a; 
		this.fc = fee; 
	}
	
	public RoutePaymentFees(PathSelection ps, int trials, boolean up, 
			FeeComputation fee, int c, int p, boolean a, int epoch) {
		super(ps, trials, up, epoch,
				new Parameter[] {new StringParameter("FEE", fee.getName()),
						new IntParameter("CONSIDERED", c),
						new IntParameter("NEEDED", p),
						new BooleanParameter("ACTIVE", a)});
		this.considered = c;
		this.needed = p;
		this.active = a; 
		this.fc = fee; 
	}
	
	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		//init values
		rand = new Random();
		this.select.initRoutingInfo(g, rand);
		this.edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		HashMap<Edge, Double> originalAll = new HashMap<Edge,Double>();
		this.transactions = ((TransactionList)g.getProperty("TRANSACTION_LIST")).getTransactions();
		Node[] nodes = g.getNodes();
		
		this.avHops = 0;
		this.avHopsSucc = 0; 
		this.avMess = 0;
		this.avMessSucc = 0; 
		this.successFirst = 0;
		this.success = 0;
		long[] trys = new long[2];
		long[] path = new long[2];
		long[] pathSucc = new long[2];
		long[] mes = new long[2];
		long[] mesSucc = new long[2];
		int count = this.transactions.length;
		Vector<Double> totalFees = new Vector<Double>();
		int len = this.transactions.length/this.tInterval; 
		int rest = this.transactions.length % this.tInterval; 
		if (rest == 0) {
		    this.succTime = new double[len];
		} else {
			this.succTime = new double[len+1];
		}
		int slot = 0;
		
		//iterate over transactions
		for (int i = 0; i < this.transactions.length; i++) {
			Transaction tr = this.transactions[i];
			int src = tr.getSrc();
			int dst = tr.getDst();
			double val = tr.getVal();
			if (log) System.out.println("Src-dst " + src + "," + dst + " " + val); 
			boolean s = true; //successful
	    	int h = 0; //hops
	    	int x = 0; //messages
					    
		//attempt at most trials times
		    for (int t = 0; t < this.trials; t++) {
		//set initial set of current nodes and partial payment values to (src, totalVal) 
		    	//split val into val/needed for considered paths 
		    	Vector<PartialPath> pps = new  Vector<PartialPath>();
		    	double perPath = val/this.needed; 
		    	double[] splitVal = this.selectRealities(perPath, rand);
		    	double[] fees = new double[splitVal.length];
		    	for (int a = 0; a < splitVal.length; a++) {
		    		if (splitVal[a] > 0) {
		    			if (log) System.out.println("selected reality " + a); 
		    	        pps.add(new PartialPath(src, splitVal[a],new Vector<Integer>(),a));
		    		} else {
		    			fees[a] = Double.MAX_VALUE;
		    		}
		        }
		    	boolean[] excluded = new boolean[nodes.length];
		    	this.minusPots = new HashMap<Edge,double[]>();
		    	 
		    	this.originalWeight = new HashMap<Edge,Double>(); //updated credit links
		    	Vector<Vector<int[]>> linksPerDim = new Vector<Vector<int[]>>();
		    	for (int a = 0; a < splitVal.length; a++) {
		    		linksPerDim.add(new Vector<int[]>());
		    	}
		
		       //while current set of nodes is not empty 
		    	while (!pps.isEmpty()) {
		    		if (log) {
		    			System.out.println("Hop " + h + " with " + pps.size() + " links ");
		    		}
		    		Vector<PartialPath> next = new  Vector<PartialPath>();
		            //iterate over set of current set of nodes 
		            for (int j = 0; j < pps.size(); j++) {
		            	PartialPath pp = pps.get(j);
		            	int cur = pp.node;
		            	int pre = -1;
		            	Vector<Integer> past = pp.pre;
		            	if (past.size() > 0) {
		            		pre = past.get(past.size()-1);
		            	}
		            	for (int l = 0; l < past.size(); l++) {
		            		excluded[past.get(l)] = true;
		            	}
		            	
		            	if (log) System.out.println("Routing at cur " + cur + " in dim " + pp.reality); 
		                //getNextVals -> distribution of payment value over neighbors
		                double[] partVals = this.select.getNextsVals(g, cur, dst, 
		                		pre, excluded, this, pp.val, rand, pp.reality);  
		                if (log) {
		                	if (partVals == null) System.out.println("No next hop found"); 
		                }
		                for (int l = 0; l < past.size(); l++) {
		            		excluded[past.get(l)] = false;
		            	}
		               
		                //add neighbors that are not the dest to new set of current nodes 
		                if (partVals != null) {
		                	past.add(cur);
		                	int[] out = nodes[cur].getOutgoingEdges();
		                	for (int k = 0; k < partVals.length; k++) {
		                		if (partVals[k] > 0) {
		                			//compute fees
		                			double partf = this.fc.getFee(g, edgeweights, partVals[k], cur, out[k]);
		                			if (h > 0) {
		                			    fees[pp.reality] = fees[pp.reality] + partf;
		                			}
		                			
		                			//increase message count
		                			x++;
		                			
		                			//update vals 
		                			Edge e = edgeweights.makeEdge(cur, out[k]);
		    						double w = edgeweights.getWeight(e);
		    						if (!originalWeight.containsKey(e)){
		    							originalWeight.put(e, w);
		    						}
		    						if (this.update && !originalAll.containsKey(e)) {
		    							originalAll.put(e, w); 
		    						}
		    						edgeweights.setWeight(cur,out[k],partVals[k]);
		    						
		    						//add links to sets
		                			if (out[k] != dst) {
		                				//add partf as previous link needs to sustain it 
		                				next.add(new PartialPath(out[k], partVals[k]+partf, 
		                						(Vector<Integer>)past.clone(),pp.reality));
		                			}
		                			linksPerDim.get(pp.reality).add(new int[] {cur,out[k]}); 
		                			double[] mins = this.minusPots.get(e);
		                			if (mins == null) {
		                				mins = new double[] {0,0};
		                				this.minusPots.put(e, mins);
		                			}
		                			if (cur < out[k]) {
		                				mins[0] = mins[0] + partVals[k];
		                			} else {
		                				mins[1] = mins[1] + partVals[k];
		                			}
		                			
		                			if (log) {
		        		    			System.out.println("add link (" + cur + "," + out[k] + ") with val "+partVals[k]);
		        		    		}
		                		}
		                	}
		                } else {
		                	//failure to split for one dimension 
		                	fees[pp.reality] = Double.MAX_VALUE;; 
		                }	
		            }
		            pps = this.merge(next); 
		            h++;
		    	}
		    	//select paths to choose
		    	int cF = 0;
		    	for (int a = 0; a < fees.length; a++) { 
		    		if (log) System.out.println(fees[a]); 
		    		if (fees[a] != Double.MAX_VALUE) {
		    			cF++; 
		    		}
		    	}
		    	boolean[] chosen = new boolean[fees.length];
		    	if (cF < this.needed) {
		    		//not enough paths -> fail
		    		s = false;
		    	} else {
		    		//find needed minimal paths
		    		double total = 0;
		    		for (int j = 0; j < this.needed; j++) {
		    			double min = Double.MAX_VALUE;
		    			Vector<Integer> mins = new Vector<Integer>(); 
		    			for (int a = 0; a < fees.length; a++) {
		    				if (!chosen[a] && fees[a] != Double.MAX_VALUE) {
		    					if (fees[a] < min) {
		    						mins = new Vector<Integer>();
		    						min = fees[a];
		    					}
		    					if (fees[a] <= min) {
		    						mins.add(a); 
		    					}
		    				}
		    			}
		    			int sel = mins.get(rand.nextInt(mins.size()));
		    			chosen[sel] = true; 
		    			total = total + min;
		    		}
		    		totalFees.add(total);
		    	}
		    	
		    	if (!s) {
		    		h--; 
		    		//payments were not made -> return to original weights
		    		this.weightUpdate(edgeweights,originalWeight);
		    		if (log) {
		    			System.out.println("Failure");
		    		}
		    	} else {
		    		if (!this.update) {
		    			//return credit links to original state
		    			this.weightUpdate(edgeweights,originalWeight);
		    		} else {
		    			//update dimensions that were not used
		    			for (int a = 0; a < chosen.length; a++) {
		    				if (!chosen[a]) {
		    					Vector<int[]> vec = linksPerDim.get(a);
		    					for (int l = 0; l < vec.size(); l++) {
		    						int[] e = vec.get(l);
		    						//change link in opposite direction 
		    						edgeweights.setWeight(e[1], e[0], perPath); 
		    					}
		    				}
		    			}
		    		}

		    		//update stats for this transaction 
			    	
			    		pathSucc = inc(pathSucc, h);
			    		mesSucc = inc(mesSucc, x);
			    		this.succTime[slot]++;
			    	
		    		this.success++;
		    		if (t == 0) {
		    			this.successFirst++;
		    		}
		    		trys = inc(trys,t);
		    		if (log) {
		    			System.out.println("Success");
		    		}
		    	}
		    	
		    	path = inc(path, h);
		    	mes = inc(mes,x);
		    	if ((i+1) % this.tInterval == 0) {
		    		this.succTime[slot] = this.succTime[slot]/this.tInterval;
		    		slot++;
		    	}
		    	
		    }
		    
		    //recompute spanning trees
		    if (this.recompute_epoch != Integer.MAX_VALUE && (i+1) % this.recompute_epoch == 0) {
		    	this.select.initRoutingInfo(g, rand);
		    }
		}

		//compute final stats
		this.hopDistribution = new Distribution(path,count);
		this.messageDistribution = new Distribution(mes,count);
		this.hopDistributionSucc = new Distribution(pathSucc,(int)this.success);
		this.messageDistributionSucc = new Distribution(mesSucc,(int)this.success);
		this.trysDistribution = new Distribution(trys,count);
		this.avHops = this.hopDistribution.getAverage();
		this.avHopsSucc = this.hopDistributionSucc.getAverage();
		this.avMess = this.messageDistribution.getAverage();
		this.avMessSucc = this.messageDistributionSucc.getAverage();
		this.success = this.success/this.transactions.length;
		this.successFirst = this.successFirst/this.transactions.length;
		double[] tf = new double[totalFees.size()];
		this.fee_av = 0;
		if (tf.length > 0) {
		  for (int i = 0; i < tf.length; i++) {
			tf[i] = totalFees.get(i);
			this.fee_av = this.fee_av + tf[i];
		  }
		  this.fee_av = this.fee_av/tf.length; 
		  Arrays.sort(tf);
		  int mid = tf.length/2; 
		  if (tf.length % 2 == 0) {
			  this.fee_med = tf[mid-1]+tf[mid];
		  } else {
			  this.fee_med = tf[mid];
		  }
		  int q = tf.length/4; 
		  this.fee_q1 = tf[q];
		  this.fee_q2 = tf[mid+q]; 
		}
		if (rest > 0) {
			   this.succTime[this.succTime.length-1] = this.succTime[this.succTime.length-1]/rest;
			}
		
		//reset weights for further metrics using them 
		if (this.update) {
			this.weightUpdate(edgeweights, originalAll);
		}
	}
	
	@Override
	public Single[] getSingles() {
		Single[] sSingle = super.getSingles();
		Single[] nSingle = new Single[sSingle.length+4];
		for (int i = 0; i < sSingle.length; i++) {
			nSingle[i] = sSingle[i];
		}
		nSingle[sSingle.length] = new Single(this.key + "_FEE_AV", this.fee_av);
		nSingle[sSingle.length+1] = new Single(this.key + "_FEE_MED", this.fee_med);
		nSingle[sSingle.length+2] = new Single(this.key + "_FEE_Q1", this.fee_q1);
		nSingle[sSingle.length+3] = new Single(this.key + "_FEE_Q3", this.fee_q2);

		return nSingle;
	}
	
    private double[] selectRealities(double ppVal, Random rand) {
    	double[] splitVal = new double[this.select.dist.realities];
    	if (this.active) {
    		
    	} else {
    		for (int i = 0; i < splitVal.length; i++) {
    			int r = rand.nextInt(splitVal.length);
    			while (splitVal[r] > 0) {
    				r = rand.nextInt(splitVal.length);
    			}
    			splitVal[r] = ppVal;
    		}
    	}
    	return splitVal;
    }
    
    @Override 
    public double computePotential(int s, int t) {
    	Edge e = edgeweights.makeEdge(s, t);
    	double[] diffs = this.minusPots.get(e); 
    	double subtract = 0;
    	double val = this.edgeweights.getPot(s, t); 
    	if (diffs != null) {
    		//obtain minimum it can drop to for this payment 
    		if (s < t) {
    			subtract = diffs[0];
    		} else {
    			subtract = diffs[1];
    		}
    		double curWeight = edgeweights.getWeight(e); 
    		edgeweights.setWeight(e, this.originalWeight.get(e));
    		val = this.edgeweights.getPot(s, t); 
    		edgeweights.setWeight(e, curWeight);
    	}
    	return val-subtract;
	}
}
