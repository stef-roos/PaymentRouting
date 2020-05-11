package treeembedding.treerouting;

import gtna.graph.Node;

public class TreerouteOnly extends Treeroute {

	public TreerouteOnly() {
		super("TREE_ROUTE_ONLY");
	}
	
	public TreerouteOnly(int trials) {
		super("TREE_ROUTE_ONLY", trials);
	}
	
	public TreerouteOnly(int trials, int trees, int t) {
		super("TREE_ROUTE_ONLY", trials, trees, t);
	}

	@Override
	protected int nextHop(int cur, Node[] nodes, int[] dest, int destN) {
		    int dbest = this.getCPL(dest, this.coords[cur]);
			int index = -1;
			int[] out = sp.getChildren(cur);
			for (int i = 0; i < out.length; i++){
				int cpl = this.getCPL(this.coords[out[i]], dest);
				if (cpl > dbest){
					dbest = cpl;
					index = out[i];
					break;
				}
			}
			if (index == -1){
				index = sp.getParent(cur);
			}
			return index;
	}
	
	private int getCPL(int[] a, int[] b){
		int cpl = 0;
		while (cpl < a.length && cpl < b.length && a[cpl] == b[cpl]){
			cpl++;
		}
		return cpl;
	}

	@Override
	protected int nextHop(int cur, Node[] nodes, int[] dest, int destN,
			boolean[] exclude, int pre) {
		int dbest = this.getCPL(dest, this.coords[cur]);
		int index = -1;
		int[] out = sp.getChildren(cur);
		for (int i = 0; i < out.length; i++){
			if (!exclude[out[i]] && pre != out[i]){
			int cpl = this.getCPL(this.coords[out[i]], dest);
			if (cpl > dbest){
				dbest = cpl;
				index = out[i];
				break;
			}
			}
		}
		if (index == -1){
			index = sp.getParent(cur);
			if (index == -1 || exclude[index] || pre == index){
				index = -1;
			}
		}
		return index;
	}

	@Override
	protected void initRoute() {
		
		
	}
}
