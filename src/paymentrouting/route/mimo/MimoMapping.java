package paymentrouting.route.mimo;

import java.util.Arrays;
import java.util.HashMap;

import gtna.graph.GraphProperty;
import gtna.io.Filereader;
import gtna.io.Filewriter;

public class MimoMapping extends GraphProperty {
	HashMap<Integer, int[]> segmentMapping; //maps an original transaction to the orbits it has splits in 
	
	public int[] getSegmentIds(int tx) {
		return this.segmentMapping.get(tx);  
	}

	@Override
	public boolean write(String filename, String key) {
		Filewriter fw = new Filewriter(filename);
        this.writeHeader(fw, this.getClass(), key);
		
		int i = 0;
		int[] index = this.segmentMapping.get(i);
		while (index != null) {
			String out = i+"";
			for (int j = 0; j < index.length; j++) {
				out = out + " " + index[j]; 
			}
			fw.writeln(out);
			i++;
			index = this.segmentMapping.get(i);
		}
		return fw.close();
	}

	@Override
	public String read(String filename) {
		Filereader fr = new Filereader(filename);
		String key = this.readHeader(fr);
		
		this.segmentMapping = new HashMap<Integer, int[]>(); 
		
		String line = fr.readLine();
		while (line != null && line.length() > 0) {
			String[] splits = line.split(" ");
			int i = Integer.parseInt(splits[0]); 
			int[] index = new int[splits.length-1];
			for (int j = 0; j < index.length; j++) {
				index[j] = Integer.parseInt(splits[j+1]);
			}
			this.segmentMapping.put(i, index); 
			line = fr.readLine(); 
		}
		fr.close();
		
		return key;
	}

}
