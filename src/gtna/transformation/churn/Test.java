package gtna.transformation.churn;

import gtna.data.Series;
import gtna.metrics.Metric;
import gtna.networks.Network;
import gtna.networks.model.BarabasiAlbert;
import gtna.networks.model.ErdosRenyi;
import gtna.networks.util.ReadableFile;
import gtna.util.Config;
import gtna.util.GetValuesGTNA;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import treeembedding.stepwise.BalancedCA;
import treeembedding.stepwise.DynamicChord;

public class Test {
	public static int[] seeds = {1583125164, 167186312,1307285313 ,-1231088374 ,-382542884 ,-1257237030 ,-1455592055 ,2063811802 ,1104774121 ,926668970 ,-810138671 ,-1789415668 
		,1250341916 ,-1958071490 ,716943338 ,-1891838502 ,-1420872183 ,-85085430 ,-50353640 ,1309097907};
	public static int ind = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//runSyn(); System.exit(0);
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/originalMaxB", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_BOUND_F_MAX",false, false);
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/virMaxB", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_BOUND_F_MAX",true, false);
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/sjMaxB", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_BOUND_F_MAX",false, true);
		
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/originalMes", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_MES_OVERLL_FRAC",false, false);
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/virMes", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_MES_OVERLL_FRAC",true, false);
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/sjMes", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_MES_OVERLL_FRAC",false, true);
		
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/originalAvF", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_MAX_F_AV",false, false);
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/virAvF", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_MAX_F_AV",true, false);
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/sjAvF", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_MAX_F_AV",false, true);
		
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/originalMaxF", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_MAX_F_MAX",false, false);
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/virMaxF", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_MAX_F_MAX",true, false);
		writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNA/sjMaxF", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_MAX_F_MAX",false, true);
		System.exit(0);
		//writeSummary("/home/stef/svns/darknet-paper/infocom16/images/resGTNAoriginalMes", "data/bca/READABLE_FILE_SPI-9222/", "BALANCED_CA_BOUND_F_MAX",false, false);
//		to do: 1-3, 1-5, 2-5, 3-5
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		Config.overwrite("MAIN_DATA_FOLDER", "data/bca/");
		Network test = new ReadableFile(args[0], args[0], args[1],null);
		int runs = 100000;
//		Metric[] m = new Metric[]{new BalancedCA("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs,1,true,false,
//				Integer.parseInt(args[3]),Double.parseDouble(args[4])),
//				new BalancedCA("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs,1,false,false,
//						Integer.parseInt(args[3]),Double.parseDouble(args[4])),
//						new BalancedCA("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs,1,false,true,
//								Integer.parseInt(args[3]),Double.parseDouble(args[4]))		
//		};
		Metric[] m = new Metric[]{new DynamicChord("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs)};		
		
        Series.generate(test, m, Integer.parseInt(args[5]));
	}
	
	private static void runSyn(){
		Network er1 = new ErdosRenyi(9222, 10.58,true,null);
		Network er2 = new ErdosRenyi(9222, 922.2,true,null);
		Network ba = new BarabasiAlbert(9222,3,null);
		Config.overwrite("MAIN_DATA_FOLDER", "data/bca/");
//		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "false");
		int runs = 100000;
//		for (int i = 0; i < 20; i++){
//			Metric[] m = new Metric[]{new BalancedCA("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs,1, seeds[i], true,false,1,2),
//			new BalancedCA("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs,1,seeds[i], false,false,1,2),
//			new BalancedCA("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs,1, seeds[i],false,true,1,2)};
//			Series.generate(er1, m, i,i);
//			Series.generate(er2, m, i,i);
//			Series.generate(ba, m, i,i);
//		}	
		Config.overwrite("SKIP_EXISTING_DATA_FOLDERS", "true");
		Metric[] m = new Metric[]{new BalancedCA("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs,1,  true,false,1,2),
				new BalancedCA("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs,1,false,false,1,2),
				new BalancedCA("FN_Model", "sessDist/sess.txt", "sessDist/inter.txt",  " ",runs,1, false,true,1,2)};
		Series.generate(er1, m, 20);
		Series.generate(er2, m, 20);
		Series.generate(ba, m, 20);
		}
	
	public static void writeSummary(String result, String folder, String metric, boolean vir, boolean sj){
		try {
			
		int[] cs = {1,2,3,4,5,6,7,8,9,10};
		double[] gs = {1.001, 1.005, 1.01, 1.1};
		for (int j = 0; j < gs.length; j++){
			BufferedWriter bw = new BufferedWriter(new FileWriter(result+"-"+gs[j]+".txt"));
			bw.write("#c " + metric + " (mean, +-conf interval)");
		for (int i = 0; i < cs.length; i++){
			String line = ""+cs[i];
			String fl = "BALANCED_CA-"+vir+"-"+sj+"-"+cs[i]+"-"+gs[j]+"-FN_Model/";
			double[] res = GetValuesGTNA.getSingleValue(folder, fl, metric);
			line = line + " " + res[0] + " " + res[1]+" "+res[2];
			bw.newLine();
			bw.write(line);
			}
		bw.flush();
		bw.close();
		}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void getMinMax(String folder){
		double min = Double.MAX_VALUE;
		double max = 0;
		String[] files = (new File(folder)).list();
		for (int i = 0; i < files.length; i++){
			System.out.println(files[i]);
			try {
				BufferedReader br = new BufferedReader(new FileReader(folder+files[i]));
				String line = br.readLine();
				if (line != null){
					double val = Double.parseDouble(line.split("	", 2)[0]);
					if (val < min){
						min = val;
					}
					String next;
					while ((next = br.readLine())!= null){
						line = next;
					}
					double val2 = Double.parseDouble(line.split("	", 2)[1]);
					if (val2 > max){
						max = val2;
					}
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("min: " + (min+5*60*60));
		System.out.println("max: " + (max-24*60*60));
	}

}
