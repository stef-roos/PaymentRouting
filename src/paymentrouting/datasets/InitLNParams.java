package paymentrouting.datasets;

import java.util.HashMap;
import java.util.Random;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.transformation.Transformation;
import gtna.util.parameter.DoubleParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;
import treeembedding.credit.CreditLinks;

public class InitLNParams extends Transformation {

  public InitLNParams() {
    super("INIT_LN_PARAMS", new Parameter[] {});
  }


  @Override
  public Graph transform(Graph g) {
    //init variables
    Node[] nodes = g.getNodes();
    Random rand = new Random();
    HashMap<Edge, double[]> params = new HashMap<>();
    LNParams links = new LNParams();

    //iterate over links and add
    for (int i = 0; i < nodes.length; i++) {
      for (int out : nodes[i].getOutgoingEdges()) { //todo distributions
        double base = 0.01; // + rand.nextGaussian() * 0.1;
        double rate = 0.0001; // + rand.nextGaussian() * 0.1;
        double delay = 5.0; // + rand.nextGaussian() * 0.1;
        double age = 5.0; // + rand.nextGaussian() * 0.1;
        double lastFailure = 1000; // + rand.nextGaussian() * 0.1;
        double[] param = new double[] {base, rate, delay, age, lastFailure};
        for (int j = 0; j < param.length; j++){
          if (param[j] < 0)
            param[j] = 0;
        }
        params.put(new Edge(i, out), param);
      }
    }
    links.setParams(params);
    g.addProperty("LN_PARAMS", links);

    return g;
  }

  @Override
  public boolean applicable(Graph g) {
    return true;
  }

}