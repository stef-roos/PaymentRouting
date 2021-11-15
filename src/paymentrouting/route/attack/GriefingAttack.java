package paymentrouting.route.attack;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import gtna.graph.Graph;
import treeembedding.credit.CreditLinks;
import treeembedding.credit.Transaction;

public class GriefingAttack {
	int attvalue;
	int startIt;
	
	public GriefingAttack(int v, int start) {
		this.attvalue = v;
		this.startIt = start; 
	}
	
	public Transaction[] addTx(Transaction[] txs, HashSet<Integer> attackers, Random rand, Graph g, CreditLinks weights) {
		if (attackers.size() < 2 || this.startIt > txs.length) {
			return txs;
		}
		int[] att = new int[attackers.size()];
		Iterator<Integer> it = attackers.iterator();
		int i = 0;
		while (it.hasNext()) {
			att[i] = it.next();
			i++;
		}
		int[] attTx = new int[att.length]; 
		int totalAdded = 0;
		int max = 0; 
		for (int j = 0; j < attTx.length; j++) {
			//get total capacity of attacker
			double c = 0;
			int[] out = g.getNode(att[j]).getOutgoingEdges();
			for (int k: out) {
				c = c + weights.getTotalCapacity(att[j], k); 
			}
			//get number of tx possible
			attTx[j] = (int) (c/this.attvalue);
			totalAdded = totalAdded + attTx[j]; 
			max = Math.max(max,attTx[j]);
		}
		
		Transaction[] allTx = new Transaction[txs.length + totalAdded]; 
		//add normal transactions
		for (int j = 0; j <= this.startIt; j++) {
			allTx[j] = txs[j];
		}
		double time = txs[this.startIt].getTime();
		int nrAtt = 0; 
		int nrNormal = this.startIt+1; 
		int nrAll = this.startIt+1;
		while (nrAtt < max) {
			//add in adversarial transactions 
			for (int j = 0; j < attTx.length; j++) {
				if (nrAtt >= attTx[j]) continue; //all txs added for this attacker 
				int r = rand.nextInt(attTx.length);
				while (r == j) {
					r = rand.nextInt(attTx.length);
				}
				Transaction aTx = new Transaction(time,this.attvalue, att[j],att[r]);
				allTx[nrAll] = aTx; 
				nrAll++; 		
			}
			time = time + 1; //one second break to starting next transaction 
			while (nrNormal < txs.length && txs[nrNormal].getTime() < time) { //adding any normal transactions happening during this time 
				allTx[nrAll] = txs[nrNormal]; 
				nrAll++; nrNormal++; 
			}
			nrAtt++;
		}
		while (nrNormal < txs.length) {
			allTx[nrAll] = txs[nrNormal]; 
			nrAll++; nrNormal++; 
		}
		System.out.println("tx " + allTx.length); 
		return allTx; 
	}

}
