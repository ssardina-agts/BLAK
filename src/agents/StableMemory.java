package agents;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import weka.core.*;

public class StableMemory 
{
	//private Instance state;
	private double previousProbability;
	private double deltaProbability;
	private int numberOfSuccesses;
	private int numberOfAttempts;
	public String targetDir;
	
	
	public StableMemory(String outputDir)
	{
		//setState(null);
		targetDir = outputDir;
		previousProbability = 0.0;
		deltaProbability = 0.0;//Double.MAX_VALUE;
		numberOfSuccesses = 0;
		numberOfAttempts = 0;
		
	}

	

	/*public void setState(Instance state) 
	{
		this.state = state;
	}


	public Instance getState() 
	{
		return state;
	}*/


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
		writeLog("No Suc: "+noSuc.doubleValue()+", no att: "+noAtt.doubleValue()+", result: "+temp, "NodeUpdates");
		//System.out.println("Nosuc: Prob: "+temp);
		return temp;
	}
	
	public void updateProbability()
	{
		writeLog("----------------------\nUpdating Prob", "NodeUpdates");
		double newProb = this.calculateCurrentProbability();
		writeLog("DeltaProb: "+this.deltaProbability, "NodeUpdates");
		
		double newDelta = Math.abs(this.previousProbability - newProb);
		writeLog("New DeltaProb: "+newDelta, "NodeUpdates");
		this.setPreviousProbability(newProb);
		this.setDeltaProbability(newDelta);
		
	}
	
	public String toString()
	{
		String returnString = "previous probability:"+previousProbability+", delta probability:"+deltaProbability
		+", successful attempts:"+numberOfSuccesses+", total attempts:"+numberOfAttempts;
		
		return returnString;
	}
	
	public String toString(int k, double e)
	{
		String returnString = "previous probability:"+previousProbability+", delta probability:"+deltaProbability
		+", successful attempts:"+numberOfSuccesses+", total attempts:"+numberOfAttempts;
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
	
	public Object clone()
	{
		StableMemory myClone  = new StableMemory(this.targetDir);
		myClone.setDeltaProbability(this.getDeltaProbability());
		myClone.setPreviousProbability(this.getPreviousProbability());
		myClone.setNumberOfSuccesses(this.getNumberOfSuccesses());
		myClone.setNumberOfAttempts(this.getNumberOfAttempts());
		
		return myClone;
	}
	
	public void writeLog(String msg, String postPend)
	{
		try
		{
			PrintWriter prnOut = new PrintWriter(new FileOutputStream(targetDir + "/" + postPend + ".txt", true), true);
			prnOut.println(msg);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created.");
		}
	}
}
