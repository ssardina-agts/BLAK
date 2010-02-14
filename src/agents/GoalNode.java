package agents;
import trees.*;
import java.util.*;

public class GoalNode extends Node{

    /*-----------------------------------------------------------------------*/
    /* MARK: Data Members */
    /*-----------------------------------------------------------------------*/

    
    /*-----------------------------------------------------------------------*/
    /* MARK: Constructors */
    /*-----------------------------------------------------------------------*/

    public GoalNode(String name, Logger logger){
		super(name, logger);
    }

    /*-----------------------------------------------------------------------*/
    /* MARK: Access Members */
    /*-----------------------------------------------------------------------*/

    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - BUL related */
    /*-----------------------------------------------------------------------*/
    
	public String[] calculateEndStateFrom(String[] state) {
    	logger.writeLog("Goal "+name()+" is determining end state starting with state "+this.stringOfState(state));
        String[] checkState = null;
    	if(this.children.size()>0) {
    		/* Find the child which has a non null last state i.e.
             * the child that was selected in the last run.
             */
            logger.indentRight();
    		for(int j =0; this.children.size()>j;j++) {
    			PlanNode thisNode = (PlanNode)this.children.elementAt(j);
    			if(thisNode.lastState()!=null) {
    				if(checkState==null) {
    					checkState = thisNode.lastState();
                        logger.writeLog("Child plan "+thisNode.name()+" has last state "+thisNode.stringOfLastState()+". Goal "+name()+" will therefore use this state.");
    				}
    			} else {
    				//logger.writeLog("Child plan "+thisNode.name()+" has NULL last state");
    			}
    		}
            logger.indentLeft();
        }
        return checkState;
    }
    
    public boolean isStable(String[] checkState)
    {
    	if(this.children.size()>0) {
    		if(checkState==null) {
                /* We have never been used in this state so can't say we are stable */
    			logger.writeLog("Goal "+name()+" found ALL last states to be NULL, so assume not stable");
                return false;
    		}
            
    		logger.writeLog("Goal "+name()+" is checking stability for state "+this.stringOfState(checkState));
            
            /* If we have succeeded in this state before then stability checking 
             * doesn't make sense (we may never experience other options
             * because we always choose the winning one), so in this case
             * assume we are stable.
             */
            if (this.isSuccessful(checkState)) {
                logger.writeLog("Goal "+name()+" is stable for state "+this.stringOfState(checkState)+" since it has succeeded in this state before");
                return true;
            }
            
            logger.indentRight();
    		for(int i = 0; this.children.size()>i;i++) {
    			PlanNode thisNode = (PlanNode)this.children.elementAt(i);
    			if(!thisNode.isStable(checkState)) {
    				logger.writeLog("Child plan "+thisNode.name()+" is unstable for state "+this.stringOfState(checkState));
    				return false;
    			} else if (thisNode.isSuccessful(checkState)) {
                    /* Fine, so this child plan node is stable, but if this child
                     * was successful then there is no point continuing because 
                     * the next child plan may never be tried (since we 
                     * would select the successful option over it) 
                     * and therefore the next child will likely fail the
                     * the stability test.
                     */
    				logger.writeLog("Child plan "+thisNode.name()+" has previously succeeded in state "+this.stringOfState(checkState)+" so forego remaining children and consider us ("+name()+") stable");
                    return true;
                }
    		}	
            logger.indentLeft();
    		
            /* Made it all the way through the children and they are all stable,
    		 * so consider this goal stable.
             */
    		return true;
    	} else {
    		/* We shouldn't reach this point, as a goal should always have children. 
    		 * But if is has none, lets assume it is stable.
             */
    		return true;
    	}
    }

    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Coverage related */
    /*-----------------------------------------------------------------------*/

    public long getPaths(int depth) {
        if (pathsKnown(depth)) {
            return paths(depth);
        }
        long p = 0;
        int nChildren = this.children.size();
        if(nChildren > 0) {
            for(int j = 0; nChildren > j; j++) {
                PlanNode thisNode = (PlanNode)this.children.elementAt(j);
                if (!thisNode.isFailedThresholdHandler()) {
                    p += thisNode.getPaths(depth);
                }
            }    
        }
        setPathsKnown(depth);
        setPaths(depth,p);
        return p;
    }
    
    public String pathID(int depth) {
        return name()+Integer.toString(depth);
    }

    public Vector getPathStrings(int depth) {
        Vector v = new Vector();
        int nChildren = this.children.size();
        if(nChildren > 0) {
            for(int j = 0; nChildren > j; j++) {
                PlanNode thisNode = (PlanNode)this.children.elementAt(j);
                boolean isLeafChild = (depth == 0) ? true : (thisNode.children.size() == 0);
                if (!thisNode.isFailedThresholdHandler()) {
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
    
    public int getCoverage(String[] state, int depth) {
        int coverage = 0;
        int nChildren = this.children.size();
        logger.indentRight();
        for(int j = 0; nChildren > j; j++) {
            PlanNode thisNode = (PlanNode)this.children.elementAt(j);
            if (!thisNode.isFailedThresholdHandler()) {
                coverage += thisNode.getCoverage(state, depth);
            }
        }
        logger.indentLeft();
        return coverage;
    }
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Failure Recovery related */
    /*-----------------------------------------------------------------------*/

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
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Misc */
    /*-----------------------------------------------------------------------*/
    
    public void addChild(PlanNode node){
        node.setParent(this);
        children.add(node);
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
}