package paymentrouting.sourcerouting;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Vector;

public class Tests {

	public static void main(String[] args) {
		HashMap<Double, Vector<Integer>> mapFeeNode = new HashMap<Double, Vector<Integer>>();
		Vector<Integer> vec = new Vector<Integer>();
		vec.add(7);
		mapFeeNode.put(0.0, vec); 
		PriorityQueue<Double> pQueue = new PriorityQueue<Double>(); 
		pQueue.add(0.0);
		double a = pQueue.poll();
		Vector<Integer> v = mapFeeNode.get(a);
		System.out.println(v.size()); 

	}

}
