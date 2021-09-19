
package paymentrouting.route;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import gtna.data.Single;
import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.Distribution;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;
import paymentrouting.datasets.TransactionList;
import treeembedding.credit.CreditLinks;
import treeembedding.credit.Transaction;

public class RoutePayment extends Metric{
	protected int trials;
	protected Distribution hopDistribution;
	protected double avHops;
	protected Distribution messageDistribution;
	protected double avMess;
	protected Distribution hopDistributionSucc;
	protected double avHopsSucc;
	protected Distribution messageDistributionSucc;
	protected double avMessSucc;
	protected Distribution trysDistribution;
	protected double success;
	protected double successFirst;
	protected double[] succTime;
	protected Random rand;
	protected boolean update; 
	protected Transaction[] transactions;
	protected boolean log = false;
	protected PathSelection select;
	protected CreditLinks edgeweights; 
	protected int tInterval = 1000;
	protected int recompute_epoch;
	long[] trys;
	long[] path;
	long[] pathSucc;
	long[] mes;
	long[] mesSucc;
	protected HashMap<Edge, Double> originalAll;
	double[] slidingSuccess;
	int window = 500; 
	boolean[] windowRes;
	
	public RoutePayment(PathSelection ps, int trials, boolean up) {
		this(ps,trials,up,Integer.MAX_VALUE); 
	}
	

	public RoutePayment(PathSelection ps, int trials, boolean up, int epoch) {
		super("ROUTE_PAYMENT", new Parameter[]{new StringParameter("SELECTION", ps.getName()), new IntParameter("TRIALS", trials),
				new BooleanParameter("UPDATE", up), new StringParameter("DISTANCE", ps.getDist().name), 
				new IntParameter("EPOCH", epoch)});
		this.trials = trials;		
		this.update = up;
		this.select = ps; 
		this.recompute_epoch = epoch; 
	}
	
	public RoutePayment(PathSelection ps, int trials, boolean up, int epoch, Parameter[] params) {
		super("ROUTE_PAYMENT", extendParams(ps.getName(), trials, up, ps.getDist().name, epoch, params));
		this.trials = trials;		
		this.update = up;
		this.select = ps; 	
		this.recompute_epoch = epoch; 
	}
	
	public RoutePayment(PathSelection ps, int trials, boolean up, Parameter[] params) {
		this(ps, trials, up, Integer.MAX_VALUE, params); 		
	}
	
	public static Parameter[] extendParams(String selName, int trials, boolean up, String distName, int epoch, Parameter[] params) {
		Parameter[] nparams = new Parameter[params.length + 5];
		nparams[0] = new IntParameter("TRIALS", trials);
		nparams[1] = new BooleanParameter("UPDATE", up);
		nparams[2] = new StringParameter("DISTANCE", distName);
		nparams[3] = new StringParameter("SELECTION", selName);
		nparams[4] = new IntParameter("EPOCH", epoch);
		for (int i = 0; i < params.length; i++) {
			nparams[i+5] = params[i]; 
		}
		return nparams;
	}
	
	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		//init values
		this.preprocess(g);
		Node[] nodes = g.getNodes();
		this.run(g); 
		this.postprocess();	
	}
	
    public void run(Graph g) {
    	Node[] nodes = g.getNodes();
		for (int i = 0; i < this.transactions.length; i++) {
			Transaction tr = this.transactions[i];
			int src = tr.getSrc();
			int dst = tr.getDst();
			if (log) System.out.println("Src-dst " + src + "," + dst); 
			double val = tr.getVal();
			boolean s = true; //successful
	    	int h = 0; //hops
	    	int x = 0; //messages
	    	int maxhops = this.select.getDist().getTimeLock(src, dst);
					    
		//attempt at most trials times
		    for (int t = 0; t < this.trials; t++) {
		//set initial set of current nodes and partial payment values to (src, totalVal) 
		    	Vector<PartialPath> pps = new  Vector<PartialPath>();
		    	double[] splitVal = this.splitRealities(val, select.getDist().getStartR(), rand);
		    	for (int a = 0; a < select.getDist().getStartR(); a++) {
		    	     pps.add(new PartialPath(src, splitVal[a],new Vector<Integer>(),a));
		        }
		    	boolean[] excluded = new boolean[nodes.length];
		    	Vector<PartialPath> finalPaths = new  Vector<PartialPath>();
		    	
		    	HashMap<Edge, Double> originalWeight = new HashMap<Edge,Double>(); //updated weights
		
		       //while current set of nodes is not empty 
		    	while (!pps.isEmpty() && h < maxhops) {
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
		            	
		            	if (log) System.out.println("Routing at cur " + cur); 
		                double[] partVals = this.select.getNextsVals(g, cur, dst, 
		                		pre, excluded, this, pp.val, rand, pp.reality); 
		                for (int l = 0; l < past.size(); l++) {
		            		excluded[past.get(l)] = false;
		            	}
		               
		                //add neighbors that are not the dest to new set of current nodes 
		                if (partVals != null) {
		                	past.add(cur);
		                	int[] out = nodes[cur].getOutgoingEdges();
		                	int zeros = 0; //delay -> stay at same node 
		                	for (int k = 0; k < partVals.length; k++) {
		                		if (partVals[k] > 0) {
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
		                			if (out[k] != dst) {
		                				next.add(new PartialPath(out[k], partVals[k], 
		                						(Vector<Integer>)past.clone(),pp.reality));
		                			} else {
		                				finalPaths.add(new PartialPath(out[k], partVals[k], 
		                						(Vector<Integer>)past.clone(),pp.reality));
		                			}
		                			if (log) {
		        		    			System.out.println("add link (" + cur + "," + out[k] + ") with val "+partVals[k]);
		        		    		}
		                		} else {
		                			zeros++;
		                		}
		                	}
		                	if (zeros == partVals.length) {
		                		//stay at node itself 
		                		next.add(new PartialPath(cur, pp.val, 
                						(Vector<Integer>)past.clone(),pp.reality));
		                	}
		                } else {
		                	finalPaths.add(pp); 
		                	//failure to split
		                	if (log) {
		                		System.out.println("fail");
		                		//throw new IllegalArgumentException();
		                	}
		                	s = false;
		                	//break;
		                }
		            }
		            pps = this.merge(next); 
		            h++;
		            
		            //reached maxhop count -> fail
		            if (h == maxhops && !pps.isEmpty()) {
		            	s = false; 
		            }
		            
		            
		    	}
		    	
		    	this.transPostprocess(finalPaths, s, h, x, i, t, originalWeight);
		    }
		    
		    //recompute spanning trees
		    if (this.recompute_epoch != Integer.MAX_VALUE && (i+1) % this.recompute_epoch == 0) {
		    	this.select.initRoutingInfo(g, rand);
		    }
		}
	}
	

	
	
	@Override
	public boolean writeData(String folder) {
		boolean succ = true;
		succ &= DataWriter.writeWithIndex(this.messageDistribution.getDistribution(),
				this.key+"_MESSAGES", folder);
		succ &= DataWriter.writeWithIndex(this.messageDistributionSucc.getDistribution(),
				this.key+"_MESSAGES_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.hopDistribution.getDistribution(),
				this.key+"_HOPS", folder);
		succ &= DataWriter.writeWithIndex(this.hopDistributionSucc.getDistribution(),
				this.key+"_HOPS_SUCC", folder);
		succ &= DataWriter.writeWithIndex(this.trysDistribution.getDistribution(),
				this.key+"_TRYS", folder);
		succ &= DataWriter.writeWithIndex(this.succTime, this.key+"_SUCCESS_TEMPORAL", folder);
		succ &= DataWriter.writeWithIndex(this.slidingSuccess, this.key+"_SUCCESS_SLIDING", folder);
		
		return succ;
	}

	@Override
	public Single[] getSingles() {
		Single m_av = new Single(this.key + "_MES_AV", this.avMess);
		Single m_av_succ = new Single(this.key + "_MES_AV_SUCC", this.avMessSucc);
		Single h_av = new Single(this.key + "_HOPS_AV", this.avHops);
		Single h_av_succ = new Single(this.key + "_HOPS_AV_SUCC", this.avHopsSucc);
		
		Single s1 = new Single(this.key + "_SUCCESS_DIRECT", this.successFirst);
		Single s = new Single(this.key + "_SUCCESS", this.success);

		return new Single[]{m_av, m_av_succ, h_av, h_av_succ, s1, s};
	}
	

	
	protected long[] inc(long[] values, int index) {
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
	
	protected void weightUpdate(CreditLinks edgeweights, HashMap<Edge, Double> updateWeight){
		Iterator<Entry<Edge, Double>> it = updateWeight.entrySet().iterator();
		while (it.hasNext()){
			Entry<Edge, Double> entry = it.next();
			edgeweights.setWeight(entry.getKey(), entry.getValue());
		}
	}
	
	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("CREDIT_LINKS") && g.hasProperty("TRANSACTION_LIST");
	}
	
	protected double[] splitRealities(double val, int r, Random rand) {
		double[] res = new double[r];
		for (int i = 0; i < r-1; i++) {
			res[i] = rand.nextDouble()*val;
		}
		res[res.length-1] = val;
		Arrays.sort(res);
		for (int i = r-1; i > 0; i--) {
			res[i] = res[i] -res[i-1];
		}
		return res;
	}
	
	/**
	 * merge all requests arriving at a node 
	 * @param unmerged
	 * @return
	 */
	protected Vector<PartialPath> merge(Vector<PartialPath> unmerged){
		Vector<PartialPath> vec = new Vector<PartialPath>();
		HashMap<Integer, HashSet<Integer>> dealtWith = new HashMap<Integer, HashSet<Integer>>();
		for (int i = 0; i < unmerged.size(); i++) {
			PartialPath p = unmerged.get(i);
			int node = p.node;
			int r = p.reality;
			HashSet<Integer> dealt = dealtWith.get(r);
			if (dealt == null) {
				dealt = new HashSet<Integer>();
				dealtWith.put(r, dealt);
			}
			if (!dealt.contains(node)) {
				dealt.add(node); 
				Vector<Integer> contained = p.pre;
				double valSum = p.val;
				for (int j = i+1; j < unmerged.size(); j++) {
					PartialPath m = unmerged.get(j); 
					if (m.node == node && m.reality == r) {
						Vector<Integer> toAdd = m.pre;
						//start at 1, because 0 is the same for all paths
						for (int l = 1; l < toAdd.size(); l++) {
							int cur = toAdd.get(l);
							if (!contained.contains(cur)) {
								contained.add(cur);
							}
						}
						valSum = valSum + m.val;
						if (log) {
							System.out.println("Merge at " + node + " new val " + valSum);
						}
					}
				}
				vec.add(new PartialPath(node, valSum, contained,r)); 
			}
		}
		return vec; 
	}
	
	/**
	 * get available funds of link (s,t) 
	 * for atomic non-concurrent payment: partial payment goes through iff all payments go through,
	 * hence consider other operations on link as if they succeed
	 * OVERRIDE FOR OTHER PAYMENTS  
	 * @param s
	 * @param t
	 * @return
	 */
	public double computePotential(int s, int t) {
		return this.edgeweights.getPot(s, t);
	}
	
	/**
	 * return total capacity of a channel 
	 * @param s
	 * @param t
	 * @return
	 */
	public double getTotalCapacity(int s, int t) {
		return this.edgeweights.getPot(s, t)+this.edgeweights.getPot(t, s); 
	}
	
	/**
	 * preprocessing 
	 */
	public void preprocess(Graph g) {
		rand = new Random();
		this.select.initRoutingInfo(g, rand);
		edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		this.transactions = ((TransactionList)g.getProperty("TRANSACTION_LIST")).getTransactions();
		originalAll = new HashMap<Edge,Double>();
		
		this.avHops = 0;
		this.avHopsSucc = 0; 
		this.avMess = 0;
		this.avMessSucc = 0; 
		this.successFirst = 0;
		this.success = 0;
		this.trys = new long[2];
		this.path = new long[2];
		this.pathSucc = new long[2];
		this.mes = new long[2];
		this.mesSucc = new long[2];
		int len = this.transactions.length/this.tInterval; 
		int rest = this.transactions.length % this.tInterval; 
		if (rest == 0) {
		    this.succTime = new double[len];
		} else {
			this.succTime = new double[len+1];
		}
		int win = Math.min(this.window, this.transactions.length); 
		 this.slidingSuccess = new double[this.transactions.length-win+1];
		 this.windowRes = new boolean[transactions.length]; 
	}
	
	
	
	/**
	 * postprocessing specific to child classes for whole run 
	 */
	public void postprocess() {
		//compute final stats
				this.hopDistribution = new Distribution(path,this.transactions.length);
				this.messageDistribution = new Distribution(mes,this.transactions.length);
				this.hopDistributionSucc = new Distribution(pathSucc,(int)this.success);
				this.messageDistributionSucc = new Distribution(mesSucc,(int)this.success);
				this.trysDistribution = new Distribution(trys,this.transactions.length);
				this.avHops = this.hopDistribution.getAverage();
				this.avHopsSucc = this.hopDistributionSucc.getAverage();
				this.avMess = this.messageDistribution.getAverage();
				this.avMessSucc = this.messageDistributionSucc.getAverage();
				this.success = this.success/this.transactions.length;
				this.successFirst = this.successFirst/this.transactions.length;
				for (int i = 0; i < this.succTime.length-1; i++) {
					this.succTime[i] = this.succTime[i]/(double)this.tInterval;
				}
				int rest = this.transactions.length % this.tInterval;
				if (rest > 0) {
				   this.succTime[this.succTime.length-1] = this.succTime[this.succTime.length-1]/(double)rest;
				} else {
					this.succTime[this.succTime.length-1] = this.succTime[this.succTime.length-1]/(double)this.tInterval;	
				}
				int windowS = 0; 
				int win = Math.min(this.window, this.transactions.length);
				for (int i = 0; i < win; i++) {
					if (this.windowRes[i]) {
						windowS++;
					} 
				}
				for (int i = 0; i < this.slidingSuccess.length-1; i++) {
					this.slidingSuccess[i] = windowS/(double)win;
					if (this.windowRes[i]) {
						windowS--;
					}	
                    if (this.windowRes[i+this.window]) {
						windowS++;
					}
				}
				this.slidingSuccess[this.slidingSuccess.length-1] = windowS/(double)win;
				
				//reset weights for further metrics using them 
				if (this.update) {
					this.weightUpdate(edgeweights, originalAll);
				}
	}
	
	/**
	 * postprocessing specific to child classes for one transaction  
	 */
	public void transPostprocess(Vector<PartialPath> finalP, boolean s, int h, int x, int i, int t,
			 HashMap<Edge,Double> originalWeight) {
		this.select.clear();
    	if (!s) {
    		//payments were not made -> return to original weights
    		this.weightUpdate(edgeweights,originalWeight);
    	} else {
    		if (!this.update) {
    			//return credit links to original state
    			this.weightUpdate(edgeweights,originalWeight);
    		}
    	}
    	transStats(s,h,x,i,t); 
	}
	
	public void transStats(boolean s, int h, int x, int i, int t) {
    	int slot = i/this.tInterval; 
		if (!s) {
    		h--; 
    		if (log) {
    			System.out.println("Failure");
    		}
    	} else {
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
    	
    	//sliding window
    	this.windowRes[i] = s; 
	}
	
	
    
}



