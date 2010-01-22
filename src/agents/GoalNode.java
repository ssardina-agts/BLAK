package agents;
import trees.*;
import java.util.*;

public class GoalNode extends Node{

    public int goal_id;
    public String name;
    public int successfulChildren;
    
    public GoalNode(int id, String gname, Logger logger){
		super(gname, logger);
		name = gname;
		goal_id = id;
		successfulChildren = 0;
    }
	
    public boolean isStable(String[] state)
    {
    	logger.writeLog("Goal "+name+" is determining which state to check stability for, starting with state "+this.stringOfState(state));
    	if(this.children.size()>0)
    	{
    		String[] checkState = null;
    		//find the children which has a non null last state..
            logger.indentRight();
    		for(int j =0; this.children.size()>j;j++)
    		{
    			PlanNode thisNode = (PlanNode)this.children.elementAt(j);
    			if(thisNode.lastState!=null)
    			{
    				logger.writeLog("Child plan "+thisNode.getItem()+" has last state "+thisNode.stringOfLastState());
    				if(checkState==null)
    				{
    					checkState = thisNode.lastState;
    					logger.writeLog("Goal "+name+" should therefore check stability for state "+this.stringOfState(checkState));
    					
    				}
    			}
    			else
    			{
    				logger.writeLog("Child plan "+thisNode.getItem()+" has NULL last state");
    			}
    			
    		}
            logger.indentLeft();
            
    		if(checkState==null)
    		{
    			//System.out.println("Goal Node: Could not find check state");
    			//this.writeLog("-Goal Node "+name+" found ALL last states to be NULL, so assume stable"/*+" HACK STABLE"*/, targetDir + "/" + "Stability-Updates");
    			//skip it....this is a quick and dirty hack. We aren't stable, because we have not been used.
    			//rather we are irrelevant to the question of stability....
    			//return true;
                
                /* We have never been used in this state so can't say we are stable */
    			logger.writeLog("Goal "+name+" found ALL last states to be NULL, so assume not stable");
                return false;
    			
    		}
    			
    		//check their stability
    		logger.writeLog("Goal "+name+" is now checking stability for state "+this.stringOfState(checkState));
            
            /* If we have succeeded in this state before then stability checking 
             * doesn't make sense (we may never experience other options
             * because we always choose the winning one), so in this case
             * assume we are stable.
             */
            if (this.isSuccessful(checkState)) {
                logger.writeLog("Goal "+name+" is stable for state "+this.stringOfState(checkState)+" since it has succeeded in this state before");
                return true;
            }
            
            logger.indentRight();
    		for(int i = 0; this.children.size()>i;i++)
    		{
    			PlanNode thisNode = (PlanNode)this.children.elementAt(i);
    			if(!thisNode.isStable(checkState))
    			{
    				logger.writeLog("Child plan "+thisNode.getItem()+" is unstable for state "+this.stringOfState(checkState));
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
    				logger.writeLog("Child plan "+thisNode.getItem()+" has previously succeeded in state "+this.stringOfState(checkState)+" so forego remaining children and consider us ("+name+") stable");
                    return true;
                }
    		}	
            logger.indentLeft();
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

    public long getPaths(int depth) {
        if (pathsKnown(depth)) {
            return paths(depth);
        }
        long p = 0;
        int nChildren = this.children.size();
        if(nChildren > 0) {
            for(int j = 0; nChildren > j; j++) {
                PlanNode thisNode = (PlanNode)this.children.elementAt(j);
                if (!thisNode.isFailedThresholdHandler) {
                    p += thisNode.getPaths(depth);
                }
            }    
        }
        setPathsKnown(depth);
        setPaths(depth,p);
        return p;
    }
    
    public String pathID(int depth) {
        return (String)(getItem())+Integer.toString(depth);
    }

    public Vector getPathStrings(int depth) {
        Vector v = new Vector();
        int nChildren = this.children.size();
        if(nChildren > 0) {
            for(int j = 0; nChildren > j; j++) {
                PlanNode thisNode = (PlanNode)this.children.elementAt(j);
                boolean isLeafChild = (depth == 0) ? true : (thisNode.children.size() == 0);
                if (!thisNode.isFailedThresholdHandler) {
                    Vector pv = thisNode.getPathStrings(depth);
                    for (int k = 0; k < pv.size(); k++) {
                        v.add(pathID(depth)+(String)(pv.elementAt(k)));
                    }
                }
            }
        }
        return v;
    }
    
    public String getDirtyPath(int depth) {
        String path = "";
        int nChildren = this.children.size();
        for(int j = 0; nChildren > j; j++) {
            PlanNode thisNode = (PlanNode)this.children.elementAt(j);
            path = thisNode.getDirtyPath(depth);
            if (path.length() > 0) {
                return pathID(depth)+path;
            }
        }
        return path;
    }

    public void clearDirtyPath(int depth) {
        int nChildren = this.children.size();
        for(int j = 0; nChildren > j; j++) {
            PlanNode thisNode = (PlanNode)this.children.elementAt(j);
            thisNode.clearDirtyPath(depth);
        }
    }

    public boolean isPropagatingFailure() {
        int nChildren = this.children.size();
        for(int j = 0; nChildren > j; j++) {
            PlanNode thisNode = (PlanNode)this.children.elementAt(j);
            if (thisNode.isPropagatingFailure()) {
                return true;
            }
        }
        return false;
    }
    
    public int getCoverage(String[] state, int depth) {
        int coverage = 0;
        int nChildren = this.children.size();
        logger.indentRight();
        for(int j = 0; nChildren > j; j++) {
            PlanNode thisNode = (PlanNode)this.children.elementAt(j);
            if (!thisNode.isFailedThresholdHandler) {
                coverage += thisNode.getCoverage(state, depth);
            }
        }
        logger.indentLeft();
        return coverage;
    }
    
    public String[] resultingState(String[] thisState) {
        /* Find the state (resultingState) that eventuated from a 
         * series of subgoal executions starting in state thisState. 
         */
        String[] resultingState = null;
        String stateStr = this.stringOfState(thisState);
        int nChildren = this.children.size();
        if(nChildren == 0) {
            /* Goal has no children, should never happen */
            logger.writeLog("ERROR: Goal "+this.getItem()+" has no children, so assume resulting state is same as "+stateStr);
            resultingState = thisState;
        }
        for(int j =0; nChildren>j;j++) {
            PlanNode thisNode = (PlanNode)this.children.elementAt(j);
            if(thisNode.lastState!=null) {
                resultingState = thisNode.lastState;
                logger.writeLog("Goal "+name+" found resulting state "+this.stringOfState(resultingState));
                break;
            }
        }
        if(resultingState==null) {
            logger.writeLog("Goal "+name+" found ALL last states to be NULL, so assume resulting state is same as "+stateStr);
            resultingState = thisState;
        }
        return resultingState;
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