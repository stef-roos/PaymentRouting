package gtna.transformation.spanningtree;

import gtna.graph.Graph;
import gtna.graph.Node;
import gtna.graph.spanningTree.ParentChild;
import gtna.graph.spanningTree.SpanningTree;
import gtna.transformation.Transformation;
import gtna.util.parameter.BooleanParameter;
import gtna.util.parameter.Parameter;
import gtna.util.parameter.StringParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import treeembedding.credit.CreditLinks;

public class MultipleSpanningTree extends Transformation {
		String rootSelector;
		boolean randomorder; 
		Random rand;
		int trees;
		double p;
		boolean d;
		Direct oneDSel;
		
		public enum Direct{
			UP, DOWN, EITHER, BOTH, TWOPHASE, NONE
		}


		public MultipleSpanningTree(String rootSelector, int k, Random rand, double p, boolean depth, Direct oneD) {
			super("SPANNINGTREE_BFS", new Parameter[] { new StringParameter(
					"ROOT_SELECTOR", rootSelector), new BooleanParameter("RANDOM_ORDER",true), 
					new StringParameter("ONED",oneD.name())});
			this.rootSelector = rootSelector;
			randomorder = true;
	        this.rand = rand;
	        this.trees = k;
	        this.p = p;
	        this.d = depth;
	        this.oneDSel = oneD;
		}
		
		public MultipleSpanningTree(String rootSelector, int k, Random rand, double p, boolean depth) {
			this(rootSelector, k, rand, p, depth,Direct.UP );
		}

		@Override
		public Graph transform(Graph graph) {
			CreditLinks edgeweights = null;
			if (graph.hasProperty("CREDIT_LINKS")){
				edgeweights = (CreditLinks) graph.getProperty("CREDIT_LINKS");
			}
			if (this.rootSelector.equals("bfs")){
				Transformation tbfs = new BFSRand("rand", rand);
				for (int i = 0; i < trees; i++){
				    graph = tbfs.transform(graph);
				}
			} else {
				//select roots 
				int[] roots = new int[this.trees]; 
				if (this.rootSelector.equals("rand") || this.rootSelector.equals("bfs")){
				     for (int i = 0; i < roots.length; i++){
				    	 roots[i] = this.selectRoot(graph, rootSelector)[0];
				     }
				} else {
					int[] r = this.selectRoot(graph, rootSelector);
					for (int i = 0; i < roots.length; i++){
						roots[i] = r[i % r.length];
					}
				}
				//set up spanning tree
				Vector<HashMap<Integer, ParentChild>> parentChildMap = new Vector<HashMap<Integer, ParentChild>>();
				HashMap<Integer, Vector<int[]>> offers = new HashMap<Integer, Vector<int[]>>();
				Node[] nodes = graph.getNodes();
				int[][] parCount = new int[nodes.length][]; //count nr of parents 
				for (int i = 0; i < nodes.length; i++){
					//obtain set of neighbors that are potential parents based on capacity
					int l;
					if (this.oneDSel == Direct.TWOPHASE){
						l = potParents(graph, nodes[i], Direct.NONE, edgeweights).length;
					} else {
						l = potParents(graph, nodes[i], this.oneDSel, edgeweights).length;
					}
					parCount[i] = new int[l];
				}
				for (int i = 0; i < this.trees; i++){
					HashMap<Integer, ParentChild> map = new HashMap<Integer, ParentChild>();
					parentChildMap.add(map);
					map.put(roots[i], new ParentChild(-1,
							roots[i], 0));
					//obtain pot children of root 
					int[] out = potChildren(graph, nodes[roots[i]], this.oneDSel, edgeweights);
					if (out.length == 0){
						out = potChildren(graph, nodes[roots[i]], Direct.EITHER, edgeweights);
						if (out.length == 0){
							out = potChildren(graph, nodes[roots[i]], Direct.NONE, edgeweights);
						}
					}
					//root offers neighbors child position 
					for (int j = 0; j < out.length; j++){
						Vector<int[]> vec = offers.get(out[j]);
						if (vec == null){
							vec = new Vector<int[]>();
							offers.put(out[j], vec);
						}
						vec.add(new int[]{roots[i],i,1});
					}
				}
				
				while (!offers.isEmpty()){
					//iterate over nodes with offers 
					Iterator<Entry<Integer,Vector<int[]>>> it = offers.entrySet().iterator();
					HashMap<Integer, int[]> next = new HashMap<Integer, int[]>();
					while (it.hasNext()){
						//obtain data
						Entry<Integer,Vector<int[]>> entry = it.next();
						int index = entry.getKey();
						Vector<int[]> vec = entry.getValue();
						int[] out = this.potParents(graph, nodes[index], Direct.NONE, edgeweights);
					    String pot = "";
						HashMap<Integer, Integer> mapIndex = new HashMap<Integer, Integer>(out.length);
						int min = this.trees; //minimal tiem a neighbor is a parent 
						for (int j = 0; j < out.length; j++){
							if (parCount[index][j] < min){
								min = parCount[index][j];
							}
							mapIndex.put(out[j], j);
							pot = pot + " " + out[j];
						}
						Vector<Integer> choice = new Vector<Integer>();
						int curM = min;
						//add to choice all parents with minimal distance 
						while (choice.isEmpty()){
							for (int j = 0; j < vec.size(); j++){
								int[] a = vec.get(j);
								if (!mapIndex.containsKey(a[0])){
									System.out.println("Offer " + a[0] + " " + a[1] + " " + a[2] + " for " + index + " with pot " + pot
											+ " " + this.oneDSel);	
								}
								if (parCount[index][mapIndex.get(a[0])] == curM){
									choice.add(j);
								}
							}
				            if (choice.isEmpty()) curM++;
						}
						if (curM > min){
							//wait another round to see if a different parent can be used
							//this should never happen when p=1
							if (rand.nextDouble() > p){
								continue;
							}
						}
						int parent;
						if (this.d){
							int minC = vec.get(choice.get(0))[2];
							for (int j = 1; j < choice.size(); j++){
								if (vec.get(choice.get(j))[2] < minC){
									minC = vec.get(choice.get(j))[2];
								}
							}
							Vector<Integer> poss = new Vector<Integer>();
							for (int j = 0; j < choice.size(); j++){
								if (vec.get(choice.get(j))[2] == minC){
									poss.add(choice.get(j));
								}
							}
							parent = poss.get(rand.nextInt(poss.size()));
						} else {
							parent = choice.get(rand.nextInt(choice.size()));
						}
						int[] a = vec.get(parent);
						next.put(index, a);
						parCount[index][mapIndex.get(a[0])]++;
					}
					//generate offers for next level 
					Iterator<Entry<Integer,int[]>> itnext = next.entrySet().iterator();
					while (itnext.hasNext()){
						Entry<Integer,int[]> entry = itnext.next();
						int index = entry.getKey();
						int[] parent = entry.getValue();
						Vector<int[]> vec = offers.get(index);
						int i = 0;
						while (i < vec.size()){
						  int[] a = vec.get(i);
						  if (a[1] == parent[1]){
							  vec.remove(i);
						  } else {
							  i++;
						  }
						}	
						if (vec.size() == 0){
							offers.remove(index);
						}
						HashMap<Integer, ParentChild> map = parentChildMap.get(parent[1]);
						map.put(index, new ParentChild(parent[0],
								index, parent[2]));
						int[] out = potChildren(graph, nodes[index], this.oneDSel, edgeweights);
						for (int j = 0; j < out.length; j++){
							if (map.containsKey(out[j])) continue;
							int[] added = next.get(out[j]);
							if (added == null || added[1] != parent[1]){
								int[] o = new int[]{index,parent[1],parent[2]+1};
								Vector<int[]> vecOut = offers.get(out[j]);
								if (vecOut == null){
									vecOut = new Vector<int[]>();
									offers.put(out[j],vecOut);
								}
								vecOut.add(o);
							}
						}
					}
					//second phase if necessary
					if (offers.isEmpty() && this.oneDSel == Direct.TWOPHASE){
						this.oneDSel = Direct.EITHER;
						for (Node n: nodes){
							int index = n.getIndex();
							int[] out = potChildren(graph, n, Direct.EITHER, edgeweights);
							for (int i = 0; i < this.trees; i++){
								HashMap<Integer, ParentChild> map = parentChildMap.get(i);
								ParentChild parent = map.get(index);
								if (parent == null) continue;
								for (int j = 0; j < out.length; j++){
									if (!map.containsKey(out[j])){
										Vector<int[]> vecOut = offers.get(out[j]);
										if (vecOut == null){
											vecOut = new Vector<int[]>();
											offers.put(out[j],vecOut);
										}
										int[] o = new int[]{index,i,parent.getDepth()+1};
										vecOut.add(o);
									}
								}
							}
						}
					}
					
					//third phase if necessary
					if (offers.isEmpty()){
						this.oneDSel = Direct.NONE;
						for (Node n: nodes){
							int index = n.getIndex();
							int[] out = potChildren(graph, n, Direct.NONE, edgeweights);
							for (int i = 0; i < this.trees; i++){
								HashMap<Integer, ParentChild> map = parentChildMap.get(i);
								ParentChild parent = map.get(index);
								if (parent == null) continue;
								for (int j = 0; j < out.length; j++){
									if (!map.containsKey(out[j])){
										Vector<int[]> vecOut = offers.get(out[j]);
										if (vecOut == null){
											vecOut = new Vector<int[]>();
											offers.put(out[j],vecOut);
										}
										int[] o = new int[]{index,i,parent.getDepth()+1};
										vecOut.add(o);
									}
								}
							}
						}
					}
				}

				for (int i = 0; i < this.trees; i++){
                	ArrayList<ParentChild> parentChildList = new ArrayList<ParentChild>();
            		parentChildList.addAll(parentChildMap.get(i).values());
            		SpanningTree result = new SpanningTree(graph, parentChildList);

            		graph.addProperty(graph.getNextKey("SPANNINGTREE"), result);
                }
			}
			
			return graph;
		}
		
		

		private int[] selectRoot(Graph graph, String rootSelector) {
			Node[] nodeList = graph.getNodes();
			if (rootSelector.equals("randStart") || rootSelector.equals("rand")) {
				int rt = rand.nextInt(nodeList.length);
				return new int[]{rt};
			} else {
				try{
					String[] parts = rootSelector.split("-");
					int[] rt = new int[parts.length];
					for (int i = 0; i < parts.length; i++){
						rt[i] = Integer.parseInt(parts[i]);
					}
					return rt;
				} catch (NumberFormatException nfe){
				   throw new IllegalArgumentException("Unknown root selector " + rootSelector);
				}
			}
			
		}
		
		public static int[] potParents(Graph g, Node n, Direct dir, CreditLinks ew){
			switch (dir){
			case UP: return potChildren(g, n, Direct.DOWN, ew);
			case DOWN: return potChildren(g, n, Direct.UP, ew);
			case BOTH: 
			case TWOPHASE:	
				return potChildren(g, n, Direct.BOTH, ew);
			case EITHER: return potChildren(g, n, Direct.EITHER, ew);
			case NONE: return potChildren(g, n, Direct.NONE, ew);
			}
			
			return null;
		}
		
		public static int[] potChildren(Graph g, Node n, Direct dir, CreditLinks ew){
			int[] in = n.getOutgoingEdges();
			int[] out = n.getOutgoingEdges();
			if (ew != null && !dir.equals(Direct.NONE)){
				Vector<Integer> creditIn = new Vector<Integer>();
				Vector<Integer> creditOut = new Vector<Integer>();
				for (int j = 0; j < in.length; j++){
					if (ew.getPot(in[j], n.getIndex()) > 0){
						creditIn.add(in[j]);
//						if(in[j] == 19051 || n.getIndex() == 19051){
//							System.out.println("Added In " + in[j] + " for " + n.getIndex());
//						}
					}
					if (ew.getPot(n.getIndex(), out[j]) > 0){
						creditOut.add(out[j]);
//						if(out[j] == 19051 || n.getIndex() == 19051){
//							System.out.println("Added Out " + out[j] + " for " + n.getIndex());
//						}
					}
				}
				in = new int[creditIn.size()];
				for (int i = 0; i < in.length; i++){
					in[i] = creditIn.get(i);
				}
				out = new int[creditOut.size()];
				for (int i = 0; i < out.length; i++){
					out[i] = creditOut.get(i);
				}
			}
			switch (dir){
			case UP: return in;
			case DOWN: return out;
			case BOTH: 
			case TWOPHASE:	
				boolean[] take = new boolean[out.length];
				int c = 0;
				for (int j = 0; j < out.length; j++){
					take[j] = false;
					for (int i = 0; i < in.length; i++){
						if(in[i] == out[j]){
							take[j] = true;
						}
					}
					if (take[j]) c++;
				}
				int[] res = new int[c];
				int d = 0;
				for (int j = 0; j < out.length; j++){
					if (take[j]){
						res[d] = out[j];
						d++;
					}
				}
				return res;
			case NONE: 	
			case EITHER:
				take = new boolean[out.length];
				c = 0;
				for (int j = 0; j < out.length; j++){
					take[j] = true;
					for (int i = 0; i < in.length; i++){
						if(in[i] == out[j]){
							take[j] = false;
							break;
						}
					}
					if (take[j]) c++;
				}
				res = new int[in.length+c];
				for (int j = 0; j < in.length; j++){
						res[j] = in[j];
				}
				d = in.length;
				for (int j = 0; j < out.length; j++){
					if (take[j]){
						res[d] = out[j];
						d++;
					}
				}
				return res;
			}
			
			return null;
		}

		@Override
		public boolean applicable(Graph g) {
			return true;
		}

	}


