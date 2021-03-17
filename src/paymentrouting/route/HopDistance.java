package paymentrouting.route;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import gtna.graph.Graph;
import gtna.graph.Node;
import paymentrouting.route.DistanceFunction.Timelock;

/**
 * distance = length of shortest path 
 * @author mephisto
 *
 */

public class HopDistance extends DistanceFunction {
	int[][] distances;

	public HopDistance() {
		super("HOP_DISTANCE", 1);
	}
	
	public HopDistance(Timelock lockMode) {
   	 super("HOP_DISTANCE_"+lockMode, 1,1, lockMode); 
    }
     
     public HopDistance(Timelock lockMode, int lockval) {
    	 super("HOP_DISTANCE_"+lockMode+"_"+lockval, 1,1, lockMode, lockval); 
     }

	@Override
	public double distance(int a, int b, int r) {
		return this.distances[a][b];
	}

	@Override
	public void initRouteInfo(Graph g, Random rand) {
       Node[] nodes = g.getNodes();
       this.distances = new int[nodes.length][nodes.length];
       for (int i = 0; i < nodes.length; i++) {
    	   for (int j = 0; j < nodes.length; j++) {
    		   this.distances[i][j] = -1;
    	   }
       }
       for (int i = 0; i < nodes.length; i++) {
    	   Queue<Integer> queue = new LinkedList<Integer>();
   		queue.add(i);
   		distances[i][i] = 0;
   		while (!queue.isEmpty()) {
   			Node current = nodes[queue.poll()];
   			for (int outIndex : current.getOutgoingEdges()) {
   				if (distances[i][outIndex] != -1) {
   					continue;
   				}
   				distances[i][outIndex] = distances[i][current.getIndex()] + 1;
   				queue.add(outIndex);
   			}
   		}
   		
       }
		
	}

	@Override
	public boolean isCloser(int a, int b, int dst, int r) {
		if (this.distance(a,dst,0) < this.distance(b, dst, 0)) {
			return true;
		} else {
			return false;
		}
	}

}
