package paymentrouting.util;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import gtna.algorithms.shortestPaths.BreadthFirstSearch;
import gtna.data.Single;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.DataWriter;
import gtna.metrics.Metric;
import gtna.networks.Network; 

public class BetweennessGreedy extends Metric {
	double[] bet;
	boolean r;

	public BetweennessGreedy(int max, boolean random) {
		super("BETWEENNESS_GREEDY");
		this.bet = new double[max+1]; 
		this.r = random; 
	}

	@Override
	public void computeData(Graph g, Network n, HashMap<String, Metric> m) {
		//preprocess 
		Node[] nodes = g.getNodes(); 
		boolean[][] closest = new boolean[nodes.length][nodes.length]; //pairs where shortest path goes via attacker
		int[][] pathlength = new int[nodes.length][nodes.length]; //original shortest path length
		BreadthFirstSearch bfs = new BreadthFirstSearch();
		for (int i = 0; i < pathlength.length; i++) {
			pathlength[i] = bfs.getShortestPaths(g, i)[0]; 
		}
		//step 1: get first two nodes
		Vector<Integer> neighbors = new Vector<Integer>(); 
		int added = 0; 
		Vector<int[]> novPairs = new Vector<int[]>();
		int n1 = -1;
		int n2 = -1;
		if (this.r) {
			Random rand = new Random();
			n1 = rand.nextInt(10000);
			n2 = rand.nextInt(10000);
			while (n1 == n2) {
				n2 = rand.nextInt(10000);
			}
			for (int k = 0; k < nodes.length; k++) {
				for (int l = k+1; l < nodes.length; l++) {
					int actDist = pathlength[k][l];
					int d1 = pathlength[k][n1] + 2 + pathlength[n2][l]; 
					int d2 = pathlength[k][n2] + 2 + pathlength[n1][l]; 
					if (Math.min(d1,d2) <= actDist) {
						novPairs.add(new int[] {k,l}); 
					} 
				}
			}
			added = novPairs.size(); 
		} else {
		for (int i = 0; i < nodes.length; i++) {
			System.out.println("Starting node " + i); 
			for (int j = i+1; j < nodes.length; j++) {
				Vector<int[]> novPairsTemp = new Vector<int[]>(); 
				for (int k = 0; k < nodes.length; k++) {
					for (int l = k+1; l < nodes.length; l++) {
						int actDist = pathlength[k][l];
						int d1 = pathlength[k][i] + 2 + pathlength[j][l]; 
						int d2 = pathlength[k][j] + 2 + pathlength[i][l]; 
						if (Math.min(d1,d2) <= actDist) {
							novPairsTemp.add(new int[] {k,l}); 
						} 
					}
				}
				//System.out.println(i+ " " + j + " " + novPairsTemp.size()); 
				if (novPairsTemp.size() > added) {
					added = novPairsTemp.size(); 
					novPairs = (Vector<int[]>) novPairsTemp.clone();
					n1 = i;
					n2 = j;
				}
			}
		}
		}
		for (int z = 0; z < novPairs.size(); z++) {
			int[] pair = novPairs.get(z); 
			closest[pair[0]][pair[1]] = true; 
			closest[pair[1]][pair[0]] = true;
		}
		neighbors.add(n1); neighbors.add(n2); 
		
		this.bet[2] = added*2/(double)((nodes.length)*(nodes.length-1)); 
		System.out.println("bet " + this.bet[2]); 
		//step 2: add others 
		for (int x = 3; x < this.bet.length; x++) {
			int addedRound = 0;
			int node = -1; 
			for (int i = 0; i < nodes.length; i++) {
				if (!neighbors.contains(i)) {
					Vector<int[]> novPairsTemp = new Vector<int[]>(); 
					for (int k = 0; k < nodes.length; k++) {
						for (int l = k+1; l < nodes.length; l++) {
							if (!closest[k][l]) {
								int actDist = pathlength[k][l];
								int min = Integer.MAX_VALUE;
								for (int j = 0; j < neighbors.size(); j++) {
									int y = neighbors.get(j);
									int d1 = pathlength[k][i]+2+pathlength[y][l];
									if (d1 < min) min = d1; 
									int d2 = pathlength[k][y]+2+pathlength[i][l];
									if (d2 < min) min = d2; 
								}
								if (min <= actDist) {
									novPairsTemp.add(new int[] {k,l}); 
								}
							}
						}
					}
					//System.out.println(i+ " " + novPairsTemp.size()); 
					if (novPairsTemp.size() > addedRound) {
						addedRound = novPairsTemp.size(); 
						novPairs = (Vector<int[]>) novPairsTemp.clone();
						node = i; 
					}
				}
			}
			for (int z = 0; z < novPairs.size(); z++) {
				int[] pair = novPairs.get(z); 
				closest[pair[0]][pair[1]] = true; 
				closest[pair[1]][pair[0]] = true;
			}
			neighbors.add(node);  
			added = added + addedRound; 
			this.bet[x] = added*2/(double)((nodes.length)*(nodes.length-1));
			System.out.println("bet " + this.bet[x]); 
		}
	}

	@Override
	public boolean writeData(String folder) {
		return DataWriter.writeWithIndex(this.bet,"BETWEENNESS_GREEDY_BETWEENNESS",folder);
	}

	@Override
	public Single[] getSingles() {
		return new Single[] {};
	}

	@Override
	public boolean applicable(Graph g, Network n, HashMap<String, Metric> m) {
		return true;
	}

}
