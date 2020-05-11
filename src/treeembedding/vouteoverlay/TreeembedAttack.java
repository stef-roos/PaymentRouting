package treeembedding.vouteoverlay;


import gtna.graph.Edges;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.SpanningTree;
import gtna.transformation.Transformation;
import gtna.transformation.spanningtree.MultipleSpanningTree;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.IntParameter;
import gtna.util.parameter.Parameter;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import treeembedding.treerouting.TreeCoordinates;

public class TreeembedAttack extends Transformation{
	private int padding;
	int trees;
    Random rand;
    String rootSelector;
    double p;
    boolean depth; 
    int edges;
	
	public TreeembedAttack(int pad, int edges,boolean manipulateRoot) {
		this(pad,1,edges,manipulateRoot);
	}
	
	public TreeembedAttack(int pad, int k,int edges, boolean manipulateRoot) {
		this(pad,k,1,false,edges,manipulateRoot);
	}
	
	public TreeembedAttack(int pad, int k,double p, boolean depth, int edges, boolean manipulateRoot) {
		super("TREE_EMBEDDING_ATTACK", new Parameter[]{
				new IntParameter("PAD", pad), new IntParameter("TREES",k), 
				new DoubleParameter("P",p), new BooleanParameter("DEPTH", depth),
				new IntParameter("EDGES", edges), new BooleanParameter("MANI_ROOT",manipulateRoot)});
		
		this.padding = pad;
		this.trees = k;
		rand = new Random();
		if (manipulateRoot){
		  this.rootSelector = "AttRoot";
		} else {
			this.rootSelector = "rand";
		}
		this.p = p;
		this.depth = depth;
		this.edges = edges;
	}
	
//	public static Parameter[] makeParam(String name){
//		
//	}
	@Override
	public Graph transform(Graph g) {
		g = addEdges(g);
		if (this.rootSelector.equals("AttRoot")){
			this.rootSelector = "" + (g.getNodeCount()-1);
		}
		Transformation tbfs = new MultipleSpanningTree(this.rootSelector,this.trees, rand,this.p,this.depth);
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
				//TO DO: random coordinate if parent is attacker
				if (parent == g.getNodeCount()-1){
				   coordinates[n] = this.getAttChildCoord(rand, parentC, child_index);
				} else {
					coordinates[n] = this.getChildCoord(rand, parentC, child_index);	
				}
				child_index++;
                nodequeue.add(n);
			}					
		}
		//pad coordinates 
		if (this.padding > 0){
		   this.padding(coordinates,rand);
		}
		g.addProperty("TREE_COORDINATES_"+i,new TreeCoordinates(coordinates));
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
	
	public int[] getAttChildCoord(Random rand, int[] parent, int index){
		int[] co = new int[parent.length+1];
		for (int i = 0; i < co.length; i++){
		   co[i] = rand.nextInt();
		}
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
	
	private Graph addEdges(Graph g){
		//cpoy nodes + extend nodes by 1 
		Node[] oldN = g.getNodes();
		Node[] newN = new Node[oldN.length+1];
		System.arraycopy(oldN, 0,newN, 0, oldN.length);
		int index = newN.length-1;
		newN[index] = new Node(index,g);
		g.setNodes(newN);
		//copy edgeset + select edges neighbors uniformly at random 
		Edges edges = g.getEdges();
		HashSet<Integer> potNeighs = new HashSet<Integer>();
		for (int j = 0; j < Math.min(this.edges,oldN.length); j++){
			int neigh = rand.nextInt(oldN.length);
			while (potNeighs.contains(neigh)){
				neigh = rand.nextInt(oldN.length);
			}
			edges.add(neigh, index);
			edges.add(index, neigh);
			potNeighs.add(neigh);
		}
		edges.fill();
		return g;
	}
	
	
}
