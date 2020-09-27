package paymentrouting.route;

import java.util.Random;

import gtna.graph.Graph;

public abstract class DistanceFunction {
     String name;
     public int realities;
     private int startR;
     Timelock timelockmode;
     int lock; 
     
     public enum Timelock{
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
    	 this.setStartR(s);
    	 this.timelockmode = lockMode;
    	 this.lock = lockval; 
     }
     
     public int getTimeLock(int src, int dst) {
    	 switch (this.timelockmode) {
    	 case CONST: 
    		 return this.lock;
    	 case MIN:
    		 int res = Integer.MAX_VALUE;
    		 for (int i = 0; i < this.realities; i++) {
    			 double d = this.distance(src, dst, i);
    			 if (d < res) {
    				 res = (int)Math.ceil(d);
    			 }
    		 }
    		 return res;
    	 case MAX: 
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

	public int getStartR() {
		return startR;
	}

	public void setStartR(int startR) {
		this.startR = startR;
	}
     
     
     
	
}
