package paymentrouting.sourcerouting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Vector;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import treeembedding.credit.CreditLinks;

public class LightningSinglePath extends SourcePathSelection {
	double[][] baseRateFee; 
	

	public LightningSinglePath(String feeFile) {
		super("LIGHTNING_SINGLEPATH");
		readFeeFile(feeFile);  
	}

	@Override
	public RoutingResult getPaths(CreditLinks edgeweights, Graph g, int src, int dst, double val, int maxtries,
			boolean up) {
		HashSet<Edge> excluded = new HashSet<Edge>();
		boolean success = false;
		boolean impossible = false;
		int tries = 0;
		int messages = 0; 
		int hops = 0;
		double fee = 0;
		int[][] path = new int[1][];
		Node[] nodes = g.getNodes(); 
		while (!success && tries < maxtries && !impossible) {
			//execute Dijkstra from receiver 
			DijkstraResult dijk = Dijkstra.dijkstra(edgeweights, nodes, src, dst, val, baseRateFee, excluded);
			success = !(dijk==null); 
			
			//check if impossible or retry
			if (success) {
				//path found but now need to check capacity
				int[] predeccessor = dijk.getPre();
				double[] minCost = dijk.getCosts(); 
				Vector<Integer> potPath = new Vector<Integer>();
				int next = src;
				potPath.add(next);
				while (next != dst && success) {
					int old = next;
					next = predeccessor[next];
					double pot = edgeweights.getPot(old, next);
					if (pot < val + minCost[next]) {
						success = false; 
					}
					potPath.add(next);
				}
				messages = messages + potPath.size();
				hops = hops + potPath.size();
				if (success) {
					path[0] = new int[potPath.size()];
					for (int l = 0; l < potPath.size(); l++) {
						path[0][l] = potPath.get(l);
					}
					fee = minCost[src]; 
				}
			} else {
				impossible = true; 
			}
			tries++; 
		}
		
		return new RoutingResult(path,success, tries,hops, messages, fee);
	}
	
	/**
	 * read file of the form
	 * line 1:  #nodes NR
	 * other lines: nodeNr baseFee feerate
	 * @param file
	 */
	
	private void readFeeFile(String file) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			int nodes = Integer.parseInt(line.split(" ")[1]);
			this.baseRateFee = new double[nodes][2];
			for (int i = 0; i < nodes; i++) {
				String[] parts = br.readLine().split(" ");
				this.baseRateFee[i] = new double[] {Double.parseDouble(parts[1]), Double.parseDouble(parts[2])};
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
