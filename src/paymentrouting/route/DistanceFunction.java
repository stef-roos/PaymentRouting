package paymentrouting.route;

import java.util.Random;

import gtna.graph.Graph;

/**
 * blueprint for distance functions, implemented in HopDistance, SpeedyMurmurs, SpeedyMurmursMulti
 * @author mephisto
 *
 */
public abstract class DistanceFunction {
     String name;
     public int realities; //number of dimensions (e.g., spanning trees)
     int startR; //if multiple dimensions: which one is relevant (if only one) 
     Timelock timelockmode; //how to treat timelocks
     int lock; //value of the timelock if a constant
     
     public enum Timelock{
    	 //CONST: global constant for timelock value
    	 //MIN: minimal distance between sender and receiver (in all dimensions) 
    	 //MAX: maximal distance between sender and receiver
 		CONST, MIN, MAX
 	}
     
     public DistanceFunction(String name, int realities) {
    	 this(name, realities, realities);
     }
     
     public DistanceFunction(String name, int realities, int s) {
    	 this(name, realities, s, Timelock.CONST, Integer.MAX_VALUE);
     }
     
     
     
     public DistanceFunction(String name, int realities, int s, Timelock lockMode) {
    	 this(name, realities, s, lockMode, Integer.MAX_VALUE); 
     }
     
     public DistanceFunction(String name, int realities, int s, Timelock lockMode, int lockval) {
    	 this.name = name;
    	 this.realities = realities;
    	 this.startR = s;
    	 this.timelockmode = lockMode;
    	 this.lock = lockval; 
     }
     
     /**
      * return timelock for a payment between src and dst based on timelockmode and val (if CONST) 
      * @param src
      * @param dst
      * @return
      */
     public int getTimeLock(int src, int dst) {
    	 switch (this.timelockmode) {
    	 case CONST: 
    		 return this.lock;
    	 case MIN:
    		 //compute minimum
    		 int res = Integer.MAX_VALUE;
    		 for (int i = 0; i < this.realities; i++) {
    			 double d = this.distance(src, dst, i);
    			 if (d < res) {
    				 res = (int)Math.ceil(d);
    			 }
    		 }
    		 return res;
    	 case MAX: 
    		 //compute maximum 
    		 res = 0;
    		 for (int i = 0; i < this.realities; i++) {
    			 double d = this.distance(src, dst, i);
    			 if (d > res) {
    				 res = (int)Math.ceil(d);
    			 }
    		 }
    		 return res;
    	 default: throw new IllegalArgumentException("unknown mode");
    	 }
     }
     
     /**
	 * distance between cur and dst according to some distance function
	 * @param nodes
	 * @param a
	 * @param dst
	 * @return
	 */
     public abstract double distance(int a, int b, int r);
     
     /**
      * is a closer to dst than b? 
      * @param a
      * @param b
      * @param dst
      * @return
      */
     public abstract boolean isCloser(int a, int b, int dst, int r);
     
     public abstract void initRouteInfo(Graph g, Random rand);
     
     
     
	
}
