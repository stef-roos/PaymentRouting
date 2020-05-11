package treeembedding.treerouting;

import gtna.graph.Node;

public class TreerouteSilentW extends Treeroute {
	boolean up = true;

	public TreerouteSilentW() {
		super("TREE_ROUTE_SILENTW");
	}
	
	public TreerouteSilentW(int trials) {
		super("TREE_ROUTE_SILENTW", trials);
	}
	
	public TreerouteSilentW(int trials, int trees, int t) {
		super("TREE_ROUTE_SILENTW", trials, trees, t);
	}

	@Override
	protected int nextHop(int cur, Node[] nodes, int[] dest, int destN) {
		int index = -1;
		if (up){
		    index = sp.getParent(cur);
		    if (index == -1){
		    		up = false;
		    }
		 }
		 if (!up){
			int[] out = sp.getChildren(cur);
			int dbest = this.getCPL(dest, this.coords[cur]);
			for (int i = 0; i < out.length; i++) {
				int cpl = this.getCPL(this.coords[out[i]], dest);
				if (cpl > dbest) {
					dbest = cpl;
					index = out[i];
					break;
				}
			}
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
		int index = -1;
		if (up){
		    index = sp.getParent(cur);
		    if (index == -1){
		    		up = false;
		    } else {
		      if (index == pre || exclude[index]){
		    	return -1;
		     }
		    } 
		    
		 }
		 if (!up){
			int[] out = sp.getChildren(cur);
			int dbest = this.getCPL(dest, this.coords[cur]);
			for (int i = 0; i < out.length; i++) {
				if (!exclude[out[i]]){
				  int cpl = this.getCPL(this.coords[out[i]], dest);
				  if (cpl > dbest) {
					dbest = cpl;
					index = out[i];
					break;
				  }
				}
			}
		 }
		return index;
	}

	@Override
	protected void initRoute() {
		up = true;
		
	}
}	