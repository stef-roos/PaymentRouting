package treeembedding.treerouting;

import gtna.graph.GraphProperty;
import gtna.io.Filereader;
import gtna.io.Filewriter;

public class TreeCoordinates extends GraphProperty {
	int[][] coords;
	
	public TreeCoordinates(){
		
	}
	
    public TreeCoordinates(int[][] coords){
		this.coords = coords;
	}

    public int[] getCoord(int node){
    	return this.coords[node];
    }
    
    public int[][] getCoords(){
    	return this.coords;
    }
    
    public void setCoord(int node, int[] coord){
    	this.coords[node]=coord;
    }

	@Override
	public boolean write(String filename, String key) {
		Filewriter fw = new Filewriter(filename);
		this.writeHeader(fw, this.getClass(), key);
		fw.writeln("TREE_SIZE: "+this.coords.length);
		for (int i = 0; i < coords.length; i++){
			String l = i+"";
			for (int j = 0;j < coords[i].length; j++){
				l = l + " " +coords[i][j];
			}
			fw.writeln(l);
		}
		return fw.close();
	}

	@Override
	public String read(String filename) {
		Filereader fr = new Filereader(filename);
		String key = this.readHeader(fr);
		int treesize = Integer.parseInt(fr.readLine().split(": ")[1]);
		this.coords = new int[treesize][];
		for (int i = 0; i < coords.length; i++){
			String[] parts =readString(fr).split(" ");
			coords[i] = new int[parts.length-1];
			for (int j = 1; j < parts.length; j++){
				coords[i][j-1] = Integer.parseInt(parts[j]);
			}
		}
		return key;
	}

}
