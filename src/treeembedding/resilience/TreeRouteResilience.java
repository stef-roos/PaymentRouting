package treeembedding.resilience;

import gtna.graph.Graph;
import gtna.graph.GraphProperty;
import gtna.graph.sorting.NodeSorter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.util.HashMap;
import java.util.HashSet;

import treeembedding.Util;
import treeembedding.treerouting.Treeroute;

public class TreeRouteResilience extends RoutingResilience {
	private int t;
	private int tr;
	private Treeroute ra;
	private boolean backtrack; 

	public TreeRouteResilience(int tr, Treeroute ra,
			NodeSorter sort, int[] steps, int trials, boolean back) {
		super("TREE_ROUTE_RESILIENCE", new Parameter[]{
				new IntParameter("TREE_ROUTES",tr), 
				new StringParameter("ROUTING_ALGORITHM", ra.getKey()),  new BooleanParameter("BACKTRACK",back)}, sort, steps, trials, 5);
		this.tr = tr;
		this.ra = ra;
		this.backtrack = back;
	}

	@Override
	protected void init(Graph g) {
		GraphProperty[] sp = g.getProperties("SPANNINGTREE");
		this.t = sp.length;

	}

	/**
	 * used failTypes: 3 -> no closest neighbor found
	 */
	@Override
	protected int[] getRouteStats(int src, int dest, Graph g, boolean[] exclude) {
        int h = Integer.MAX_VALUE;
        int m = 0;
        boolean done = false;
        boolean[] tried = new boolean[this.t];
        HashSet<Integer> contained = new HashSet<Integer>();;
        while (!done){
        int[] s = Util.getkOfN(this.t, this.tr, rand,tried);
        for (int l = 0; l < s.length; l++){
			 tried[s[l]] = true;
		 }
        if (s.length == 0) break;
        contained = new HashSet<Integer>();
        boolean first = true;
        for (int i = 0; i < s.length; i++){
        	 HashSet<Integer> containedCur = new HashSet<Integer>();
        	 int[] route;
        	 if (this.backtrack){
        		 route = ra.getRouteBacktrack(src, dest, s[i], g, g.getNodes(),exclude);
        	 }else{
        		 route = ra.getRoute(src, dest, s[i], g, g.getNodes(),exclude);
        	 }
        	if (route[route.length-1] == -1){
        		//failed
        		m = m + 2*(route.length-2);
        	} else {
        		done = true;
        		m = m + route.length-1;
        		for (int j = 1; j < route.length-1; j++){
        			if (first || contained.contains(route[j])){
        				containedCur.add(route[j]);
        			}
        		}
        		contained = containedCur;
        		first = false;
        		if (h > route.length-1){
        			h = route.length-1;
        		}
        	}
        }
        }
        if (h == Integer.MAX_VALUE){
        	h = -1;
        }
		return new int[]{h,m,contained.size(),3};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("TREE_COORDINATES_0");
	}

}
