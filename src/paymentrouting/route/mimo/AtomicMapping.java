package paymentrouting.route.mimo;

import java.util.Arrays;
import java.util.HashMap;

import gtna.graph.GraphProperty;
import gtna.io.Filereader;
import gtna.io.Filewriter;
import treeembedding.credit.Transaction;

public class AtomicMapping extends GraphProperty {
	protected HashMap<Integer, Integer> atomicsetIndex; //transaction number -> number of atomic orbit/segment -> file contains this mapping  
	protected HashMap<Integer, int[]> atomicSet; //number of orbit -> transactions in orbit (numbers in transaction vector) 
	
	public AtomicMapping() {
		
	}

	@Override
	public boolean write(String filename, String key) {
		Filewriter fw = new Filewriter(filename);
        this.writeHeader(fw, this.getClass(), key);
		
		int i = 0;
		Integer index = this.atomicsetIndex.get(i);
		while (index != null) {
			fw.writeln(i + " " + index);
			i++;
			index = this.atomicsetIndex.get(i);
		}
		return fw.close();
		
	}

	@Override
	public String read(String filename) {
		Filereader fr = new Filereader(filename);
		String key = this.readHeader(fr);
		
		this.atomicsetIndex = new HashMap<Integer, Integer>();
		this.atomicSet = new HashMap<Integer, int[]>();
		
		String line = fr.readLine();
		while (line != null && line.length() > 0) {
			String[] splits = line.split(" ");
			int i = Integer.parseInt(splits[0]); 
			int index = Integer.parseInt(splits[1]); 
			this.atomicsetIndex.put(i, index); 
			
			//reconstruct opposite mapping, orbit -> transactions 
			if (this.atomicSet.containsKey(index)) {
				int[] arr = this.atomicSet.get(index); 
				arr = Arrays.copyOf(arr, arr.length+1);
				arr[arr.length-1] = i; 
				this.atomicSet.put(index, arr);
			} else {
				int[] arr = new int[] {i}; 
				this.atomicSet.put(index, arr);
			}
			line = fr.readLine();
		}
		fr.close();
		
		return key;
	}
	

}
