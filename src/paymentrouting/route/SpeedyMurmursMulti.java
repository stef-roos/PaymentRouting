package paymentrouting.route;

import java.util.Random;

import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;
import gtna.transformation.spanningtree.MultipleSpanningTree;
import treeembedding.treerouting.TreeCoordinates;
import treeembedding.vouteoverlay.Treeembedding;

/**
 * InterdimensionalSpeedyMurmurs 
 * @author mephisto
 *
 */
public class SpeedyMurmursMulti extends DistanceFunction {
	int[][][] coords; //node coordinates: one vector per tree and node 
	int[][] levels; // node level in tree: one integer per tree and node
	
	/**
	 * basic constructor without timeout 
	 * @param t
	 */
	public SpeedyMurmursMulti(int t) {
		super("SPEEDYMURMURS_MULTI_"+t, t, 1);
		this.coords = new int[t][][];
		this.levels = new int[t][];
	}
	
	/**
	 * timelockmode included, if mode != CONST, no value needed  
	 * @param t
	 */
	public SpeedyMurmursMulti(int t, Timelock lockMode) {
		super("SPEEDYMURMURS_MULTI_"+t+"_"+lockMode.toString(), t, 1, lockMode);
		this.coords = new int[t][][];
		this.levels = new int[t][];
	}
	
	/**
	 * timelockmode included, can be CONST for this constructor  
	 * @param t
	 */
	public SpeedyMurmursMulti(int t, Timelock lockMode, int lockval) {
		super("SPEEDYMURMURS_MULTI_"+t+"_"+lockMode.toString()+"_"+lockval, t, 1, lockMode, lockval);
		this.coords = new int[t][][];
		this.levels = new int[t][];
	}

	@Override
	/**
	 * distance is the minimal distance for all spanning trees 
	 */
	public double distance(int a, int b, int r) {
		double min = Integer.MAX_VALUE;
		for (int j = 0; j < this.realities; j++) {
			double d = distOne(a,b,j);
			if (d < min) {
				min = d;
			}
		}		
		return min;
	}
	
	/**
	 * distance between a and b in tree j 
	 * same as SpeedyMurmurs.distance()
	 * @param a
	 * @param b
	 * @param j
	 * @return
	 */
	private double distOne(int a, int b, int j) {
		int[] cA = this.coords[j][a];
		int[] cB = this.coords[j][b];
		int cpl = 0;
		for (int i = 0; i < levels[j][a]+1; i++) {
			if (cA[i] == cB[i]) {
				cpl++;
			} else {
				break;
			}
		}
		return levels[j][a]+levels[j][b]-2*cpl; 
		
	}

	@Override
	/**
	 * init spanning trees and coordinates
	 * in contrast to SpeedyMurmurs.initRouteInfo, this one removes old trees always, which is less error-prone but less efficient  
	 */
	public void initRouteInfo(Graph g, Random rand) {
		Node[] nodes = g.getNodes();
		//remove old trees
		int j = 0; 
		while (g.hasProperty("SPANNINGTREE_"+j)) {
			g.removeProperty("SPANNINGTREE_"+j);
			if (g.hasProperty("TREE_COORDINATES_"+j)) {
				g.removeProperty("TREE_COORDINATES_"+j);
			}
			j++; 
		}
		
		//choose roots
		int[] roots = new int[this.realities];
		for (int i = 0; i < roots.length; i++) {
			roots[i] = rand.nextInt(nodes.length);
		}
 		 
		//embed
		Treeembedding embed = new Treeembedding("T",60,roots, 
					 MultipleSpanningTree.Direct.TWOPHASE);
		
			  g = embed.transform(g);
		  
		 for (int i = 0; i < this.realities; i++) {
			 SpanningTree tree = (SpanningTree)g.getProperty("SPANNINGTREE_"+i);
			 this.levels[i] = tree.depth;
			 this.coords[i] = ((TreeCoordinates)g.getProperty("TREE_COORDINATES_"+i)).getCoords();
		 }
		  
	}

	@Override
	/**
	 * isCloser returns true if a is closer than b to dst in at least one tree
	 * note that we can have isCloser(a,b,dst,r) = true AND isCloser(b,a,dst,r) = true
	 * thus, we need loop detection as closer nodes are potential next hops and previous nodes are not automatically excluded 
	 */
	public boolean isCloser(int a, int b, int dst, int r) {
		boolean closer = false;
		for (int j = 0; j < this.realities; j++) {
			double dA = this.distOne(a, dst, j);
			double dB = this.distOne(b, dst, j);
			if (dA < dB) {
				closer = true;
				break;
			}
		}
		return closer; 
	}
	
}