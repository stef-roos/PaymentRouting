package treeembedding.tests;

import gtna.data.Series;
import gtna.graph.Graph;
import gtna.io.graphWriter.GtnaGraphWriter;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.util.ReadableFile;
import gtna.util.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

import treeembedding.credit.CreditLinks;
import treeembedding.credit.CreditMaxFlow;
import treeembedding.credit.CreditNetwork;
import treeembedding.credit.partioner.Partitioner;
import treeembedding.credit.partioner.RandomPartitioner;
import treeembedding.treerouting.Treeroute;
import treeembedding.treerouting.TreerouteSilentW;
import treeembedding.treerouting.TreerouteTDRAP;

public class DynamicCTests {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int type = Integer.parseInt(args[0]);
		int run = Integer.parseInt(args[1]);
		int step = Integer.parseInt(args[2]);
		String prefix;
		if (type == 0){
			prefix = "SW";
		}else{
			if (type == 7){
			   prefix = "SM";
			}else {
				prefix = "M";
			}
		}
		String graph, trans, newlinks;
		if(step==0){
			graph =  "gtna/jan2013-lcc-t0.graph";
			trans = "gtna/jan2013-trans-lcc-noself-uniq-1.txt";
			newlinks = "gtna/jan2013-newlinks-lcc-sorted-uniq-t0.txt";
		} else {
			graph = "data/READABLE_FILE_"+prefix+"-P"+step+"-93502/"+run+"/";
			 FilenameFilter fileNameFilter = new FilenameFilter() {
				   
		            @Override
		            public boolean accept(File dir, String name) {
		               if(name.contains("CREDIT_NETWORK") || name.contains("CREDIT_MAX")) {
		                  return true;
		               }
		               return false;
		            }
		         };
			String[] files = (new File(graph)).list(fileNameFilter);
			graph = graph + files[0]+"/graph.txt";
			trans = "gtna/jan2013-trans-lcc-noself-uniq-"+(step+1)+".txt";
			newlinks = "gtna/jan2013-newlinks-lcc-sorted-uniq-t"+(step)+".txt";
		}
		if (type == 0){
			runDynSWSM(new String[] {graph, "SW-P"+(step+1),
			trans ,  newlinks,
			"0", ""+run});
		} else {
			if (type == 7){
			runDynSWSM(new String[] {graph, "SM-P"+(step+1),
					trans ,  newlinks,
					"7", ""+run});
			} else {
				
				runMaxFlow(graph, trans, "M-P"+(step+1), newlinks, 165.55245497208898*1000);
			}
		}
//			runDynSWSM(new String[] {"../data/gtna/jan2013-lcc-t0.graph", "TSM",
//					"../data/gtna/testmf.txt" ,  "../data/gtna/jan2013-newlinks-lcc-sorted-uniq-t0.txt",
//					"7", "0"});
//			runDynSWSM(new String[] {"../data/gtna/jan2013-lcc-t0.graph", "TSW",
//					"../data/gtna/testmf.txt" ,  "../data/gtna/jan2013-newlinks-lcc-sorted-uniq-t0.txt",
//					"0", "0"});

	}
	
	public static void runDynSWSM(String[] args){
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		String graph = args[0];
		String name = args[1];
		String trans = args[2];
		String add = args[3];
		int type = Integer.parseInt(args[4]);
		int i = Integer.parseInt(args[5]);
		
		double epoch = 165.55245497208898*1000;
		Treeroute ra;
		boolean dyn;
		boolean multi;
		if (type == 0){
			ra = new TreerouteSilentW();
			multi = true;
			dyn = false;
		} else {
			ra = new TreerouteTDRAP();
			multi = false;
			dyn = true;
		}
		int max = 1;
		double req = 165.55245497208898*2;
		int[] roots ={64,36,43};
		Partitioner part = new RandomPartitioner();
		
		Network net = new ReadableFile(name, name,graph,null);
		CreditNetwork cred = new CreditNetwork(trans, name, epoch, ra,
				dyn, multi, req, part, roots, max, add);
        Series.generate(net, new Metric[]{cred}, i,i);	
	}
	
	public static void runMaxFlow(String graph, String transList, String name, String links,
			double epoch){
		Config.overwrite("SERIES_GRAPH_WRITE", ""+true);
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("MAIN_DATA_FOLDER", "./data/" );
		CreditMaxFlow m = new CreditMaxFlow(transList, name, 
				0, 0,links, epoch); 
		Network test = new ReadableFile(name, name, graph,null);
		Series.generate(test, new Metric[]{m}, 1);
	}
	
	private static void subLists(int numb){
		try {
			BufferedReader brL = new BufferedReader(new FileReader("../data/gtna/jan2013-newlinks-lcc-sorted-uniq-t0.txt"));
			BufferedReader brT = new BufferedReader(new FileReader("../data/gtna/jan2013-trans-lcc-noself-uniq.txt"));
			BufferedWriter bwL = new BufferedWriter(new FileWriter("../data/gtna/jan2013-newlinks-lcc-sorted-uniq-t1.txt"));
			BufferedWriter bwT = new BufferedWriter(new FileWriter("../data/gtna/jan2013-trans-lcc-noself-uniq-1.txt"));
			int i = 1;
			int count = 0;
			String line;
			while ( (line = brT.readLine()) != null){
				count++;
				bwT.write(line);
				if (count % numb == 0){
					double t = Double.parseDouble(line.split(" ")[0]);
					bwT.flush();
					bwT.close();
					i++;
					System.out.println(i);
					bwT = new BufferedWriter(new FileWriter("../data/gtna/" +
							"jan2013-trans-lcc-noself-uniq-"+i+".txt"));
					
					String lineL;
					boolean done = false;
					while (!done){
						lineL = brL.readLine();
						if (lineL == null){
							done = true;
						} else {
							double tL = Double.parseDouble(lineL.split(" ")[0]);
							if (tL < t){
								bwL.write(lineL);
								bwL.newLine();
							} else {
								done = true;
							}
						}
					}
					bwL.flush();
					bwL.close(); 
					bwL = new BufferedWriter(new FileWriter("../data/gtna/jan2013-newlinks-lcc-sorted-uniq-t"+i+".txt"));
				} else {
					bwT.newLine();
				}
			}
			String lineL;
			while ((lineL = brL.readLine()) != null){
				bwL.write(lineL);
				bwL.newLine();
			}
			bwL.flush();
			bwL.close();
			brT.close();
			brL.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private static void grapht0(){
		Network net = new ReadableFile("G", "G", "../data/gtna/jan2013-lcc.graph", null);
		//CreditNetwork cred = new CreditNetwork("empty.text", "STARTGRAPH", 1,new TreerouteOnly(),false,
		//		false,1,new RandomPartitioner(),new int[]{1,3},1, "../data/gtna/jan2013-newlinks-lcc-sorted-uniq.txt");
		
		Graph g = net.generate();
		CreditLinks edgeweights = (CreditLinks) g.getProperty("CREDIT_LINKS");
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("../data/gtna/jan2013-newlinks-lcc-sorted-uniq.txt"));
			BufferedWriter bw = new BufferedWriter(new FileWriter("../data/gtna/jan2013-newlinks-lcc-sorted-uniq-t0.txt"));
			double t = -1;
			String line;
			while( (line = br.readLine()) != null){
				String[] parts = line.split(" ");
				if (parts.length == 0) continue;
				t = Double.parseDouble(parts[0]);
				int src = Integer.parseInt(parts[1]);
				int dst = Integer.parseInt(parts[2]);
				if (src == dst) continue;
				double val = Double.parseDouble(parts[3]);
				if (t < 0){
					edgeweights.setBound(src, dst, val);
				} else {
					bw.write(line);
					bw.newLine();
				}
			}
			bw.flush();
			bw.close();
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//cred.computeData(g, net, null);
		(new GtnaGraphWriter()).writeWithProperties(g, "../data/gtna/jan2013-lcc-t0.graph");
	}

}
