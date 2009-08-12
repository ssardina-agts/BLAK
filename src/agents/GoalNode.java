package agents;
import trees.*;


public class GoalNode extends Node{

    public int goal_id;
    public String name;
    public int successfulChildren;
	public String targetDir;
    private Logger logger;
    
    public GoalNode(int id, String gname, Logger logger){
		super(gname, logger);
		name = gname;
		goal_id = id;
		successfulChildren = 0;
		targetDir = "";
    }
	
    public boolean isStable(String[] state)
    {
    	this.writeLog("Goal Node "+name+" is determining which state to check stability for, starting with state "+this.stringOfState(state), targetDir + "/" + "Stability-Updates");
    	if(this.children.size()>0)
    	{
    		String[] checkState = null;
    		//find the children which has a non null last state..
    		for(int j =0; this.children.size()>j;j++)
    		{
    			PlanNode thisNode = (PlanNode)this.children.elementAt(j);
    			if(thisNode.lastState!=null)
    			{
    				this.writeLog("-Child plan "+thisNode.getItem()+" has last state "+thisNode.stringOfLastState(), targetDir + "/" + "Stability-Updates");
    				if(checkState==null)
    				{
    					checkState = thisNode.lastState;
    					this.writeLog("-Goal Node "+name+" should therefore check stability for state "+this.stringOfState(checkState), targetDir + "/" + "Stability-Updates");
    					
    				}
    			}
    			else
    			{
    				this.writeLog("-Child plan "+thisNode.getItem()+" has NULL last state", targetDir + "/" + "Stability-Updates");
    			}
    			
    		}
    		if(checkState==null)
    		{
    			//System.out.println("Goal Node: Could not find check state");
    			//this.writeLog("-Goal Node "+name+" found ALL last states to be NULL, so assume stable"/*+" HACK STABLE"*/, targetDir + "/" + "Stability-Updates");
    			//skip it....this is a quick and dirty hack. We aren't stable, because we have not been used.
    			//rather we are irrelevant to the question of stability....
    			//return true;
                
                /* We have never been used in this state so can't say we are stable */
    			this.writeLog("-Goal Node "+name+" found ALL last states to be NULL, so assume not stable"/*+" HACK STABLE"*/, targetDir + "/" + "Stability-Updates");
                return false;
    			
    		}
    			
    		//check their stability
    		this.writeLog("-Goal Node "+name+" is now checking stability for determined state "+this.stringOfState(checkState), targetDir + "/" + "Stability-Updates");
            
            /* If we have succeeded in this state before then stability checking 
             * doesn't make sense (we may never experience other options
             * because we always choose the winning one), so in this case
             * assume we are stable.
             */
            if (this.isSuccessful(checkState)) {
                this.writeLog("-Goal Node "+name+" is stable for state "+this.stringOfState(checkState)+" since it has succeeded in this state before", targetDir + "/" + "Stability-Updates");
                return true;
            }
            
    		for(int i = 0; this.children.size()>i;i++)
    		{
    			PlanNode thisNode = (PlanNode)this.children.elementAt(i);
    			if(!thisNode.isStable(checkState))
    			{
    				this.writeLog("--Child plan "+thisNode.getItem()+" is unstable for state "+this.stringOfState(checkState), targetDir + "/" + "Stability-Updates");
    				return false;
    			} else if (thisNode.isSuccessful(checkState)) {
                    /* Fine, so this child plan node is stable, but if this child
                     * was successful then there is no point continuing because 
                     * the next child plan would never be tried (since we 
                     * would select the successful option over it) 
                     * and therefore the next child will always fail the
                     * the stability test.
                     * The reality is that this goal is in fact stable, because
                     * there is nothing else to try in this state.
                     */
    				this.writeLog("--Child plan "+thisNode.getItem()+" has previously succeeded in state "+this.stringOfState(checkState)+" so forego remaining children and consider us ("+name+") stable", targetDir + "/" + "Stability-Updates");
                    return true;
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
    
    /* A goal is successful whenever ANY child plan is successful */
	public boolean isSuccessful(String[] state)
	{
		if(this.children.size()>0)
		{
			for(int i =0; this.children.size()>i;i++)
			{
				PlanNode thisNode = (PlanNode)this.children.elementAt(i);
				if(thisNode.isSuccessful(state))
				{
					return true;
				}
			}
		}
		return false;
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
		GoalNode myClone  = new GoalNode(this.goal_id, this.name, this.logger);
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