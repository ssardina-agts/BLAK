package agents;

import trees.*;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Hashtable;

import weka.core.*;

public class Experience 
{
	//private Instance state;
	private double previousProbability;
	private double deltaProbability;
	private int numberOfSuccesses;
	private int numberOfAttempts;
    private Hashtable coverage;
    private Logger logger;
    private String[] state;
	
	
	public Experience(Logger logger)
	{
		//setState(null);
		this.logger = logger;
		previousProbability = 0.0;
		deltaProbability = 0.0;//Double.MAX_VALUE;
		numberOfSuccesses = 0;
		numberOfAttempts = 0;
        coverage = new Hashtable();
	}

	

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
	
	public String toString()
	{
		String returnString = "previous probability:"+previousProbability+", delta probability:"+deltaProbability
		+", successful attempts:"+numberOfSuccesses+", total attempts:"+numberOfAttempts+", coverage:"+coverage.size();
		
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
