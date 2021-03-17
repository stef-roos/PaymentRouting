package paymentrouting.route;

import java.util.Random;

import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;
import gtna.transformation.spanningtree.MultipleSpanningTree;
import paymentrouting.route.DistanceFunction.Timelock;
import treeembedding.treerouting.TreeCoordinates;
import treeembedding.vouteoverlay.Treeembedding;

/**
 * Original SpeedyMurmurs
 * @author mephisto
 *
 */
public class SpeedyMurmurs extends DistanceFunction {
	int[][][] coords; //node coordinates: one vector per tree and node 
	int[][] levels; // node level in tree: one integer per tree and node 

	public SpeedyMurmurs(int t) {
		super("SPEEDYMURMURS_"+t, t);
		this.coords = new int[t][][];
		this.levels = new int[t][];
	}
	

	public SpeedyMurmurs(String key, int t) {
		super(key+"_"+t, t);
		this.coords = new int[t][][];
		this.levels = new int[t][];

    }


	public SpeedyMurmurs(int t, Timelock lockMode) {
		super("SPEEDYMURMURS_"+t+"_"+lockMode.toString(), t, t, lockMode);
		this.coords = new int[t][][];
		this.levels = new int[t][];
	}
	
	public SpeedyMurmurs(int t, Timelock lockMode, int lockval) {
		super("SPEEDYMURMURS_"+t+"_"+lockMode.toString()+"_"+lockval, t, t, lockMode, lockval);
        this.coords = new int[t][][];
		this.levels = new int[t][];
	}

	@Override
	/**
	 * distance is the sum of the levels minus twice the number of initial common elements for tree r
	 */
	public double distance(int a, int b, int r) {
		int[] cA = this.coords[r][a];
		int[] cB = this.coords[r][b];
		//compute common prefix length
		int cpl = 0;
		for (int i = 0; i < levels[r][a]+1; i++) {
			if (cA[i] == cB[i]) {
				cpl++;
			} else {
				break;
			}

		}
		//compute distance 
		return levels[r][a]+levels[r][b]-2*cpl; 
	}

	@Override
	/**
	 * compute spanning trees and coordinates 
	 */
	public void initRouteInfo(Graph g, Random rand) {
		Node[] nodes = g.getNodes();
		//check if there are old trees that can be kept 
		int j = 0; 
		boolean recompute = true;
		while (g.hasProperty("SPANNINGTREE_"+j)) {
			j++;
		}
		if (j == levels.length) {
			//no need to recompute if number of trees correct 
			//(otherwise: recompute as there are algorithms with dependencies between trees) 
			recompute = false; 
		}
		
		if (recompute) {
			j = 0;
			while (g.hasProperty("SPANNINGTREE_" + j)) {
				g.removeProperty("SPANNINGTREE_" + j);
				if (g.hasProperty("TREE_COORDINATES_" + j)) {
					g.removeProperty("TREE_COORDINATES_" + j);
				}
				j++;
			}

			// choose random roots
			int[] roots = new int[this.realities];
			for (int i = 0; i < roots.length; i++) {
				roots[i] = rand.nextInt(nodes.length);
			}

			// embed (padded to 60 but ignore for this study) 
			Treeembedding embed = new Treeembedding("T", 60, roots, MultipleSpanningTree.Direct.TWOPHASE);
			g = embed.transform(g);
		}

		for (int i = 0; i < this.realities; i++) {
			SpanningTree tree = (SpanningTree) g.getProperty("SPANNINGTREE_" + i);
			this.levels[i] = tree.depth;
			this.coords[i] = ((TreeCoordinates) g.getProperty("TREE_COORDINATES_" + i)).getCoords();
		}
		  
	}

	@Override
	public boolean isCloser(int a, int b, int dst, int r) {
		if (this.distance(a,dst,r) < this.distance(b, dst, r)) {
			return true;
		} else {
			return false;
		}
	}

}
