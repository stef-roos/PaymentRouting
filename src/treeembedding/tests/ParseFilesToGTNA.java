package treeembedding.tests;

import gtna.graph.Edge;
import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.io.graphReader.GtnaGraphReader;
import gtna.io.graphWriter.GtnaGraphWriter;
import gtna.transformation.partition.LargestWeaklyConnectedComponent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import treeembedding.credit.CreditLinks;

public class ParseFilesToGTNA {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        movingAverages("/home/stef/svns/credit/images/SM-succ-norm.txt", 
        		"/home/stef/svns/credit/images/SM-succ-norm-50.txt",
        		50);
        movingAverages("/home/stef/svns/credit/images/SW-succ-norm.txt", "/home/stef/svns/credit/images/SW-succ-norm-50.txt",
        		50);
//		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssX");
//		long unix =  ((long)1357758780)*1000;//1487116359;
//		String dt = dateFormat.format(new Date(unix));
//		System.out.println(dt); 
//		System.out.println(getUnixTimeStamp(dt));
//		System.exit(0);
		
//		String name = "RIPPLEJan29";
//		//String rawgraph = "/home/sroos/workspace/data/trust-set/graph.txt";
//		String graph = "/home/sroos/workspace/data/trust-set/graph.txt";
//		String resGraph = "/home/sroos/workspace/data/gtna/jan2013";
//		String add = "/home/sroos/workspace/data/trust-set/ripple_links_history.txt";
//		String resAdd = "/home/sroos/workspace/data/gtna/jan2013-newlinks";
//		String trans = "/home/sroos/workspace/data/ripple-data-nov7/transactions-in-USD-jan-2013-aug-2016-sorted.txt";
//		String resTrans = "/home/sroos/workspace/data/gtna/jan2013-trans";
//		
//		//add Multi links to one
//		//addMultiLinks(rawgraph, graph);
//		
//		//transform to gtna files
//		HashMap<String, Integer> map = turnGraphs(graph, resGraph+".graph", name, add);
//		long start = toTransList(trans, resTrans+".txt", map);
//		toAddList(add, resAdd+".txt", map, start);
//		
//		//clean up: reduce to giant comp and remove self-transactions
//		reducetoGiantComp(resGraph+".graph", resTrans+".txt", resAdd + ".txt", 
//				resGraph+"-lcc.graph", resTrans+"-lcc.txt", resAdd + "-lcc.txt");
//		removeSE(resTrans+"-lcc.txt", 2, 3 , resTrans+"-lcc-noself.txt");
		
		//compute degree rank lists
//		writeDegs("../data/gtna/jan2013-lcc-t0.graph", true, "../data/gtna/jan2013-degBi.txt");
//		writeDegs("../data/gtna/jan2013-lcc-t0.graph", false, "../data/gtna/jan2013-degUni.txt");
//		writePerEpoch("../data/gtna/jan2013-newlinks-lcc-sorted-uniq-t0.txt",165552.45497208898,
//				"/home/stef/svns/credit/images/setLinkEpoch.txt");
		
	}
	
	private static void movingAverages(String in, String out, int window){
		Vector<Double> vals = new Vector<Double>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(in));
			BufferedWriter bw = new BufferedWriter(new FileWriter(out));
			
			String line;
			double sum = 0;
			int c = 0;
			while ((line= br.readLine())!= null){
				if (line.length() < 2) continue;
				double d = Double.parseDouble(line.split(" ")[1]);
				vals.add(d);
				c++;
				sum = sum + d;
				if (c > window){
					sum = sum - vals.get(c-window);
				} 
				if (c >= window){
					bw.write(c + " " + sum/window);
					bw.newLine();
				}
				
			}
			br.close();
			
			bw.flush();
			bw.close();
		 } catch (IOException e){
				e.printStackTrace();
		}	
	}
	
	private static void divide(String a1, String div, String out){
		try{
			BufferedReader br = new BufferedReader(new FileReader(a1));
			BufferedReader brDiv = new BufferedReader(new FileReader(div));
			BufferedWriter bw = new BufferedWriter(new FileWriter(out));
			
			String line, lineDiv;
			while ((line= br.readLine())!= null && (lineDiv=brDiv.readLine())!= null){
				String[] parts = line.split("	");
				//System.out.println("got" + line);
				String[] partDivs = lineDiv.split("	");
				if (parts.length > 1 && partDivs.length > 1){
					
					
					double d = Double.parseDouble(partDivs[1]);
					double a = Double.parseDouble(parts[1]);
					double q;
					if (d == 0){
						q = 1.0;
					} else {
						q = a/d;
					}
					bw.write(parts[0] + " " + q);
					bw.newLine();
				}
			}
			br.close();
			brDiv.close();
			bw.flush();
			bw.close();
		 } catch (IOException e){
				e.printStackTrace();
		}	
	}
	
	private static void countEdgesPerEpoch(String init, String add, double epoch, String out){
		HashMap<String, double[]> edges = new HashMap<String, double[]>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(init));
			BufferedWriter bw = new BufferedWriter(new FileWriter(out));
			String line;
			int c = 0;
			while ((line = br.readLine())!= null && line.length() > 1){
				String[] parts = line.split(" ");
				if (parts.length == 5){
					double[] a = new double[3];
					for (int i = 2; i < 5; i++){
						double s = Double.parseDouble(parts[i]);
						a[i-2] = s;
					}
					for (int i = 2; i < 5; i++){
						if (a[i-2] != 0){
							c++;
							break;
						}
					}
					edges.put(parts[0]+"-"+parts[1], a);
				}
			}
			br.close();
			bw.write(0 + " " + c);
			
			double t = 0;
			int n = 0;
			br = new BufferedReader(new FileReader(add));
			while ((line = br.readLine())!= null && line.length() > 1){
				String[] parts = line.split(" ");
				if (parts.length == 4){
					t = Double.parseDouble(parts[0]);
					int f = (int)Math.floor(t/epoch);
					for (int j = n; j < f; j++){
						bw.newLine();
						bw.write(j + " " + c);
					}
					n = f;
					
					double[] a = edges.get(parts[1]+ "-"+ parts[2]);
					int index = 0;
					if (a == null){
						a = edges.get(parts[2]+ "-"+ parts[1]);
						index = 2;
					}
					if (a == null){
						System.out.println(edges.size()+ " " + parts[1] + " " + parts[2]);
					}
					double val = Double.parseDouble(parts[3]);
					boolean before = false;
					if (a[0] != 0 || a[2] != 0){
						before = true;
					}
					a[index] = val;
					boolean after = false;
					if (a[0] != 0 || a[2] != 0){
						after = true;
					}
					if (after && !before){
						c++;
					}
					if (!after && before){
						c--;
					}
				}
			}
			br.close();
			bw.flush();
			bw.close();
         } catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private static void writeEpochs(int[][] epochs, String prefix, String postfix, String file){
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for (int i = 1; i < 10; i++){
			BufferedReader br = new BufferedReader(new FileReader(prefix+i+postfix));
			int c = 0;
			String line;
			while ((line = br.readLine())!= null && line.length() > 1){
				if (c >= epochs[i-1][0] && c <= epochs[i-1][1]){
					bw.write(line);
					bw.newLine();
				}
				c++;
			}
			
			
			br.close();
			}
			bw.flush();
			bw.close();
         } catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private static int[][] getEpochs(double epoch){
		int[][] res = new int[9][2];
		int old = -1;
		for (int i = 0; i < 9; i++){
			try{
				BufferedReader br = new BufferedReader(new FileReader("../data/gtna/jan2013-trans-lcc-noself-uniq-"
						+(i+1)+".txt"));
				String line;
				res[i][0] = old;
				String lastline="";
				while ((line = br.readLine())!= null && line.length() > 1){
					lastline = line;
				}
				double val = Double.parseDouble(lastline.split(" ")[0]);
				int b = (int)Math.ceil(val/epoch);
				res[i][1] = b+1;
				old = b;
				br.close();
				
	         } catch (IOException e){
				e.printStackTrace();
			}
		}
		return res;
	}
	
	private static void writePerEpoch(String file, double interval, String out){
		Vector<Integer> cs = countPerEpoch(file, interval);
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(out));
			for (int i = 0; i < cs.size(); i++){
				bw.write((i+1)+ " " + cs.get(i));
				bw.newLine();
			}
			bw.flush();
			bw.close();

		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private static Vector<Integer> countPerEpoch(String file, double interval){
		double next = interval;
		Vector<Integer> cs = new Vector<Integer>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			Vector<String> poss = new Vector<String>();
			int cur = 0;
			while ((line = br.readLine()) != null){
				if (line.length() < 2) continue;
				double d = Double.parseDouble(line.split(" ", 2)[0]);
				if (d < next){
					cur++;
				} else{
					cs.add(cur);
					next = next + interval;
					cur = 1;
					while (next < d){
						cs.add(0);
						next = next + interval;
					}
				}
			}			
			br.close();
			
         } catch (IOException e){
			e.printStackTrace();
		}
		return cs;
	}
	
	private static void selectRandom(String file, int num, String prefix, int per){
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			Vector<String> poss = new Vector<String>();
			while ((line = br.readLine()) != null){
				if (line.length() > 0){
					poss.add(line);
				}
			}			
			br.close();
			
			Random rand = new Random();
			for (int i = 0; i < num; i++){
				BufferedWriter bw = new BufferedWriter(new FileWriter(prefix+"-"+i+".txt"));
				for (int j = 0; j < per; j++){
					bw.write(poss.get(rand.nextInt(poss.size())));
					bw.newLine();
				}
				
				bw.flush();
				bw.close();
			}
         } catch (IOException e){
			e.printStackTrace();
		}	
			
	}
	
	private static void addMultiLinks(String raw, String graph){
		try{
			BufferedReader br = new BufferedReader(new FileReader(raw));
			HashMap<String,double[]> weights = new HashMap<String,double[]>();
			String line;
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				if (parts.length == 5){
					String edge = parts[0] + " " + parts[1];
                    double[] val = weights.get(edge);
                    boolean swap = false;
                    if (val == null){
                    	String oedge = parts[1] + " " + parts[0];
                    	val = weights.get(oedge);
                    	if (val != null){
                    		swap = true;
                    	} else {
                    	   val = new double[3];
                    	   weights.put(edge, val);
                    	}   
                    }
					double low = swap?Double.parseDouble(parts[4]):Double.parseDouble(parts[2]);
					double cur = swap?Double.parseDouble(parts[3]):Double.parseDouble(parts[3]);
					double up = swap?Double.parseDouble(parts[2]):Double.parseDouble(parts[4]);
					val[0] = val[0] + low;
					val[1] = val[1] + cur;
					val[2] = val[2] + up;
				}
			}
			br.close();
			
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(graph));
			Iterator<Entry<String,double[]>> it = weights.entrySet().iterator();
			while (it.hasNext()){
				Entry<String,double[]> entry = it.next();
				double[] val = entry.getValue();
				bw.write(entry.getKey() + " " + val[0] + " " + val[1] + " " + val[2]);
				if (it.hasNext()) bw.newLine();
			}
			bw.newLine();
			
			bw.flush();
			bw.close();
			
			
		} catch (IOException e){
			
		}
	}
	
	private static void reducetoGiantComp(String graph, String trans, String add,
			String nGraph, String nTrans, String nAdd){
		Graph g = (new GtnaGraphReader()).readWithProperties(graph);
		LargestWeaklyConnectedComponent lcc = new LargestWeaklyConnectedComponent();
		//change graph
		Graph nG = lcc.transform(g);
		//change credit links
		CreditLinks cL = (CreditLinks) g.getProperty("CREDIT_LINKS");
		Iterator<Entry<Edge, double[]>> it = cL.getWeights().iterator();
		Map<Edge, double[]> nWeights = new HashMap<Edge, double[]>();
		while (it.hasNext()){
			Entry<Edge, double[]> entry = it.next();
			int src = entry.getKey().getSrc();
			int dst = entry.getKey().getDst();
			int nSrc = lcc.getNewIndex(src);
			int nDst = lcc.getNewIndex(dst);
			if (nSrc != -1 && nDst != -1){
				nWeights.put(new Edge(nSrc, nDst), entry.getValue());
			}
		}
		CreditLinks nCL = new CreditLinks();
		nCL.setWeights(nWeights);
		nG.addProperty("CREDIT_LINKS", nCL);
		(new GtnaGraphWriter()).writeWithProperties(nG, nGraph);
		
		//change transactions
		try{
		 BufferedReader br = new BufferedReader(new FileReader(trans));
		 BufferedWriter bw = new BufferedWriter(new FileWriter(nTrans));
		 String line;
		 while ((line = br.readLine()) != null){
			 String[] parts = line.split(" ");
			 if (parts.length == 4){
				 int src = Integer.parseInt(parts[2]);
				 int dst = Integer.parseInt(parts[3]);
				 int nSrc = lcc.getNewIndex(src);
				 int nDst = lcc.getNewIndex(dst);
				 if (nSrc != -1 && nDst != -1){
						bw.write(parts[0] + " " + parts[1] + " " + nSrc + " " + nDst);
						bw.newLine();
				 }
			 }
		 }
		 br.close();
		 bw.flush();
		 bw.close();
		} catch (IOException e){
			e.printStackTrace();
		}
		
		//change links
		try {
			BufferedReader br = new BufferedReader(new FileReader(add));
			BufferedWriter bw = new BufferedWriter(new FileWriter(nAdd));
			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(" ",4);
				if (parts.length == 4) {
					int src = Integer.parseInt(parts[1]);
					int dst = Integer.parseInt(parts[2]);
					int nSrc = lcc.getNewIndex(src);
					int nDst = lcc.getNewIndex(dst);
					if (nSrc != -1 && nDst != -1) {
						bw.write(parts[0] +  " " + nSrc + " "
								+ nDst + " " + parts[3]);
						bw.newLine();
					}
				}
			}
			br.close();
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static HashMap<String, Integer> turnGraphs(String in, String out, String name){
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		HashMap<Integer, Vector<Integer>> neighbors = new HashMap<Integer, Vector<Integer>>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(in));
			BufferedWriter bw = new BufferedWriter(new FileWriter(out+"_CREDIT_LINKS"));
			bw.write("# Graph Property Class \ntreeembedding.credit.CreditLinks\n# Key \nCREDIT_LINKS");
			String line;
			int c = 0;
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				if (parts.length == 5){
					c++;
					bw.newLine();
					int src = getIndex(parts[0],map);
					int dst = getIndex(parts[1],map);
					addNeigh(src, dst, neighbors);
					addNeigh(dst, src, neighbors);
					double low = -Double.parseDouble(parts[2]);
					double cur = Double.parseDouble(parts[3]);
					double up = Double.parseDouble(parts[4]);
					if (src < dst){
						bw.write(src + " " + dst + " " + low + " " + cur + " " + up);
					} else {
						bw.write(dst + " " + src + " " + -up + " " + -cur + " " + -low);
					}
						
				}
			}
			br.close();
			bw.flush();
			bw.close();
			
			bw = new BufferedWriter(new FileWriter(out));
			bw.write("# Name of the Graph:\ncredit network basic\n# Number of Nodes:\n"+map.size()+"\n# Number of Edges:\n"+2*c+"\n");
            for (int i = 0; i < map.size(); i++){
            	bw.newLine();
            	String l = i+":";
            	Vector<Integer> vec = neighbors.get(i);
            	l = l + vec.get(0);
            	for (int j = 1; j < vec.size(); j++){
            		l = l + ";" + vec.get(j);
            	}
            	bw.write(l);
            }
            bw.flush();
            bw.close();
		} catch (IOException e){
			
		}
		
		return map;
	}
	
	private static HashMap<String, Integer> turnGraphs(String in, String out, String name,
			String addList){
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		HashMap<Integer, Vector<Integer>> neighbors = new HashMap<Integer, Vector<Integer>>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(in));
			BufferedWriter bw = new BufferedWriter(new FileWriter(out+"_CREDIT_LINKS"));
			bw.write("# Graph Property Class \ntreeembedding.credit.CreditLinks\n# Key \nCREDIT_LINKS");
			String line;
			int c = 0;
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				if (parts.length == 5){
					c++;
					bw.newLine();
					int src = getIndex(parts[0],map);
					int dst = getIndex(parts[1],map);
					addNeigh(src, dst, neighbors);
					addNeigh(dst, src, neighbors);
					double low = -Double.parseDouble(parts[2]);
					double cur = Double.parseDouble(parts[3]);
					double up = Double.parseDouble(parts[4]);
					if (src < dst){
						bw.write(src + " " + dst + " " + low + " " + cur + " " + up);
					} else {
						bw.write(dst + " " + src + " " + -up + " " + -cur + " " + -low);
					}
						
				}
			}
			br.close();
			
			
			br = new BufferedReader(new FileReader(addList));
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				if (parts.length == 4){
					c++;
					int src = getIndex(parts[0],map);
					int dst = getIndex(parts[1],map);
					if (addNeigh(src, dst, neighbors)
							&& addNeigh(dst, src, neighbors)) {
						bw.newLine();
						double low = 0;
						double cur = 0;
						double up = 0;
						if (src < dst) {
							bw.write(src + " " + dst + " " + low + " " + cur
									+ " " + up);
						} else {
							bw.write(dst + " " + src + " " + -up + " " + -cur
									+ " " + -low);
						}
					}
						
				}
			}
			br.close();
			bw.flush();
			bw.close();
			
			bw = new BufferedWriter(new FileWriter(out));
			bw.write("# Name of the Graph:\n"+name+"\n# Number of Nodes:\n"+map.size()+"\n# Number of Edges:\n"+2*c+"\n");
            for (int i = 0; i < map.size(); i++){
            	bw.newLine();
            	String l = i+":";
            	Vector<Integer> vec = neighbors.get(i);
            	l = l + vec.get(0);
            	for (int j = 1; j < vec.size(); j++){
            		l = l + ";" + vec.get(j);
            	}
            	bw.write(l);
            }
            bw.flush();
            bw.close();
		} catch (IOException e){
			
		}
		
		return map;
	}
	
	private static boolean addNeigh(int src, int dst, HashMap<Integer, Vector<Integer>> map){
		Vector<Integer> vec = map.get(src);
		if (vec == null){
			vec = new Vector<Integer>();
			map.put(src, vec);
		}
		if (vec.contains(dst)) return false;
		vec.add(dst);
		return true;
	}
	
	private static int getIndex(String name, HashMap<String, Integer> map){
		Integer a = map.get(name);
		if (a == null){
			a = map.size();
			map.put(name, a);
		}
		return a;
	}
	
	private static long toTransList(String in, String out, HashMap<String, Integer> map){
		try{
			BufferedReader br = new BufferedReader(new FileReader(in));
			BufferedWriter bw = new BufferedWriter(new FileWriter(out));
			String line;
			long a = 0;
			int c = 0;
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				if (parts.length == 5){
					if (!map.containsKey(parts[1])){
						c++;
						continue;
					}
					if (!map.containsKey(parts[2])){
						c++;
						continue;
					}
				int src = map.get(parts[1]);
				int dst = map.get(parts[2]);
				long time = Long.parseLong(parts[4]);
				if (a == 0) {
					a = time;
				} else {
					bw.newLine();
				}
				time = time - a;
				bw.write(time + " " + parts[3] + " " + src + " " + dst);
				}
			}
			br.close();
			bw.flush();
			bw.close();
			return a;
		} catch (IOException e){
			e.printStackTrace();
		}
		return 0;
	}
	
	private static int toAddList(String in, String out, HashMap<String, Integer> map, long a){
		try{
			BufferedReader br = new BufferedReader(new FileReader(in));
			BufferedWriter bw = new BufferedWriter(new FileWriter(out));
			long min = Long.MAX_VALUE;
			String minS="";
			String line;
			int c = 0;
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				if (parts.length == 4){
					
					if (!map.containsKey(parts[0])){
						c++;
						continue;
					}
					if (!map.containsKey(parts[1])){
						c++;
						continue;
					}
				int src = map.get(parts[0]);
				int dst = map.get(parts[1]);
				long time = getUnixTimeStamp(parts[3]);
				if (time < min){
					min = time;
					minS = parts[3];
				}
				time = time - a;
				bw.write(time + " " + src + " " + dst + " " + parts[2]);
				bw.newLine();
				}
			}
			br.close();
			bw.flush();
			bw.close();
			System.out.println(min+ " " + minS);
			return c;
		} catch (IOException e){
			e.printStackTrace();
		}
		return 0;
	}
	
	private static long getUnixTimeStamp(String dateString){
		DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ssX");
		Date date;
		try {
			date = dateFormat.parse(dateString);
		    long unixTime = (long)date.getTime()/1000;
		    return unixTime;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	private static int removeSE(String file, int c1, int c2, String res){
		int c = 0;
		try{ 
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    BufferedWriter bw = new BufferedWriter(new FileWriter(res));
		    String line;
		    while ((line =br.readLine()) != null){
		    	String[] parts = line.split(" ");
		    	if (parts.length > Math.max(c1, c2)){
		    		if (!parts[c1].equals(parts[c2])){
		    			bw.write(line);
		    			bw.newLine();
		    		}
		    	}
		    }
		    bw.flush();
		    bw.close();
		    br.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		return c;
	}
	
	private static void writeDegs(String graph, boolean bi, String res){
		Graph g = (new GtnaGraphReader()).readWithProperties(graph);
		CreditLinks ew = (CreditLinks) g.getProperty("CREDIT_LINKS");
		Node[] nodes = g.getNodes();
		int[] conns = new int[nodes.length];
		HashMap<Integer, Vector<Integer>> levels = new HashMap<Integer, Vector<Integer>>();
		for (int i = 0; i < nodes.length; i++){
			int[] out = nodes[i].getOutgoingEdges();
			for (int n: out){
				if (bi && ew.getPot(i, n) > 0 && ew.getPot(n,i) > 0){
					conns[i]++;
				}
				if(!bi) {
					if (ew.getPot(n,i) > 0){
					   conns[i]++;
					}
					if (ew.getPot(i,n) > 0){
						   conns[i]++;
					}
				}
			}
			Vector<Integer> vec = levels.get(conns[i]);
			if (vec == null){
				vec = new Vector<Integer>();
				levels.put(conns[i], vec);
			}
			vec.add(i);
		}
		Arrays.sort(conns);
		
		try{ 
		    BufferedWriter bw = new BufferedWriter(new FileWriter(res));
		    int old = -1;
			for(int i = conns.length-1; i > -1; i--){
				if (conns[i] != old){
					Vector<Integer> vec = levels.get(conns[i]);
					String line = vec.get(0)+"";
					for (int j = 1; j < vec.size(); j++){
						line = line + " " + vec.get(j);
					}
					bw.write(line);
					bw.newLine();
					old = conns[i];
				}
			}
		    bw.flush();
		    bw.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		
	}
	
	

}


