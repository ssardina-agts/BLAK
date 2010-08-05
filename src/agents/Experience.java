package agents;

import trees.*;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Vector;

import weka.core.*;

class StabilityInfo {
    public int nStable;
    public int nTotal;
    public StabilityInfo(int stable, int total) {
        nStable = stable;
        nTotal = total;
    }
}

public class Experience 
{
    /*-----------------------------------------------------------------------*/
    /* MARK: Data Members */
    /*-----------------------------------------------------------------------*/

	private double previousProbability;
	private double deltaProbability;
	private int numberOfSuccesses;
	private int numberOfAttempts;
    private Hashtable coverage;
    private Logger logger;
    private String[] state;
    private Hashtable decay;
    private boolean useForLearning;
    private boolean hasStableChildren;
    private Vector<StabilityInfo> stableHistory;
	
    public static final int STABLE_HISTORY_CAPACITY = 100;
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Constructors */
    /*-----------------------------------------------------------------------*/
	public Experience(Logger logger)
	{
		//setState(null);
		this.logger = logger;
		previousProbability = 0.0;
		deltaProbability = 0.0;//Double.MAX_VALUE;
		numberOfSuccesses = 0;
		numberOfAttempts = 0;
        coverage = new Hashtable();
        decay = new Hashtable();
        useForLearning = true;
        hasStableChildren = false;
        stableHistory = new Vector<StabilityInfo>();
	}

    /*-----------------------------------------------------------------------*/
    /* MARK: Access Members */
    /*-----------------------------------------------------------------------*/

    public boolean useForLearning() { return useForLearning; }
    public void setUseForLearning(boolean val) { useForLearning = val; }

    public boolean hasStableChildren() { return hasStableChildren; }
    public void setHasStableChildren(boolean val) { hasStableChildren = val; }

    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Misc */
    /*-----------------------------------------------------------------------*/

	public int addCoverage(String[] paths, int depth) 
	{
        int added = 0;
        for (int i = 0; paths != null && i < paths.length; i++, added++) {
            addCoverage(paths[i],depth);
        }
        return added;
	}
    
	public boolean addCoverage(String path, int depth) 
	{
        if (path != null) {
            Hashtable c =  (coverage.containsKey(depth)) ? (Hashtable)(coverage.get(depth)) : new Hashtable();
            c.put(path,true);
            coverage.put(depth,c);
            return true;
        }
        return false;
	}
    
	public int coverage(int depth) 
	{
        Hashtable c = (Hashtable)(coverage.get(depth));
		return (c != null) ? c.size() : 0;
	}

    public void setDecay(int depth, double d) {
        decay.put(depth,d);
	}

    public double getDecay(int depth) {
        return decay.containsKey(depth) ? ((Double)(decay.get(depth))).doubleValue() : 1.0;
	}
    

	public void setPreviousProbability(double previousProbability) 
	{
		this.previousProbability = previousProbability;
	}


	public double getPreviousProbability() 
	{
		return previousProbability;
	}


	public void setDeltaProbability(double deltaProbability) 
	{
		this.deltaProbability = deltaProbability;
	}


	public double getDeltaProbability() 
	{
		return deltaProbability;
	}


	public void setNumberOfSuccesses(int numberOfSuccesses) 
	{
		this.numberOfSuccesses = numberOfSuccesses;
	}


	public int getNumberOfSuccesses() 
	{
		return numberOfSuccesses;
	}

    public int getNumberOfFailures() 
	{
		return (numberOfAttempts - numberOfSuccesses);
	}
    

	public void setNumberOfAttempts(int numberOfAttempts) 
	{
		this.numberOfAttempts = numberOfAttempts;
	}


	public int getNumberOfAttempts() 
	{
		return numberOfAttempts;
	}
	
	public void incrementSuccesses()
	{
		this.numberOfSuccesses++;
	}
	
	public void incrementAttempts()
	{
		this.numberOfAttempts++;
	}
	
	public double calculateCurrentProbability()
	{
		Double noSuc = new Double(this.numberOfSuccesses);
		
		Double noAtt = new Double(this.numberOfAttempts);
		double temp = noSuc.doubleValue()/noAtt.doubleValue();
		return temp;
	}
	
	public void updateProbability()
	{
		double newProb = this.calculateCurrentProbability();
		double newDelta = Math.abs(this.previousProbability - newProb);
		this.setPreviousProbability(newProb);
		this.setDeltaProbability(newDelta);
	}
	
    public void clearStableHistory() {
        stableHistory.clear();
    }

    public void addStableHistory(int stable, int total) {
        stableHistory.add(new StabilityInfo(stable,total));
        if (stableHistory.size() > STABLE_HISTORY_CAPACITY) {
            /* Pop the oldest element if we have exceeded capacity */
            stableHistory.remove(0);
        }
    }
    
    public double averageStability(int window) {
        /* Returns the average of last 'window' samples from stableHistory */
        if (window < 1) { return 0.0;}
        double sum = 0.0;
        double total = 0.0;
        int len = (window < stableHistory.size()) ? window : stableHistory.size();
        StabilityInfo[] hist = stableHistory.toArray(new StabilityInfo[0]);
        for (int i = hist.length-1; i > hist.length-1-len; i--) {
            sum += hist[i].nStable;
            total += hist[i].nTotal;
        }
        return sum/(total+(window-len));
    }
    
	public String toString()
	{
		String returnString = "previous probability:"+previousProbability+
        ", delta probability:"+deltaProbability+
		", successful attempts:"+numberOfSuccesses+
        ", total attempts:"+numberOfAttempts+
        ", coverage:"+coverage.size()+
        ", use for learning:"+useForLearning;
		
		return returnString;
	}
	
	public String toString(int k, double e)
	{
		String returnString = "previous probability:"+previousProbability+", delta probability:"+deltaProbability
		+", successful attempts:"+numberOfSuccesses+", total attempts:"+numberOfAttempts+", coverage:"+coverage.size();
		if(this.isStateStable(k, e))
		{
			returnString = returnString + ", stable: yes";
		}
		else
		{
			returnString = returnString + ", stable: no";
		}
		
		return returnString;
	}
	
	public boolean isStateStable(int k, double e)
	{
		//System.out.println("State stable check. K: "+k+", epis: "+e);
		if(this.getNumberOfAttempts()>=k && e>=this.getDeltaProbability())
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
    public void setState (String[] s) {
        state = s;
    }

    public String[] getState () {
        return state;
    }
    
	public Object clone()
	{
		Experience myClone  = new Experience(this.logger);
		myClone.setDeltaProbability(this.getDeltaProbability());
		myClone.setPreviousProbability(this.getPreviousProbability());
		myClone.setNumberOfSuccesses(this.getNumberOfSuccesses());
		myClone.setNumberOfAttempts(this.getNumberOfAttempts());
		
		return myClone;
	}
}
