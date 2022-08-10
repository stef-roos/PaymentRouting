package paymentrouting.datasets;

import gtna.graph.Graph;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import gtna.graph.Edge;
import gtna.graph.GraphProperty;
import gtna.io.Filereader;
import gtna.io.Filewriter;

/**
 * based on Dan Andreescu's https://github.com/dandreescu/PaymentRouting/blob/lightning/src/paymentrouting/datasets/LNParams.java
 * 
 *
 */

public class LNParams extends GraphProperty {

  private Map<Edge, double[]> params; // double[]{base, rate, delay, delay, age, lastFailure}



  public LNParams() {
    this.params = new HashMap<Edge, double[]>();
  }

  public void setParams(Map<Edge, double[]> params) {
    this.params = params;
  }

  public Map<Edge, double[]> getParams() {
    return params;
  }

  public double[] getParams(int src, int dst) {
    return params.get(new Edge(src, dst));
  }

  public double getBase(Edge e) {
    return params.get(e)[0];
  }

  public double getRate(Edge e) {
    return params.get(e)[1];
  }

  public double getDelay(Edge e) {
    return params.get(e)[2];
  }

  public double getAge(Edge e) {
    return params.get(e)[3];
  }

  public double getLastFailure(Edge e) {
    return params.get(e)[4];
  }


  @Override
  public boolean write(String filename, String key) {
    Filewriter fw = new Filewriter(filename);
    this.writeHeader(fw, this.getClass(), key);

    for (Entry<Edge, double[]> entry : this.params.entrySet()) {
      double[] w = entry.getValue();
      String ws = w[0] + " " + w[1] + " " + w[2] + " " + w[3] + " " + w[4];
      fw.writeln(entry.getKey().getSrc() + " " + entry.getKey().getDst() + " " + ws);
    }

    return fw.close();
  }

  @Override
  public String read(String filename) {
    Filereader fr = new Filereader(filename);

    String key = this.readHeader(fr);
    this.params = new HashMap<Edge, double[]>();
    String line = null;
    while ((line = fr.readLine()) != null) {
      String[] parts = line.split(" ");
      if (parts.length < 2) continue;
      Edge e = new Edge(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      double base = Double.parseDouble(parts[2]);
      base /= 1000;//milli
      double rate = Double.parseDouble(parts[3]);
      rate /= 1000000; //per million
      double delay = Double.parseDouble(parts[4]);
      double age = Double.parseDouble(parts[5]);
      double lastFailure = Double.parseDouble(parts[6]);
      this.params.put(e, new double[]{base, rate, delay, age, lastFailure});
    }

    fr.close();

    return key;
  }

  public LNParams rand(Graph g) {
    Random r = new Random();
    this.params = new HashMap<Edge, double[]>();
    for (Edge e: g.getEdges().getEdges()) {
      double base = 0;
      double p = r.nextDouble();
      if (p >= 0.5) {
    	  base = 1; 
      } 
      p = r.nextDouble();
      double rate = 0.000001;
      if (p > 0.25) {
    	  double pdash = (p-0.25)/0.75;
    	  rate = -0.000001 * (4*Math.log(1-pdash)); 
      }
      double delay = 144;
      this.params.put(e, new double[]{base, rate, delay, 0, -1});
    }
    return this;
  }
  
  public double computeFee(Edge e, double val) {
	  double[] pars = this.params.get(e);
	  if (pars == null) {
		  System.out.println(e.toString()); 
	  }
	  return pars[0] + val*pars[1];
  }
  
  public double getFeePart(Edge e, double val) {
	  double[] pars = this.params.get(e);
	  return (val-pars[0])/(1-pars[1]);
  }

}