package treeembedding;

import java.util.Random;
import java.util.Vector;

public class Util {
	
	public static int[] getkOfN(int n, int k, Random rand){
		//System.out.println("n= " + n + " t=" + t);
		Vector<Integer> numbs = new Vector<Integer>();
		for (int i = 0; i < n; i++){
			numbs.add(i);
		}
		int[] s = new int[k];
		for (int i = 0; i < s.length; i++){
			s[i] = numbs.remove(rand.nextInt(numbs.size()));
		}
		return s;
	}
	
	public static int[] getkOfN(int n, int k, Random rand, boolean[] tried){
		//System.out.println("n= " + n + " t=" + t);
		Vector<Integer> numbs = new Vector<Integer>();
		for (int i = 0; i < n; i++){
			if (!tried[i]){
			numbs.add(i);
			}
		}
		int l = k;
		if (numbs.size() < k){
			l = numbs.size();
		}
		int[] s = new int[l];
		for (int i = 0; i < s.length; i++){
			s[i] = numbs.remove(rand.nextInt(numbs.size()));
		}
		return s;
	}

}
