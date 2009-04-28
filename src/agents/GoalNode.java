package agents;
import trees.*;


public class GoalNode extends Node{

    public int goal_id;
    public String name;
    public int successfulChildren;
	public String targetDir;
    
    public GoalNode(int id, String gname, String outputDir){
		super(gname);
		name = gname;
		goal_id = id;
		successfulChildren = 0;
		targetDir = outputDir;
    }
	
    public boolean isStable(String[] state)
    {
    	this.writeLog("Goal Node :"+name+", checking stability. Will use state: "+this.stringOfState(state), targetDir + "/" + "Stability-Updates");
    	if(this.children.size()>0)
    	{
    		String[] checkState = null;
    		//find the children which has a non null last state..
    		for(int j =0; this.children.size()>j;j++)
    		{
    			PlanNode thisNode = (PlanNode)this.children.elementAt(j);
    			if(thisNode.lastState!=null)
    			{
    				this.writeLog("Found Last State: "+thisNode.stringOfLastState()+ " at plan: "+thisNode.getItem(), targetDir + "/" + "Stability-Updates");
    				if(checkState==null)
    				{
    					checkState = thisNode.lastState;
    					this.writeLog(name+": will now use state: "+this.stringOfState(checkState), targetDir + "/" + "Stability-Updates");
    					
    				}
    			}
    			else
    			{
    				this.writeLog("Found Last State to be NULL at plan: "+thisNode.getItem(), targetDir + "/" + "Stability-Updates");
    			}
    			
    		}
    		if(checkState==null)
    		{
    			//System.out.println("Goal Node: Could not find check state");
    			this.writeLog("Found ALL Last States to be NULL at plan: "+getItem()+" HACK STABLE", targetDir + "/" + "Stability-Updates");
    			//skip it....this is a quick and dirty hack. We aren't stable, because we have not been used.
    			//rather we are irrelevant to the question of stability....
    			return true;
    			//System.exit(0);
    			
    		}
    			
    		//check their stability
    		this.writeLog("Goal Node :"+name+", double checking stability. Will use state: "+this.stringOfState(checkState), targetDir + "/" + "Stability-Updates");
    		for(int i = 0; this.children.size()>i;i++)
    		{
    			PlanNode thisNode = (PlanNode)this.children.elementAt(i);
    			
    			if(!thisNode.isStable(checkState))
    			{
    				this.writeLog("Goal Node :"+name+" found "+thisNode.getItem()+" to be unstable for state: "+this.stringOfState(checkState), targetDir + "/" + "Stability-Updates");
    				
    				return false;
    			}
    			
    		}	
    		//Made it all the way through the children and they are all stable,
    		//Therefore we are considered stable :D 
    		return true;
    	}
    	else
    	{
    		//We shouldn't reach this point, as a goal should always have children. 
    		//But if is has none, lets assume it's stable.
    		return true;
    		
    	}
    }
    
	public boolean equals(Object obj)
	{
		if(obj instanceof GoalNode)
		{
			GoalNode compareTo = (GoalNode)obj;
			if(compareTo.getItem().equals(this.getItem()))
			{
				return true;
			}
		}
		return false;
	}
	
	public Object clone()
	{
		GoalNode myClone  = new GoalNode(this.goal_id, this.name, this.targetDir);
		return myClone;
	}
	
	public void setSuccessFulChildren(int newValue)
	{
		this.successfulChildren = newValue;
	}
	
	public int getSuccessfulChildren()
	{
		return this.successfulChildren;
	}
	
	public void resetSuccessfulChildren()
	{
		this.successfulChildren = 0;
		this.successful = false;
	}

	public boolean determineSuccessful()
	{
		if(this.children.size()>0)
		{
			for(int i =0; this.children.size()>i;i++)
			{
				PlanNode thisNode = (PlanNode)this.children.elementAt(i);
				if(thisNode.isSuccessful())
				{
					this.successful = true;
					return this.successful;
				}
			}
			this.successful = false;
		}
		return this.successful;
		//if we have no children then can we be successful?
		//Yes, but we need to be the first place to become successful, which requires us to be a plan with a successful action.
	}
	
	public void addSuccessfulChild()
	{
		this.successfulChildren++;
	}

	public int getNumberOfChildren()
	{
		return this.children.size();
	}

	public boolean allSuccessFul()
	{
		if(this.successfulChildren==1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
}