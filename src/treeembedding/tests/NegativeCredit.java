package treeembedding.tests;

import gtna.data.Series;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.util.ReadableFile;
import gtna.util.Config;

import java.util.Random;

import treeembedding.credit.CreditNetwork;
import treeembedding.credit.partioner.NormalDistPartitioner;
import treeembedding.credit.partioner.Partitioner;
import treeembedding.treerouting.Treeroute;
import treeembedding.treerouting.TreerouteTDRAP;

public class NegativeCredit {
	
	public static void main(String[] args) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/negCredit/" );
		int i = Integer.parseInt(args[0]);
		int t = Integer.parseInt(args[1]);
		int a = Integer.parseInt(args[2]);
		int m = Integer.parseInt(args[3]);
		int s = Integer.parseInt(args[4]);
		basic("../credRes/gtna/ripple-lcc.graph", "../credRes/trSet/sampleTr-"+i+".txt", 
				"NegCre-1",a,t,"../credRes/gtna/degOrder-bi.txt",i,m,s);
	}
	
	public static void basic(String graph, String transactions, String name, int tries,
			int trees,String degs, int run, int mean, double sigma){
		Partitioner part = new NormalDistPartitioner(mean, sigma);
		Treeroute voute = new TreerouteTDRAP();
		int[] roots = CreditTests.selectRoots(degs, false, trees, run);
		
		CreditNetwork credit = new CreditNetwork(transactions, name, 1.0, voute,
				true, false, 1.0, part, roots, tries, false);
		Network net = new ReadableFile(name, name, graph ,null);	
		  Series.generate(net, new Metric[]{credit}, run,run);
		
	}

}
