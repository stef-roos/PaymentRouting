package paymentrouting.datasets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import gtna.graph.Edge;
import gtna.graph.GraphProperty;
import gtna.io.Filereader;
import gtna.io.Filewriter;

public class CapacityList extends GraphProperty {
	HashMap<Edge,Integer> cap;
	
	public CapacityList() {
		
	}
	
	public CapacityList(HashMap<Edge,Integer> capacities) {
		this.cap = capacities; 
	}
	
	

	public HashMap<Edge, Integer> getCap() {
		return cap;
	}



	public void setCap(HashMap<Edge, Integer> cap) {
		this.cap = cap;
	}



	@Override
	public boolean write(String filename, String key) {
		Filewriter fw = new Filewriter(filename);

		this.writeHeader(fw, this.getClass(), key);
		
		Iterator<Entry<Edge, Integer>> it = this.cap.entrySet().iterator();
        while (it.hasNext()){
        	Entry<Edge, Integer> entry = it.next();
        	int w = entry.getValue();
        	fw.writeln(entry.getKey().getSrc()+ " " + entry.getKey().getDst() + " " + w);
        }
		
		return fw.close();
	}

	@Override
	public String read(String filename) {
		Filereader fr = new Filereader(filename);

		String key = this.readHeader(fr);
        this.cap = new HashMap<Edge, Integer>();
		String line = null;
		while ((line = fr.readLine()) != null) {
			String[] parts = line.split(" ");
			if (parts.length < 2) continue;
			Edge e = new Edge(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			int val = Integer.parseInt(parts[2]);
			this.cap.put(e, val);
		}

		fr.close();

		return key;
	}
	

}
