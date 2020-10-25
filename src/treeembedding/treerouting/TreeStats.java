package treeembedding.treerouting;

import java.util.HashMap;

import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.spanningTree.SpanningTree;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.util.Util;

public class TreeStats extends Metric {
	//distribution: the depth of all nodes (each tree is one row in output) 
	private double[][] depths;
	//distribution: the depth of leaf nodes (each tree is one row in output)
	private double[][] depthsLeaf;
	//distribution: number of children (each tree is one row in output)
	private double[][] children;
	//average depth (per tree, each value one tree)
	private double[] depthAvs;
	//average depth only leaves (per tree, each value one tree)
	private double[] depthAvsLeaf;
	//average children for non-leaf(!!) nodes (per tree, each value one tree)
	private double[] childrenAvs;
	//average Depth over all trees
	private double depthAv; 
	//average Depth over all trees only leaf nodes
	private double depthAvLeaf; 
	//average number  
	private double childrenAv; 
	//number leaves (per tree) 
	private double[] leafCount;

	public TreeStats() {
		super("TREE_STATS");
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		//figure out number of trees
		int index = 1;
		while (g.hasProperty("SPANNINGTREE_"+index)) {
			index++; 
		}
		//get depths, children 
		int nodes = g.getNodeCount();
		int[][] listDepths = new int[index][nodes]; 
		int[][] listChildren = new int[index][nodes]; 
		int maxDep = 0;
		int maxChild = 0; 
		for (int i = 0; i < index; i++) {
			SpanningTree sp = (SpanningTree) g.getProperty("SPANNINGTREE_"+i);
			if (sp == null) {
				//in case there is only one spanning tree without index 
				sp = (SpanningTree) g.getProperty("SPANNINGTREE");
			}
			for (int k = 0; k < nodes; k++) {
				listDepths[i][k] = sp.getDepth(k); 
				if (listDepths[i][k] > maxDep) {
					maxDep = listDepths[i][k]; 
				}
				listChildren[i][k] = sp.getChildren(k).length;
				if (listChildren[i][k] > maxChild) {
					maxChild = listChildren[i][k];
				}
			}
		}
		
		//process info on depths
		this.depthAv = 0;
		this.depthAvLeaf = 0;
		this.depthAvs = new double[index]; 
		this.depthAvsLeaf = new double[index]; 
		this.leafCount = new double[index];
		this.depths = new double[index][maxDep+1]; 
		this.depthsLeaf = new double[index][maxDep+1]; 
		for (int i = 0; i < index; i++) {
			for (int k = 0; k < nodes; k++) {
				this.depths[i][listDepths[i][k]]++;
				this.depthAvs[i] = this.depthAvs[i] + listDepths[i][k]; 
				if (listChildren[i][k] == 0) {
					//leaf
					this.depthsLeaf[i][listDepths[i][k]]++;
					this.depthAvsLeaf[i] = this.depthAvsLeaf[i] + listDepths[i][k]; 
					this.leafCount[i]++;
				}
			}
			this.depthAvs[i] = this.depthAvs[i]/nodes; 
			this.depthAvsLeaf[i] = this.depthAvsLeaf[i]/this.leafCount[i]; 
			for (int l = 0; l < maxDep + 1; l++) {
				this.depths[i][l] = this.depths[i][l]/nodes;
				this.depthsLeaf[i][l] = this.depthsLeaf[i][l]/this.leafCount[i];
			}
		}
		this.depthAv = Util.avg(this.depthAvs); 
		this.depthAvLeaf = Util.avg(this.depthAvsLeaf); 
		
		//get children stats 
		this.childrenAv = 0;
		this.childrenAvs = new double[index]; 
		this.children = new double[index][maxChild+1];  
		for (int i = 0; i < index; i++) {
			for (int k = 0; k < nodes; k++) {
				this.children[i][listChildren[i][k]]++; 
			    if (listChildren[i][k] > 0) {
				    this.childrenAvs[i] = this.childrenAvs[i] + listChildren[i][k]; 
				}    
			}
			this.childrenAvs[i] = this.childrenAvs[i]/(nodes-this.leafCount[i]); 
			for (int l = 0; l < maxChild + 1; l++) {
				this.children[i][l] = this.children[i][l]/nodes;
			}
		}
		this.childrenAv = Util.avg(this.childrenAvs); 
	}

	@Override
	public boolean writeData(String folder) {
		boolean succ = DataWriter.writeWithIndex(this.depthAvs, "TREE_STATS_AV_DEPTH", folder); 
		succ &=  DataWriter.writeWithIndex(this.depthAvsLeaf, "TREE_STATS_AV_DEPTH_LEAF", folder); 
		succ &=  DataWriter.writeWithIndex(this.childrenAvs, "TREE_STATS_AV_CHILDREN", folder); 
		succ &=  DataWriter.writeWithIndex(this.leafCount, "TREE_STATS_LEAF_COUNT", folder); 
		succ &= DataWriter.writeWithoutIndex(this.depths, "TREE_STATS_DEPTH", folder); 
		succ &= DataWriter.writeWithoutIndex(this.depthsLeaf, "TREE_STATS_DEPTH_LEAF", folder); 
		succ &= DataWriter.writeWithoutIndex(this.children, "TREE_STATS_CHILDREN", folder); 
		
		return succ;
	}

	@Override
	public Single[] getSingles() {
		Single d = new Single("TREE_STATS_AV_DEPTH_ALL", this.depthAv); 
		Single dl = new Single("TREE_STATS_AV_DEPTH_LEAF_ALL", this.depthAvLeaf); 
		Single c = new Single("TREE_STATS_AV_CHILDREN_ALL", this.childrenAv); 
		return new Single[] {d, dl, c};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return g.hasProperty("SPANNINGTREE") || g.hasProperty("SPANNINGTREE_0");
	}
	
	
		
	

}
