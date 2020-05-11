package gtna.transformation.embedding.greedy;

import gtna.graph.GraphProperty;
import gtna.id.prefix.PrefixSPartitionSimple;
import gtna.io.Filereader;
import gtna.io.Filewriter;

public class TopologyAwareKeys extends GraphProperty {
	double[][] top;
	
	public  TopologyAwareKeys(double[][] top){
		this.top = top;
	}
	
	public  TopologyAwareKeys(){
	}

	@Override
	public boolean write(String filename, String key) {
		Filewriter fw = new Filewriter(filename);
		this.writeHeader(fw, this.getClass(), key);
		fw.writeln("TREE_SIZE: "+this.top.length);
		for (int i = 0; i < top.length; i++){
			fw.writeln(i + " " + top[i][0] + " " + top[i][1]);
		}
		return fw.close();
	}

	@Override
	public String read(String filename) {
		Filereader fr = new Filereader(filename);
		String key = this.readHeader(fr);
		int treesize = Integer.parseInt(fr.readLine().split(": ")[1]);
		this.top = new double[treesize][2];
		for (int i = 0; i < top.length; i++){
			String[] parts =readString(fr).split(" ");
			top[i][0] = Double.parseDouble(parts[1]);
			top[i][1] = Double.parseDouble(parts[2]);
		}
		return key;
	}

	public double[][] getTop() {
		return top;
	}

	public void setTop(double[][] top) {
		this.top = top;
	}
	
	

}
