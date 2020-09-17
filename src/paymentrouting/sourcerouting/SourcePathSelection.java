package paymentrouting.sourcerouting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import gtna.graph.Edge;
import gtna.graph.Graph;
import treeembedding.credit.CreditLinks;

public abstract class SourcePathSelection {
	String name;
	HashMap<Edge, Double> originalAll; 
	HashMap<Edge, Double> lastHop; 
	
	public SourcePathSelection(String n){
		this.name = n;
	}
	
	public void init(boolean up) {
		if (up) {
		   originalAll = new HashMap<Edge, Double>();
		}   
	}
	
	public abstract RoutingResult getPaths(CreditLinks edgeweights,Graph g, int src, int dst, double val, int maxtries, boolean up);
	
	public void updateLast(CreditLinks edgeweights) {
		if (lastHop != null) {
			reverseUpdates(edgeweights,this.lastHop);
		} 
	}
	
	public void updateAll(CreditLinks edgeweights) {
		if (originalAll != null) {
			reverseUpdates(edgeweights,this.originalAll);
		} 
	}
	
	
	
	public void reverseUpdates(CreditLinks edgeweights, HashMap<Edge, Double> updateWeight){
		Iterator<Entry<Edge, Double>> it = updateWeight.entrySet().iterator();
		while (it.hasNext()){
			Entry<Edge, Double> entry = it.next();
			edgeweights.setWeight(entry.getKey(), entry.getValue());
		}
	}
	

	public String getName() {
		return this.name; 
	}

}
