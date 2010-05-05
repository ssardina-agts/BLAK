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
	public int numberOfChildren() {
        int n = 0;
        for (int i = 0; i < children.size(); i++) {
            PlanNode thisNode = (PlanNode)this.children.elementAt(i);
            if(!thisNode.isFailedThresholdHandler()) {
                n++;
            }
        }
		return n;
	}
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - BUL related */
    /*-----------------------------------------------------------------------*/
    public boolean isStable(String[] checkState) {
        int[] val = stability(checkState);
        return ((val[0] != 0) && (val[0] == val[1]));
    }
    
    public int[] stability(String[] checkState) {
        int[] val = new int[2];
        int stable = 0;
        if(this.children.size() > 0) {
    		logger.writeLog("Goal "+name()+" is checking stability for state "+
                            this.stringOfState(checkState));
            logger.indentRight();
    		for(int i = 0; this.children.size()>i;i++) {
    			PlanNode thisNode = (PlanNode)this.children.elementAt(i);
    			if(!thisNode.isFailedThresholdHandler() && thisNode.isStable(checkState)) {
    				stable++;
    			}
    		}	
            logger.indentLeft();
    	}
        val[0] = stable;
        val[1] = numberOfChildren();
        return val;
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