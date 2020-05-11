package treeembedding.treerouting;

import java.util.LinkedList;
import java.util.Vector;

import gtna.graph.Node;
import treeembedding.credit.CreditLinks;

public class TreerouteLookahead extends TreerouteNH{
	int ahead;
	
	public TreerouteLookahead() {
		super("TREE_ROUTE_LOOKAHEAD");
	}
	
	public TreerouteLookahead(int trials) {
		super("TREE_ROUTE_LOOKAHEAD",trials);
	}
	
	public TreerouteLookahead(int trials, int trees, int t) {
		super("TREE_ROUTE_LOOKAHEAD", trials, trees,t);
	}
	
	@Override
	protected int nextHop(int cur, Node[] nodes, int[] destID, int dest) {
		int[] out = nodes[cur].getIncomingEdges();
		double dbest = this.dist(cur, cur, dest);
		Vector<Integer> closest = new Vector<Integer>();
		Vector<Integer> seconds = new Vector<Integer>();
		for (int i = 0; i < out.length; i++){
			double dcur = this.dist(cur, out[i], dest)-0.1;
			//if (dcur > 0) {
			//	dcur = dbest + 1;
			//}
			//check neighbors
			int[] outNeigh = nodes[out[i]].getIncomingEdges();
			int next = -1;
			for (int j = 0; j < outNeigh.length; j++) {
				if (outNeigh[j] != cur) {
					double d = this.dist(cur, out[i], dest);
					if (d < dcur) {
						dcur = d;
						next = outNeigh[j];
					}
				}
			}
			if (dcur <= dbest){
				if (closest.size() == 0 && dcur == dbest) continue;
				if (dcur < dbest){
					dbest = dcur;
				   closest = new Vector<Integer>();
				   seconds = new Vector<Integer>();
				}
				closest.add(out[i]);
				seconds.add(next);
			}
		}
		int index;
		if (closest.size() == 0){
			index = -1;
		} else {
			int r = rand.nextInt(closest.size());
			index = closest.get(r);
			this.ahead = seconds.get(r);
		}
		return index;
	}

	@Override
	protected int nextHop(int cur, Node[] nodes, int[] destID, int dest,
			boolean[] exclude, int pre) {
		int[] out = nodes[cur].getIncomingEdges();
		double dbest = this.dist(cur, cur, dest);
		Vector<Integer> closest = new Vector<Integer>();
		Vector<Integer> seconds = new Vector<Integer>();
		for (int i = 0; i < out.length; i++){
			if (!exclude[out[i]] && pre != out[i]){
				double dcur = this.dist(cur, out[i], dest)-0.1;
				//if (dcur > 0) {
				//	dcur = dbest + 1;
				//}
				//check neighbors
				int[] outNeigh = nodes[out[i]].getIncomingEdges();
				int next = -1;
				for (int j = 0; j < outNeigh.length; j++) {
					if (outNeigh[j] != cur & !exclude[outNeigh[j]] && pre != outNeigh[j]) {
						double d = this.dist(cur, out[i], dest);
						if (d < dcur) {
							dcur = d;
							next = outNeigh[j];
						}
					}
				}
				if (dcur <= dbest){
					if (closest.size() == 0 && dcur == dbest) continue;
					if (dcur < dbest){
						dbest = dcur;
					   closest = new Vector<Integer>();
					   seconds = new Vector<Integer>();
					}
					closest.add(out[i]);
					seconds.add(next);
				}
			}
		}
		int index;
		if (closest.size() == 0){
			index = -1;
		} else {
			int r = rand.nextInt(closest.size());
			index = closest.get(r);
			this.ahead = seconds.get(r);
		}
		return index;
	}

	@Override
	protected double dist(int node, int neighbor, int dest) {
		int[] a = this.coords[neighbor];
		int[] b = this.coords[dest];
		int cpl = 0;
		int depth = sp.getDepth(neighbor);
		while (cpl < depth && cpl < b.length && a[cpl] == b[cpl]){
			cpl++;
		}
		return depth+b.length-2*cpl;
	}
	
	@Override
	protected LinkedList<Integer> nextHopsWeight(CreditLinks edgeWeights, int cur, 
			Node[] nodes, int[] destID, int dest, boolean[] exclude, int pre, double weight){
		LinkedList<Integer> list = new LinkedList<Integer>();
		LinkedList<Integer> listall = new LinkedList<Integer>();
		int add = this.nextHop(cur, nodes, destID, dest, exclude, pre);
		while (add != -1){
			if (edgeWeights.getPot(cur, add) >= weight - 0.0000001){
				list.add(add);
			} 
			if (this.ahead != -1) {
			    listall.add(this.ahead);
			    exclude[this.ahead] = true;
			} else {
				listall.add(add);
			    exclude[add] = true;
			}			
			add = this.nextHop(cur, nodes, destID, dest, exclude, pre);
		}
		for (int i = 0; i < listall.size(); i++){
			exclude[listall.get(i)] = false;
		}
		this.ahead = -1;
		return list;
	}

}
