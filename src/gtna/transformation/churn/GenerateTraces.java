package gtna.transformation.churn;

import gtna.graph.Graph;
import gtna.transformation.Transformation;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

public class GenerateTraces extends Transformation {
	String folder;
	String sep;
	boolean r; 
	double minus;
	int seed;
	String map;

	public GenerateTraces(String name, String folder, String m,String s, boolean random, double minus, int seed) {
		super("GEN_TRACES", new Parameter[]{new StringParameter("DESC", name), new BooleanParameter("RANDOM",random )});
		this.folder = folder;
		this.sep = s;
		this.r = random;
		this.minus = minus;
		this.seed = seed;
		this.map = m;
	}
	
	public GenerateTraces(String name, String folder, String m, String s, boolean random, double minus) {
		this(name,folder,m,s,random,minus, (new Random()).nextInt());
	}

	@Override
	public Graph transform(Graph g) {
		double[][][] traces = new double[g.getNodeCount()][][];
		if (this.map == null){
		String[] files = (new File(folder)).list();
		Random rand = new Random(seed);
		int cur = rand.nextInt(files.length);
		for (int i = 0; i < traces.length; i++){
			traces[i] = getTrace(folder+files[cur]);
			if (this.r){
				cur = rand.nextInt(files.length);
			} else {
				cur = (cur + 1) % files.length;
			}
		}			
		} else {
			try {
				BufferedReader br = new BufferedReader(new FileReader(this.map));
				int i = 0;
				String line;
				while ((line = br.readLine()) != null){
					traces[i] = getTrace(folder+line);
					i++;
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		g.addProperty("TRACES", new Trace(traces));
		return g;
	}
	
	
	private double[][] getTrace(String file){
		Vector<double[]> t = new Vector<double[]>();
		try{
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null){
			String[] parts = line.split(sep);
			if (parts.length > 1){
				t.add(new double[]{Math.max(0, Double.parseDouble(parts[0])-minus), 
						Math.max(0, Double.parseDouble(parts[1])-minus)});
			}
		}
		br.close();
		} catch (IOException e){
			e.printStackTrace();
		}
		double[][] trace = new double[t.size()][];
		for (int j = 0; j < trace.length; j++){
			trace[j] = t.get(j);
		}
		return trace;
	}

	@Override
	public boolean applicable(Graph g) {
		// TODO Auto-generated method stub
		return true;
	}

}
