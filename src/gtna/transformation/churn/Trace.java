package gtna.transformation.churn;

import java.util.Vector;

import gtna.graph.GraphProperty;
import gtna.io.Filereader;
import gtna.io.Filewriter;

public class Trace extends GraphProperty {
	double[][][] traces;
	
	public Trace(double[][][] t){
		this.traces = t;
	}
	
	public Trace(){
	}

	@Override
	public boolean write(String filename, String key) {
		Filewriter fw = new Filewriter(filename);
		this.writeHeader(fw, this.getClass(), key);
		fw.writeln("SIZE: "+this.traces.length);
		for (int i = 0; i < traces.length; i++){
			String l = i+":";
			fw.writeln(l);
			for (int j = 0;j < traces[i].length; j++){
				fw.writeln(traces[i][j][0] + " " + traces[i][j][1]);
			}
		}
		return fw.close();
	}

	@Override
	public String read(String filename) {
		Filereader fr = new Filereader(filename);
        String key = this.readHeader(fr);
		int nodes = Integer.parseInt(fr.readLine().split(": ")[1]);
        this.traces = new double[nodes][][];
        fr.readLine();
        for (int i = 0; i < nodes; i++){
        	Vector<double[]> vec = new Vector<double[]>();
        	String[] parts = (fr.readLine()).split(" ");
        	while (parts.length > 1){
        		double a = Double.parseDouble(parts[0]);
        		double b = Double.parseDouble(parts[1]);
        		vec.add(new double[]{a,b});
        		String l = fr.readLine();
        		if (l != null){
        		parts = l.split(" ");
        		} else  {
        			parts = new String[0];
        		}
        	}
        	traces[i] = new double[vec.size()][2];
        	for (int j = 0; j < traces[i].length; j++){
        		traces[i][j] = vec.get(j);
        	}
        }
		fr.close();
		
		return key;
	}

	public double[][][] getTraces() {
		return traces;
	}

	public void setTraces(double[][][] traces) {
		this.traces = traces;
	}

}
