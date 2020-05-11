package treeembedding.tests;

import gtna.data.Series;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.util.ReadableFile;
import gtna.util.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import treeembedding.credit.CreditMaxFlow;
import treeembedding.credit.CreditNetwork;
import treeembedding.credit.partioner.EqualPartitioner;
import treeembedding.credit.partioner.Partitioner;
import treeembedding.credit.partioner.RandomPartitioner;
import treeembedding.treerouting.Treeroute;
import treeembedding.treerouting.TreerouteLookahead;
import treeembedding.treerouting.TreerouteOnly;
import treeembedding.treerouting.TreerouteSilentW;
import treeembedding.treerouting.TreerouteTDRAP;



public class CreditTests {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/static/" );
		String path = "data/";
		//iteration 
		int i = Integer.parseInt(args[0]);
		//number of embeddings 
		int trees = Integer.parseInt(args[1]);
		//configuration in terms of routing algorithm 0-10, see below
		int config = Integer.parseInt(args[2]);
		//file of transactions + graph 
		String transList = path + "finalSets/static/1000.txt";
		String graph = path + "finalSets/static/ripple-lcc.graph";
		//name of experiment; 
		String name = "STATIC";
		//epoch, set to 1000
		double epoch = 1000;
		//time between retries 
		double tl= 2*epoch;
		//partition transaction value randomly 
		Partitioner part = new RandomPartitioner();
		//file with degree information + select highest degree nodes as roots 
		String degFile = path + "finalSets/static/degOrder-bi.txt";
		int[] roots = selectRoots(degFile, false, trees, i);
		//number of attempts 
		int tries = Integer.parseInt(args[3]);
		//no updates 
		boolean up = false; 
		
		
		
		Treeroute sW = new TreerouteSilentW();
		Treeroute voute = new TreerouteTDRAP();
		Treeroute only = new TreerouteOnly();
		Treeroute lookahead = new TreerouteLookahead();
		
		//vary dynRepair, multi, routing algo -> 8 poss + 2 treeonly versions
		CreditNetwork silentW = new CreditNetwork(transList, name, epoch, sW,
						false, true, tl, part, roots, tries,up); //original silent whisper, config=0
		CreditNetwork silentWnoMul = new CreditNetwork(transList, name, epoch, sW,
						false, false, tl, part, roots, tries,up); //remove multi-party, config=1
		CreditNetwork silentWdyn = new CreditNetwork(transList, name, epoch, sW,
						true, true, tl, part, roots, tries,up); //add dynamic repair, config=2
		CreditNetwork silentWdynNoMul = new CreditNetwork(transList, name, epoch, sW,
						true, false, tl, part, roots, tries,up); //dyn+no multi-party, config=3
		CreditNetwork vouteMulnoDyn = new CreditNetwork(transList, name, epoch, voute,
						false, true, tl, part, roots, tries,up); //voute routing 
		                                                         //but multi+periodic repair, config=4
		CreditNetwork voutenoDyn = new CreditNetwork(transList, name, epoch, voute,
						false, false, tl, part, roots, tries,up); //voute without dyn repair, config=5
		CreditNetwork vouteMul = new CreditNetwork(transList, name, epoch, voute,
						true, true, tl, part, roots, tries,up); //voute with multi-party, config=6
		CreditNetwork voutenoMul = new CreditNetwork(transList, name, epoch, voute,
						true, false, tl, part, roots, tries,up); //voute no multi, config=7
		CreditNetwork treeonly1 = new CreditNetwork(transList, name, epoch, only,
				false, true, tl, part, roots, tries,up); //tree-only, multi-party, no dyn repair, config=8
		CreditNetwork treeonly2 = new CreditNetwork(transList, name, epoch, only,
				true, false, tl, part, roots, tries,up); //tree-only, multi-party, no dyn repair, config=9
		CreditNetwork looka = new CreditNetwork(transList, name, epoch, lookahead,
				true, false, tl, part, roots, tries,up); //lookahead, config=10
		
		Metric[] m = new Metric[]{silentW, silentWnoMul, silentWdyn, silentWdynNoMul,
				vouteMulnoDyn, voutenoDyn, vouteMul, voutenoMul, treeonly1, treeonly2, looka};
		String[] com = {"SW-PER-MUL", "SW-PER", "SW-DYN-MUL", "SW-DYN",
				"V-PER-MUL", "V-PER", "V-DYN-MUL", "V-DYN", "TREE-ONLY1", "TREE-ONLY1", "LOOKAHEAD"};
		
		Network network = new ReadableFile(com[config], com[config], graph,null);	
		  Series.generate(network, new Metric[]{m[config]}, i,i);


	}
	
	
	
	
	
	public static void summarize(String transList, String name, double epoch, int i){
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "true");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/RIPPLENov2016/" );
		//String transList = "/home/stef/workspace/data/gtna/transactionsRipple.txt";
		
		
		Partitioner part = new RandomPartitioner();
		Treeroute sW = new TreerouteSilentW();
		Treeroute voute = new TreerouteTDRAP();
		double tl = 2*epoch;
		int tries = 1;
		int[] roots = new int[0];
		
		//metrics: vary dynRepair, multi, routing algo -> 8 poss
		CreditNetwork silentW = new CreditNetwork(transList, name, epoch, sW,
				false, true, tl, part, roots, tries); //original silent whisper
		CreditNetwork silentWnoMul = new CreditNetwork(transList, name, epoch, sW,
				false, false, tl, part, roots, tries); //remove multi-party
		CreditNetwork silentWdyn = new CreditNetwork(transList, name, epoch, sW,
				true, true, tl, part, roots, tries); //add dynamic repair
		CreditNetwork silentWdynNoMul = new CreditNetwork(transList, name, epoch, sW,
				true, false, tl, part, roots, tries); //dyn+no multi-party
		CreditNetwork vouteMulnoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, true, tl, part, roots, tries); //voute routing but multi+periodic repair
		CreditNetwork voutenoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, false, tl, part, roots, tries); //voute without dyn repair
		CreditNetwork vouteMul = new CreditNetwork(transList, name, epoch, voute,
				true, true, tl, part, roots, tries); //voute with multi-party
		CreditNetwork voutenoMul = new CreditNetwork(transList, name, epoch, voute,
				true, false, tl, part, roots, tries); //voute no multi
		
		Metric[] m = new Metric[]{silentW, silentWnoMul, silentWdyn, silentWdynNoMul,
				vouteMulnoDyn, voutenoDyn, vouteMul, voutenoMul};
		String[] com = {"SW-PER-MUL", "SW-PER", "SW-DYN-MUL", "SW-DYN",
				"V-PER-MUL", "V-PER", "V-DYN-MUL", "V-DYN"};
		
		Network test = new ReadableFile(com[i], com[i], "gtna/ripple-lcc.graph",null);	
		  Series.generate(test, new Metric[]{m[i]}, 10);
		  
	}
	
	public static void main2(String[] args){
        String graph = "gtna/ripple-lcc.graph";
		
		Date start = new Date();
		long s = start.getTime();
		System.out.print("start " + s);

        int trees = Integer.parseInt(args[0]);
		boolean random = Boolean.parseBoolean(args[1]);
		String degFile = args[2];
	
		String trans = args[3];
		int type = Integer.parseInt(args[4]);
		String name = args[5];
		int fac = Integer.parseInt(args[6]);
		int it = Integer.parseInt(args[7]);
		Partitioner part;
		if (args[8].equals("rand")){
			part = new RandomPartitioner();
		} else {
			part = new EqualPartitioner();
		}
		int tries = Integer.parseInt(args[9]);
		
		int[] roots = selectRoots(degFile, random, trees, it);
		String rts = "ROOTS";
		for (int j = 0; j < roots.length; j++){
			rts = rts + " " + roots[j];
		}
		System.out.println(rts);
		double av = avDiff(trans);
		runRipple(fac*av, type, it, trans, roots, name,tries, part);
		
		Date end = new Date();
		long e = end.getTime();
		System.out.print("end " + e + " time " + (s-e));
	}
	
	public static int[] selectRoots(String file, boolean random, int trees, int seed){
		try{ 
			int[] roots = new int[trees];
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    int a = 0;
		    Random rand = new Random(seed);
		    while ((line =br.readLine()) != null){
		    	String[] parts = line.split(" ");
		    	int[] cur = new int[parts.length];
		    	for (int j = 0; j < cur.length; j++){
		    		cur[j] = Integer.parseInt(parts[j]);
		    		if (random){
			    		if (cur[j] > a){
			    			a = cur[j];
			    		}
			    	} else {
			    		if (a + cur.length - j <= trees){
			    			roots[a] = cur[j];
			    			a++;
			    		}
			    	}
		    	}
		    	if (!random && a + cur.length > trees){
		    		Set<Integer> set = new HashSet<Integer>();
		    		while (a < trees){
		    			int index = rand.nextInt(cur.length);
		    			if (!set.contains(index)){
		    				roots[a] = cur[index];
		    				set.add(index);
		    				a++;
		    			}
		    		}
		    		 br.close();
		    		return roots;
		    	}
		    }
		    if (random){
		    	for (int i = 0; i < roots.length; i++){
		    		roots[i] = rand.nextInt(a);
		    	}
		    	 br.close();
		    	return roots;
		    }
		   
		}catch (IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static void runSimpleTest(String transList, String name, int tries, boolean up){
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/"+name+"/" );
		Partitioner part = new RandomPartitioner();
		Treeroute sW = new TreerouteSilentW();
		Treeroute voute = new TreerouteTDRAP();
		int epoch = 10;
		int tl = epoch;
		int[] roots = {0,3};
		
		//metrics: vary dynRepair, multi, routing algo -> 8 poss
		CreditNetwork silentW = new CreditNetwork(transList, name, epoch, sW,
				false, true, tl, part, roots, tries,up); //original silent whisper
		CreditNetwork silentWnoMul = new CreditNetwork(transList, name, epoch, sW,
				false, false, tl, part, roots, tries,up); //remove multi-party
		CreditNetwork silentWdyn = new CreditNetwork(transList, name, epoch, sW,
				true, true, tl, part, roots, tries,up); //add dynamic repair
		CreditNetwork silentWdynNoMul = new CreditNetwork(transList, name, epoch, sW,
				true, false, tl, part, roots, tries,up); //dyn+no multi-party
		CreditNetwork vouteMulnoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, true, tl, part, roots, tries,up); //voute routing but multi+periodic repair
		CreditNetwork voutenoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, false, tl, part, roots, tries,up); //voute without dyn repair
		CreditNetwork vouteMul = new CreditNetwork(transList, name, epoch, voute,
				true, true, tl, part, roots, tries,up); //voute with multi-party
		CreditNetwork voutenoMul = new CreditNetwork(transList, name, epoch, voute,
				true, false, tl, part, roots, tries,up); //voute no multi
		
		Metric[] m = new Metric[]{silentW, silentWnoMul, silentWdyn, silentWdynNoMul,
				vouteMulnoDyn, voutenoDyn, vouteMul, voutenoMul};
		String[] com = {"SW-PER-MUL", "SW-PER", "SW-DYN-MUL", "SW-DYN",
				"V-PER-MUL", "V-PER", "V-DYN-MUL", "V-DYN"};
		
		for (int i = 0; i < 8; i++){
			Network test = new ReadableFile(com[i], com[i], "data/tests/basic2.graph",null);	
		  Series.generate(test, new Metric[]{m[i]}, 1,1);
		}
	}
	
//	public static void runSimpleMaxFlow(String transList, String name, int tries){
//		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
//		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
//		Config.overwrite("MAIN_DATA_FOLDER", "./data/"+name+"/" );
//		CreditMaxFlow m = new CreditMaxFlow(transList, name, 
//				10, tries); 
//		Network test = new ReadableFile(name, name, "data/tests/basic.graph",null);
//		Series.generate(test, new Metric[]{m}, 1,1);
//	}	
//	
//	public static void runRippleMaxFlow(String graph, String transList, String name, 
//			boolean up, int run){
//		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
//		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
//		Config.overwrite("MAIN_DATA_FOLDER", "./data/"+name+"/" );
//		CreditMaxFlow m = new CreditMaxFlow(transList, name, 
//				0, 0,up); 
//		Network test = new ReadableFile(name, name, graph,null);
//		Series.generate(test, new Metric[]{m}, run, run);
//	}
	
	public static void runSimpleTest(String transList, String name, int tries, String addLink){
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/"+name+"/" );
		
		
		
		Partitioner part = new RandomPartitioner();
		Treeroute sW = new TreerouteSilentW();
		Treeroute voute = new TreerouteTDRAP();
		int epoch = 10;
		int tl = epoch;
		int[] roots = {0,3};
		
		//metrics: vary dynRepair, multi, routing algo -> 8 poss
		CreditNetwork silentW = new CreditNetwork(transList, name, epoch, sW,
				false, true, tl, part, roots, tries, addLink); //original silent whisper
		CreditNetwork silentWnoMul = new CreditNetwork(transList, name, epoch, sW,
				false, false, tl, part, roots, tries, addLink); //remove multi-party
		CreditNetwork silentWdyn = new CreditNetwork(transList, name, epoch, sW,
				true, true, tl, part, roots, tries, addLink); //add dynamic repair
		CreditNetwork silentWdynNoMul = new CreditNetwork(transList, name, epoch, sW,
				true, false, tl, part, roots, tries, addLink); //dyn+no multi-party
		CreditNetwork vouteMulnoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, true, tl, part, roots, tries, addLink); //voute routing but multi+periodic repair
		CreditNetwork voutenoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, false, tl, part, roots, tries, addLink); //voute without dyn repair
		CreditNetwork vouteMul = new CreditNetwork(transList, name, epoch, voute,
				true, true, tl, part, roots, tries, addLink); //voute with multi-party
		CreditNetwork voutenoMul = new CreditNetwork(transList, name, epoch, voute,
				true, false, tl, part, roots, tries, addLink); //voute no multi
		
		Metric[] m = new Metric[]{silentW, silentWnoMul, silentWdyn, silentWdynNoMul,
				vouteMulnoDyn, voutenoDyn, vouteMul, voutenoMul};
		String[] com = {"SW-PER-MUL", "SW-PER", "SW-DYN-MUL", "SW-DYN",
				"V-PER-MUL", "V-PER", "V-DYN-MUL", "V-DYN"};
		
		for (int i = 0; i < 8; i++){
			Network test = new ReadableFile(com[i], com[i], "data/tests/basic3.graph",null);	
		  Series.generate(test, new Metric[]{m[i]}, 1,1);
		}
	}
	
	
	public static void runRipple(double epoch, int setting, int run, String transList, 
			int[] roots, String name, int tries, Partitioner part
			){
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/RIPPLENov2016/" );
		//String transList = "/home/stef/workspace/data/gtna/transactionsRipple.txt";
		
		
		Treeroute sW = new TreerouteSilentW();
		Treeroute voute = new TreerouteTDRAP();
		double tl = 2*epoch;
		
		
		//metrics: vary dynRepair, multi, routing algo -> 8 poss
		CreditNetwork silentW = new CreditNetwork(transList, name, epoch, sW,
				false, true, tl, part, roots, tries); //original silent whisper
		CreditNetwork silentWnoMul = new CreditNetwork(transList, name, epoch, sW,
				false, false, tl, part, roots, tries); //remove multi-party
		CreditNetwork silentWdyn = new CreditNetwork(transList, name, epoch, sW,
				true, true, tl, part, roots, tries); //add dynamic repair
		CreditNetwork silentWdynNoMul = new CreditNetwork(transList, name, epoch, sW,
				true, false, tl, part, roots, tries); //dyn+no multi-party
		CreditNetwork vouteMulnoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, true, tl, part, roots, tries); //voute routing but multi+periodic repair
		CreditNetwork voutenoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, false, tl, part, roots, tries); //voute without dyn repair
		CreditNetwork vouteMul = new CreditNetwork(transList, name, epoch, voute,
				true, true, tl, part, roots, tries); //voute with multi-party
		CreditNetwork voutenoMul = new CreditNetwork(transList, name, epoch, voute,
				true, false, tl, part, roots, tries); //voute no multi
		
		Metric[] m = new Metric[]{silentW, silentWnoMul, silentWdyn, silentWdynNoMul,
				vouteMulnoDyn, voutenoDyn, vouteMul, voutenoMul};
		String[] com = {"SW-PER-MUL", "SW-PER", "SW-DYN-MUL", "SW-DYN",
				"V-PER-MUL", "V-PER", "V-DYN-MUL", "V-DYN"};
		
		
			Network test = new ReadableFile(com[setting], com[setting], "gtna/ripple-lcc.graph",null);	
		  Series.generate(test, new Metric[]{m[setting]}, run,run);
		
	}
	
	public static void runRippleNoUp(double epoch, int setting, int run, String transList, 
			int[] roots, String name, int tries, Partitioner part, double tl
			){
		Config.overwrite("SERIES_GRAPH_WRITE", ""+false);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/RIPPLENov2016/" );
		//String transList = "/home/stef/workspace/data/gtna/transactionsRipple.txt";
		
		
		Treeroute sW = new TreerouteSilentW();
		Treeroute voute = new TreerouteTDRAP();
		Treeroute canal = new TreerouteOnly();

		
		
		//metrics: vary dynRepair, multi, routing algo -> 8 poss
		CreditNetwork silentW = new CreditNetwork(transList, name, epoch, sW,
				false, true, tl, part, roots, tries, false); //original silent whisper
		CreditNetwork silentWnoMul = new CreditNetwork(transList, name, epoch, sW,
				false, false, tl, part, roots, tries, false); //remove multi-party
		CreditNetwork silentWdyn = new CreditNetwork(transList, name, epoch, sW,
				true, true, tl, part, roots, tries, false); //add dynamic repair
		CreditNetwork silentWdynNoMul = new CreditNetwork(transList, name, epoch, sW,
				true, false, tl, part, roots, tries, false); //dyn+no multi-party
		CreditNetwork vouteMulnoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, true, tl, part, roots, tries, false); //voute routing but multi+periodic repair
		CreditNetwork voutenoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, false, tl, part, roots, tries, false); //voute without dyn repair
		CreditNetwork vouteMul = new CreditNetwork(transList, name, epoch, voute,
				true, true, tl, part, roots, tries, false); //voute with multi-party
		CreditNetwork voutenoMul = new CreditNetwork(transList, name, epoch, voute,
				true, false, tl, part, roots, tries, false); //voute no multi
		
		CreditNetwork canal0 = new CreditNetwork(transList, name, epoch, canal,
				false, true, tl, part, roots, tries, false); //canal routing wth SW
		CreditNetwork canal7 = new CreditNetwork(transList, name, epoch, canal,
				true, false, tl, part, roots, tries, false); //canal routing with SM
		
		Metric[] m = new Metric[]{silentW, silentWnoMul, silentWdyn, silentWdynNoMul,
				vouteMulnoDyn, voutenoDyn, vouteMul, voutenoMul, canal0, canal7};
		String[] com = {"SW-PER-MUL", "SW-PER", "SW-DYN-MUL", "SW-DYN",
				"V-PER-MUL", "V-PER", "V-DYN-MUL", "V-DYN", "CANAL-SW", "CANAL-SM"};
		
		
			Network test = new ReadableFile(com[setting], com[setting], "gtna/ripple-lcc.graph",null);	
		  Series.generate(test, new Metric[]{m[setting]}, run,run);
		
	}
	
	public static void runRipple(String graph, double epoch, int setting, int run, 
			String transList, int[] roots, String name, String add, int tries, Partitioner part){
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/RIPPLEJan2013/" );
		//String transList = "/home/stef/workspace/data/gtna/transactionsRipple.txt";
		
		
		
		Treeroute sW = new TreerouteSilentW();
		Treeroute voute = new TreerouteTDRAP();
		double tl = 2*epoch;
		
		
		//metrics: vary dynRepair, multi, routing algo -> 8 poss
		CreditNetwork silentW = new CreditNetwork(transList, name, epoch, sW,
				false, true, tl, part, roots, tries,add); //original silent whisper
		CreditNetwork silentWnoMul = new CreditNetwork(transList, name, epoch, sW,
				false, false, tl, part, roots, tries,add); //remove multi-party
		CreditNetwork silentWdyn = new CreditNetwork(transList, name, epoch, sW,
				true, true, tl, part, roots, tries,add); //add dynamic repair
		CreditNetwork silentWdynNoMul = new CreditNetwork(transList, name, epoch, sW,
				true, false, tl, part, roots, tries,add); //dyn+no multi-party
		CreditNetwork vouteMulnoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, true, tl, part, roots, tries,add); //voute routing but multi+periodic repair
		CreditNetwork voutenoDyn = new CreditNetwork(transList, name, epoch, voute,
				false, false, tl, part, roots, tries,add); //voute without dyn repair
		CreditNetwork vouteMul = new CreditNetwork(transList, name, epoch, voute,
				true, true, tl, part, roots, tries,add); //voute with multi-party
		CreditNetwork voutenoMul = new CreditNetwork(transList, name, epoch, voute,
				true, false, tl, part, roots, tries,add); //voute no multi
		
		Metric[] m = new Metric[]{silentW, silentWnoMul, silentWdyn, silentWdynNoMul,
				vouteMulnoDyn, voutenoDyn, vouteMul, voutenoMul};
		String[] com = {"SW-PER-MUL", "SW-PER", "SW-DYN-MUL", "SW-DYN",
				"V-PER-MUL", "V-PER", "V-DYN-MUL", "V-DYN"};
		
		
			Network test = new ReadableFile(com[setting], com[setting], graph ,null);	
		  Series.generate(test, new Metric[]{m[setting]}, run,run);
		
	}
	
	public static double avDiff(String file){
		double first = -1;
		double last = -1;
		int c = 0;
		try{ 
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    while ((line =br.readLine()) != null){
		    	String[] parts = line.split(" ");
		    	if (parts.length == 4){
		    		double ta = Double.parseDouble(parts[0]);
		    		if (first != -1){
		    			first = ta;
		    		}
		    		last = ta;
		    		c++;
		    	}
		    }
		    br.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		return (last-first)/c;
	}
	
	private static int countSE(String file, int c1, int c2){
		int c = 0;
		try{ 
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    while ((line =br.readLine()) != null){
		    	String[] parts = line.split(" ");
		    	if (parts.length > Math.max(c1, c2)){
		    		if (parts[c1].equals(parts[c2])){
		    			c++;
		    		}
		    	}
		    }
		    br.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		return c;
	}
	
	
	
	

}
