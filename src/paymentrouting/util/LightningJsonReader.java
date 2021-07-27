package paymentrouting.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import gtna.graph.Edge;
import gtna.graph.Edges;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.graphReader.GraphReader;
import paymentrouting.datasets.CapacityList;


public class LightningJsonReader extends GraphReader {
	int count; 

	public LightningJsonReader() {
		super("LIGHTNING");
		
	}

	@Override
	public Graph read(String filename) {
		JSONParser parser = new JSONParser();

	    try {
            Object obj = parser.parse(new FileReader(filename));
	        Object nodes = ((JSONObject) obj).get("nodes");
	        JSONArray nodeList = (JSONArray) nodes;
	        HashMap<String, Integer> indices = new HashMap<String, Integer>();
	        Iterator<JSONObject> it = nodeList.iterator();
	        count = 0;
	        while (it.hasNext()) {
	        	String key = it.next().get("pub_key").toString(); 
	        	indices.put(key,count);
	        	count++; 
	        }
	        Graph graph = new Graph("LIGHTNING", count); 
	        Node[] nodelist = Node.init(count, graph);
	        
	        Object edges = ((JSONObject) obj).get("edges");
	        JSONArray edgeList = (JSONArray) edges;
	        Iterator<JSONObject> itE = edgeList.iterator();
	        //edges and capacities
	        Edges edgesG = new Edges(nodelist, edgeList.size()*2);
	        HashMap<Edge,Integer> cap = new HashMap<Edge,Integer>();
	        while (itE.hasNext()) {
	        	//add edge 
	        	JSONObject edge = itE.next();
	        	String key1 = edge.get("node1_pub").toString(); 
	        	int index1 = indices.get(key1); 
	        	String key2 = edge.get("node2_pub").toString(); 
	        	int index2 = indices.get(key2); 
                edgesG.add(index1, index2); 
                edgesG.add(index2, index1); 
                
                //add capacities
                int v = Integer.parseInt(edge.get("capacity").toString()); 
                Edge e = new Edge(index1, index2); 
                Integer past = cap.get(e);
                if (past != null) {
                	v = v + past; 
                }
                cap.put(e, v);
                cap.put(new Edge(index2, index1), v);
	        }
	        edgesG.fill();
	        graph.setNodes(nodelist);
	        graph.addProperty("CAPACITES", new CapacityList(cap));
	        return graph; 
	        
	    } catch (IOException e) {
	    	e.printStackTrace();
	    } catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int nodes(String filename) {
		JSONParser parser = new JSONParser();
		 Object obj;
		 int c = 0; 
		try {
			obj = parser.parse(new FileReader(filename));
			Object nodes = ((JSONObject) obj).get("nodes");
	        JSONArray nodeList = (JSONArray) nodes;
	       Iterator<JSONObject> it = nodeList.iterator();
	        while (it.hasNext()) {
	        	c++; 
	        }
	    } catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	        
		return c;
	}
}
