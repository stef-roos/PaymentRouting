package treeembedding.vouteoverlay;


import gtna.graph.Graph;
import gtna.graph.spanningTree.SpanningTree;
import gtna.transformation.Transformation;
import gtna.transformation.spanningtree.MultipleSpanningTree;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import treeembedding.treerouting.TreeCoordinates;

public class Treeembedding extends Transformation{
	private int padding;
	int trees;
    Random rand;
    String rootSelector;
    double p;
    boolean depth; 
    MultipleSpanningTree.Direct dir;
	
	public Treeembedding(String name, int pad) {
		this(name,pad,1);
	}
	
	public Treeembedding(String name, int pad, int k) {
		this(name,pad,k,"bfs",1,false, MultipleSpanningTree.Direct.BOTH);
	}
	
	public Treeembedding(String name, int pad, int[] roots, MultipleSpanningTree.Direct dir) {
		this(name,pad,roots.length,turnSelector(roots),1,false, dir);
	}
	
	private static String turnSelector(int[] r){
		String sel = r[0]+"";
		for (int i = 1; i < r.length; i++){
			sel = sel + "-" + r[i];
		}
		return sel;
	}
	
	public Treeembedding(String name, int pad, int k, String rootSelector, double p, boolean depth, MultipleSpanningTree.Direct dir) {
		super("TREE_EMBEDDING", new Parameter[]{new StringParameter("NAME", name), 
				new IntParameter("PAD", pad), new IntParameter("TREES",k), new StringParameter("ROOT",rootSelector),
				new DoubleParameter("P",p), new BooleanParameter("DEPTH", depth), new StringParameter("PARENT_DIR",dir.name())});
		this.padding = pad;
		this.trees = k;
		rand = new Random();
		this.rootSelector = rootSelector;
		this.p = p;
		this.depth = depth;
		this.dir = dir;
	}
	
//	public static Parameter[] makeParam(String name){
//		
//	}
	@Override
	public Graph transform(Graph g) {
		Transformation tbfs = new 
				MultipleSpanningTree(this.rootSelector,this.trees, rand,this.p,this.depth, this.dir);
		g = tbfs.transform(g);
		for (int i = 0; i < trees; i++){
		SpanningTree st = (SpanningTree) g.getProperty("SPANNINGTREE_"+(i));
		
		int[][] coordinates = new int[g.getNodeCount()][];
		
		int rootIndex = st.getSrc();
		// root has an empty coordinate vector 
		coordinates[rootIndex] = this.getRootCoord(rand);
		
		Queue<Integer> nodequeue = new LinkedList<Integer>();
		nodequeue.add(rootIndex);
		
		// while currentNode has some child node which has not been visited
		while(!nodequeue.isEmpty()){
			int parent = nodequeue.poll(); 
			int[] parentC = coordinates[parent];
			int[] outgoing = st.getChildren(parent);
			// along the spanning tree assign the children indices
			// index of the current child
			int child_index = 0;
			
			for (int n: outgoing){			
				// the new coordinates are based on the coords from the parent plus the additional coordinate
				coordinates[n] = this.getChildCoord(rand, parentC, child_index);
				child_index++;
                nodequeue.add(n);
			}					
		}
		//pad coordinates 
		if (this.padding > 0){
		   this.padding(coordinates,rand);
		}
		g.addProperty(g.getNextKey("TREE_COORDINATES"),new TreeCoordinates(coordinates));
		}
		return g;
	}
	
	public int[] getRootCoord(Random rand){
		return new int[0];
	}
	
	public int[] getChildCoord(Random rand, int[] parent, int index){
		int[] co = new int[parent.length+1];
		System.arraycopy(parent, 0, co, 0, parent.length);
		co[co.length-1] = rand.nextInt();
		return co;
	}
	
	public void padding(int[][] coordinates, Random rand){
		for (int i = 0; i < coordinates.length; i++){
			int[] newC = new int[this.padding];
			System.arraycopy(coordinates[i], 0, newC, 0, coordinates[i].length);
			for (int j = coordinates[i].length; j < newC.length; j++){
				newC[j] = rand.nextInt();
			}
			coordinates[i] = newC;
		}
	}

	@Override
	public boolean applicable(Graph g) {
		return true;
	}
	
	
}
