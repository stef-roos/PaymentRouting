package treeembedding.treerouting;

public class TreerouteCPLPPP  extends TreerouteNH {
	
	public TreerouteCPLPPP() {
		super("TREE_ROUTE_CPLPPP");
	}
	
	public TreerouteCPLPPP(int trials) {
		super("TREE_ROUTE_CPLPPP",trials);
	}
	
	public TreerouteCPLPPP(int trials, int trees, int t) {
		super("TREE_ROUTE_CPLPPP", trials, trees,t);
	}

	@Override
	protected double dist(int node, int neighbor, int dest) {
		int[] a = this.coords[neighbor];
		int[] b = this.coords[dest];
		int[] own = this.coords[node];
		int cpl = 0;
		int depth = sp.getDepth(neighbor);
		while (cpl < depth && cpl < b.length && a[cpl] == b[cpl]){
			if (cpl == 0 || own[cpl-1] == b[cpl-1]){
				   cpl++;
				} else {
					break;
				}
		}
		return -cpl-1/(double)(depth+1+b.length);
	}

}
