package treeembedding.treerouting;

public class TreerouteCPLRAP extends TreerouteNH {
	
	public TreerouteCPLRAP() {
		super("TREE_ROUTE_CPLRAP");
	}
	
	public TreerouteCPLRAP(int trials) {
		super("TREE_ROUTE_CPLRAP",trials);
	}
	
	public TreerouteCPLRAP(int trials, int trees, int t) {
		super("TREE_ROUTE_CPLRAP", trials, trees,t);
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
		//System.out.println(node + " " + neighbor + " " + (-cpl-1/(double)(depth+1+b.length)));
		return -cpl-1/(double)(depth+1+b.length);
	}

}
