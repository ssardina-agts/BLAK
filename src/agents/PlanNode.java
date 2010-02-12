package agents;

import agents.Config.UpdateMode;
import agents.Config.PlanSelectMode;
import agents.Config.RunMode;
import trees.*;

import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.*;
import java.lang.Math;

public class PlanNode extends Node{

    /*-----------------------------------------------------------------------*/
    /* MARK: Data Members */
    /*-----------------------------------------------------------------------*/

    /* Decision Tree related */
    private J48 decisionTree;
    private FastVector atts;
    private Instances data;
    private int countdownToRebuild;
    private final int buildThreshold = 1;
    
    /* Stores the world state for when this plan was called last */
    private String[] lastState;

    /* Configuration parameters */
    private UpdateMode update_mode;
    private PlanSelectMode select_mode;
    private RunMode run_mode;

    /* Stores information about each success/fail experience */
    private Hashtable experiences;

    /* BUL related parameters */
    private int stableK;
    private double stableE;

    /* Used to indicate that this is a "dummy" plan that should be selected
     * when no other plans apply, for instance, if all plans have an 
     * expectation of success lower than a set threshold.
     */
    public boolean isFailedThresholdHandler;
    
    /* Coverage related. See also parent Node class for more. */
    private Hashtable isDirty;
    
    /* Complexity-based confidence measure related */
    private Hashtable complexity;
    private Hashtable decay;
    private Hashtable domainDecay;
    private double domainComplexityDecayMultiplier;

    /* Failure-recovery related */
    private boolean handledRepostedGoal;

    /* Todo: Vars to sort out */
    public int successfulChildren;
    GoalNode topGoal;
    
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Constructors */
    /*-----------------------------------------------------------------------*/
    public PlanNode(String pname, 
                    FastVector attributes,
                    UpdateMode update_mode, 
                    PlanSelectMode select_mode,
                    RunMode run_mode,
                    double epsilion, 
                    int kStable, 
                    boolean isFTH, 
                    Logger logger){
        super(pname, logger);
        atts = attributes;
        lastState = null;
        experiences = new Hashtable();
        countdownToRebuild = buildThreshold;
        this.update_mode = update_mode;
        this.select_mode = select_mode;
        this.run_mode = run_mode;
        stableK = kStable;
        stableE = epsilion;
        successfulChildren = 0;
        topGoal = null;
        isDirty = new Hashtable();
        decay = new Hashtable();
        domainDecay = new Hashtable();
        complexity = new Hashtable();
        isFailedThresholdHandler = isFTH;
        data = new Instances(pname, atts, 0);
        data.setClassIndex(atts.size()-1);
        domainComplexityDecayMultiplier = 0.0;
        handledRepostedGoal = false;
        try{
            decisionTree = new J48();
            String[] options = new String[4];
            //options[0] = "-U";
            options[0] = "-C";
            options[1] = "0.5";
            options[2] = "-M";
            options[3] = "1";
            decisionTree.setOptions(options);
        }
        catch(Exception e){
            System.err.println("error with weka when creating the decision tree \n" + e);
            System.exit(9);
        }
    }

    /*-----------------------------------------------------------------------*/
    /* MARK: Access Members */
    /*-----------------------------------------------------------------------*/

    public String[] lastState() { return lastState; }
    public void setLastState(String[] val) { this.lastState = val; }
    
    public FastVector attributesForDT() { return atts; }
    public void setAttributesForDT(FastVector val) { this.atts = val; }
        
    public double stableE() { return stableE; }
    public void setStableE(int e) { this.stableE = e; }

    public int stableK() { return stableK; }
    public void setStableK(int k) { this.stableK = k; }

    public void setTopGoal(GoalNode val) {
        topGoal = val;
    }
    public GoalNode topGoal() {
        return topGoal;
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
        for(int i = 0; this.children.size()>i; i++)
        {
            Node tempNode = (Node)this.children.elementAt(i);
            if(tempNode instanceof GoalNode)
            {
                GoalNode thisNode = (GoalNode)tempNode;
                thisNode.resetSuccessfulChildren();
            }
        }
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
        if(this.successfulChildren==this.getNumberOfChildren())
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public boolean determineSuccessful()
    {
        if(this.children.size()>0)
        {
            for(int i =0; this.children.size()>i;i++)
            {
                GoalNode thisNode = (GoalNode)this.children.elementAt(i);
                if(!thisNode.isSuccessful())
                {
                    this.successful = false;
                    return false;
                }
            }
            this.successful = true;
            return true;
        }
        else
        {
            return this.successful;
        }
    }
    
    
    public void resetLastState()
    {
        this.lastState = null;
    }
    
    /**
     * record method called each time a plan succeeds or fails. The method
     * retrieves the state of the world at the moment when the plan was
     * chosen, and records the feature vector with the outcome.  In this 
     * implementation, the feature vector with the outcome is used as a key 
     * in a hash table and the number of times this feature vector occured
     * is stored as the data, and then, it is translated to the format accepted
     * by weka
     * @param res
     */
    public void record(boolean res) {   
        record(res, 0 /*depth*/, (topGoal!=null) /*isRoot*/, 1.0/*failureNodeComplexity*/);
    }
    
    public void record(boolean res, int depth, boolean isRoot, double failureNodeComplexity)
    {
        if (this.isFailedThresholdHandler) {
            logger.writeLog("Skipped recording for failed threshold handler plan "+this.getItem()+" in state "+this.stringOfLastState());
            return;
        }
        
        logger.writeLog("Plan "+this.getItem()+" is recording result "+(res?"(+)":"(-)")+" for state "+this.stringOfLastState());

        /* In failure recovery mode, if a subplan at any level below handled a 
         * reposted subgoal then that means that the initial choice had failed
         * so for this parent the only sensible choice is to record a failure.
         * Do this only for non-leaf plans.
         */
        if ((run_mode == RunMode.FAILURE_RECOVERY) && this.isPropagatingFailure() && (this.getNumberOfChildren() > 0)) {
            logger.writeLog("Forced result to (-) for plan "+this.getItem()+" since a subgoal initially failed and was reposted." );
            res = false;
        }
        
        /* TODO: coverage calc for reposted goals is very tricky. 
         * does not work as it is now.
         * Needs more working out.
         */
        
        if(!res && (update_mode == UpdateMode.STABLE) && (this.getNumberOfChildren() > 0)) {
            //we failed...
            if(this.childrenStable()) {
                //children are stable so we should update..
                logger.writeLog("Plan "+this.getItem()+" has "+this.children.size()+" stable children for state "+this.stringOfLastState());
            } else {
                //children are unstable and we have failed. do not update. This pulls us out of the recording process.
                logger.writeLog("Plan "+this.getItem()+" will not update on this failure since some of its "+this.children.size()+" children are unstable in state "+this.stringOfLastState());
                return;
            }
        }
        
        /* Now, record the experience */
        String memoryKey = this.stringOfState(lastState);
        Experience thisMemory;
        String newold;
        if (this.experiences.containsKey(memoryKey)) {
            thisMemory = (Experience)this.experiences.get(memoryKey);
            newold = "EXISTING";
        } else {
            thisMemory = new Experience(logger);
            newold = "NEW";
        }
        if(res) {
            if (thisMemory.getNumberOfFailures() > 0) {
                /* Always reset the failures count. At this point
                 * we consider this a succesful plan for this state, 
                 * so we will count any future failures afresh.
                 */
                thisMemory.setNumberOfSuccesses(1);
                thisMemory.setNumberOfAttempts(1);
                logger.writeLog("Plan "+this.getItem()+" had success in state "+memoryKey+" so will reset all previous failures now.");
            }
            thisMemory.incrementAttempts();
            thisMemory.incrementSuccesses();
        } else {
            thisMemory.incrementAttempts();
        }
        thisMemory.updateProbability();
        
        if(!(update_mode == UpdateMode.STABLE) || 
           ((update_mode == UpdateMode.STABLE) && (res || this.childrenStable()))) {
            thisMemory.setState(lastState);
        }
        
        this.experiences.put(memoryKey, thisMemory);
        
        if (select_mode == PlanSelectMode.COVERAGE) {
            if (!this.handledRepostedGoal || (topGoal() != null)) {
                /* Mark only the first tried plans as dirty for coverage calculation */
                isDirty.put(depth,true);
            }
            /* Now calculate the coverage and store back */
            int pathsAdded;
            String dp = getDirtyPath(depth);
            logger.writeLog("Plan "+this.getItem()+" has dirty path ["+dp+"] in state "+memoryKey);
            if ((pathsAdded = thisMemory.addCoverage(deadPaths(dp,depth),depth)) > 0) {
                logger.writeLog("Plan "+this.getItem()+" added "+pathsAdded+" dead paths in state "+memoryKey+". Coverage is now "+thisMemory.coverage(depth)+"/"+getPaths(depth));
            }
            this.experiences.put(memoryKey, thisMemory);
        }

        if (isRoot) {
            this.clearDirtyPath(depth);
        }

        if ((select_mode == PlanSelectMode.CONFIDENCE) && (getConfidence(depth,lastState) != 1.0)/*stop decay when full confidence*/) {
            //double f = (this.getNumberOfChildren() == 0) ? getComplexity(depth) : Math.max(1,getComplexity(depth)-failureNodeComplexity);
            double f = Math.max(2,getComplexity(depth)-failureNodeComplexity);
            setComplexityDecay(memoryKey, depth, getComplexityDecay(memoryKey,depth) * (1.0 - (1.0/(Math.log(f)/Math.log(2.0)))));
            setDomainDecay(depth, getDomainDecay(depth) * domainComplexityDecayMultiplier);
            logger.writeLog("Plan "+this.getItem()+" at depth "+depth
                            +" with complexity "+ getComplexity(depth)
                            +" calculated new decays="+getComplexityDecay(memoryKey, depth)
                            +","+getDomainDecay(depth));
        }
        logger.writeLog("Plan "+this.getItem()+" recorded "+(res?"(+)":"(-)")+" experience ["+thisMemory.toString()+"] to "+newold+" key "+memoryKey);

        /* Rebuild the DT if we have had sufficient new experiences*/
        countdownToRebuild--;
        try{
            if (countdownToRebuild == 0) {
                countdownToRebuild = buildThreshold;
                buildDataset();
                decisionTree.buildClassifier(data);
            }
        }
        catch(Exception e){
            System.err.println("Something went wrong during the construction of the decision tree\n" + e);
            System.exit(9);
        }
        

    }
    
    public boolean isPropagatingFailure() {
        int nChildren = this.children.size();
        for(int j = 0; nChildren > j; j++) {
            GoalNode thisNode = (GoalNode)this.children.elementAt(j);
            if (thisNode.isPropagatingFailure()) {
                return true;
            }
        }
        return handledRepostedGoal;
    }
    
    public Vector getPathStringsMatching(String str, int depth) {
        Object[] allPaths = getPathStrings(depth).toArray();
        Vector matches = new Vector();
        String[] splits = str.split("\\*");
        for (int i = 0; i < allPaths.length; i++) {
            String fullPath = (String)(allPaths[i]);
            String path = fullPath;
            int sindex = -1;
            for (int j = 0; j < splits.length; j++) {
                sindex = path.indexOf(splits[j]);
                if (sindex != 0 /* must match the start of string */) {
                    break;
                } else {
                    path = path.substring(sindex+splits[j].length());
                }
            }
            if (sindex != -1) {
                matches.add(fullPath);
                //logger.writeLog("Plan "+this.getItem()+" matched ["+fullPath+"]");
            }
        }
        logger.writeLog("Plan "+this.getItem()+"("+depth+") found "+matches.size()+" matches for ["+str+"]");
        return matches;
    }

    public String pathID() {
        return (String)(getItem())+":";
    }
    
    public boolean isDirty(int depth) {
        return isDirty.containsKey(depth);
    }
    public void setDirty(int depth) {
        isDirty.put(depth,true);
    }
    
    public boolean handledRepostedGoal() {
        return handledRepostedGoal;
    }
    public void setHandledRepostedGoal(boolean val) {
        handledRepostedGoal = val;
    }
    
    public void setDecayMultiplier(double d) {
        domainComplexityDecayMultiplier = d;
    }
    
    public void setComplexityDecay(String memoryKey, int depth, double d) {
        Experience thisMemory = (experiences.containsKey(memoryKey)) ? (Experience)(experiences.get(memoryKey)) : new Experience(logger);
        thisMemory.setDecay(depth,d);
        experiences.put(memoryKey,thisMemory);
    }
    
    public double getComplexityDecay(String memoryKey, int depth) {
        Experience thisMemory = (experiences.containsKey(memoryKey)) ? (Experience)(experiences.get(memoryKey)) : new Experience(logger);
        return thisMemory.getDecay(depth);
    }

    public void setDomainDecay(int depth, double d) {
        domainDecay.put(depth,d);
    }
    
    public double getDomainDecay(int depth) {
        return domainDecay.containsKey(depth) ? ((Double)(domainDecay.get(depth))).doubleValue() : 1.0;
    }
    
    public void setComplexity(int depth, double c) {
        complexity.put(depth,c);
    }

    public void setComplexity(double c) {
        complexity.put(0,c);
    }
    
    public double getComplexity(int depth) {
        return complexity.containsKey(depth) ? ((Double)(complexity.get(depth))).doubleValue() : 1.0;
    }

    public double getConfidence(int depth, String[] state) {
        if (this.isFailedThresholdHandler) {
            return 1.0;
        }
        
        /* If we have succeeded in this state before then base confidence in the
         * ratio success/total.
         */
        String memoryKey = this.stringOfState(state);
        double cdecay = getComplexityDecay(memoryKey,depth);
        if ((this.experiences.size() > 0) && this.experiences.containsKey(memoryKey)) {
            Experience thisMemory = (Experience)this.experiences.get(memoryKey);
            if (thisMemory.getNumberOfSuccesses() > 0) {
                double c = ((double)thisMemory.getNumberOfSuccesses())/thisMemory.getNumberOfAttempts();
                logger.writeLog("Plan "+this.getItem()+" using experience confidence c="+c);
                return c;
            } else {
                cdecay = thisMemory.getDecay(depth);
                logger.writeLog("Plan "+this.getItem()+" using experience decay="+cdecay);
            }
        }
        /* Otherwise confidence is a decaying proposition */
        logger.writeLog("Plan "+this.getItem()+" using confidence c="+(1 - cdecay)+"*"+(1 - getDomainDecay(depth)));
        return (1 - cdecay) * (1 - getDomainDecay(depth));
    }
    
    public void clearDirty(int depth) {
        isDirty.remove(depth);
    }
        
    public Vector pathStrings(int depth) {
        return (pathStrings.containsKey(depth)) ? (Vector)(pathStrings.get(depth)) : new Vector();
    }
    public void setPathStrings(int depth, Vector val) {
        pathStrings.put(depth,val);
    }
    public void addPathString(int depth, String path) {
        Vector v = (pathStrings.containsKey(depth)) ? (Vector)(pathStrings.get(depth)) : new Vector();
        v.add(path);
        pathStrings.put(depth,v);
    }
    
    public Vector getPathStrings(int depth) {
        if (pathStringsKnown(depth)) {
            return pathStrings(depth);
        }
        int nChildren = this.children.size();
        boolean terminate = true;
        for(int j = 0; nChildren > j; j++) {
            GoalNode thisNode = (GoalNode)children.elementAt(j);
            int d = (thisNode != parent) ? 0 : (depth >= 0) ? depth-1: 0;
            if (d != -1) {
                terminate = false;
                Vector goalPathStrings = thisNode.getPathStrings(d);
                if (pathStrings(depth).size() == 0) {
                    setPathStrings(depth,goalPathStrings); 
                } else {
                    Vector oldPathStrings = pathStrings(depth);
                    setPathStrings(depth,new Vector());
                    for (int k = 0; k < oldPathStrings.size(); k++) {
                        for (int m = 0; m < goalPathStrings.size(); m++) {
                            String str = pathID()+(String)(oldPathStrings.elementAt(k))+(String)(goalPathStrings.elementAt(m));
                            addPathString(depth,str);
                        }
                    }
                }
            }
            logger.writeLog("Plan "+this.getItem()+"("+depth+") built "+pathStrings(depth).size()+" paths");
        }
        if(nChildren == 0 || terminate) {
            addPathString(depth,pathID());
        }
        Vector ps = pathStrings(depth);
        int sz = ps.size();
        logger.writeLog("Plan "+this.getItem()+"("+depth+") has "+sz+" paths");
        /*
        for (int k = 0; k < sz; k++) {
            logger.writeLog("Plan "+this.getItem()+" has path ["+ps.elementAt(k)+"]");
        }
        */
        setPathStringsKnown(depth);
        return pathStrings(depth);
    }

    public long getPaths() {
        return getPaths(0);
    }
    
    public long getPaths(int depth) {
        if (pathsKnown(depth)) {
            return paths(depth);
        }
        long p = 1;
        int nChildren = this.children.size();
        if(nChildren > 0) {
            for(int j = 0; nChildren > j; j++) {
                GoalNode thisNode = (GoalNode)this.children.elementAt(j);
                int d = (thisNode != parent) ? 0 : (depth >= 0) ? depth-1: 0;
                if (d != -1) {
                    p *= thisNode.getPaths(d);
                }
            }    
        }
        setPathsKnown(depth);
        setPaths(depth,p);
        return p;
    }

    public String getDirtyPath(int depth) {
        if (!isDirty(depth)) {
            return "";
        }
        String path = pathID();
        int nChildren = this.children.size();
        for(int j = 0; nChildren > j; j++) {
            GoalNode thisNode = (GoalNode)this.children.elementAt(j);
            int d = (thisNode != parent) ? 0 : (depth >= 0) ? depth-1: 0;
            if (d != -1) {
                path += thisNode.getDirtyPath(d);
            }
        }
        return path;
    }

    public void clearDirtyPath(int depth) {
        int nChildren = this.children.size();
        for(int j = 0; nChildren > j; j++) {
            GoalNode thisNode = (GoalNode)this.children.elementAt(j);
            int d = (thisNode != parent) ? 0 : (depth >= 0) ? depth-1: 0;
            if (d != -1) {
                thisNode.clearDirtyPath(d);
            }
        }
        isDirty.remove(depth);
        handledRepostedGoal = false;
    }

    public String[] deadPaths(String prefix, int depth) {
        Vector matches = getPathStringsMatching(prefix,depth);
        return (String[])matches.toArray(new String[0]);
    }
    
    public double getCoverageRatio(String[] state, int depth) {
        String memoryKey = this.stringOfState(state);
        double coverage = 0.0;
        if (this.isFailedThresholdHandler) {
            coverage = 1.0;
            logger.writeLog("Failed threshold handler plan "+this.getItem()+" is using full coverage="+coverage+" in state "+memoryKey);
            return coverage;
        }
        int c = 0;
        if (this.experiences.size() > 0) {
            Object[] earr = this.experiences.values().toArray();
            for (int i = 0; i < earr.length; i ++) {
                Experience m = (Experience)earr[i];
                c += m.coverage(depth);
            }
            coverage = (double)(c)/(getPaths(depth)*earr.length);
            logger.writeLog("Plan "+this.getItem()+" is averaging previous worlds coverage="+((double)((int)(coverage*10000)))/10000);
        }
        return coverage;
    }

    public int getCoverage(String[] state) {
        return this.getCoverage(state, 0/*depth*/);
    }    
    
    public int getCoverage(String[] state, int depth) {
        String memoryKey = this.stringOfState(state);
        int coverage = 0;
        if (this.isFailedThresholdHandler) {
            coverage = (int)getPaths(depth); 
            logger.writeLog("Failed threshold handler plan "+this.getItem()+" is using full coverage="+coverage+" in state "+memoryKey);
            return coverage;
        }
        if (this.experiences.size() > 0) {
            if (this.experiences.containsKey(memoryKey)) {
                Experience thisMemory = (Experience)this.experiences.get(memoryKey);
                coverage = thisMemory.coverage(depth);
            } else {
                coverage = 0;
                logger.writeLog("Plan "+this.getItem()+" has not seen state "+memoryKey+" before, so will use coverage c="+coverage);
            }
        } else {
            coverage = 0;
            logger.writeLog("Plan "+this.getItem()+" has not seen state "+memoryKey+" before and has no previous state to leverage, so will use coverage c="+coverage);
        }
        return coverage;
    }    
    
    
    /**
     * sets the state of the world each time the plan is chosen
     * @param state state of the world when the plan is chosen
     */
    public void setLastInstance(String[] state)
    {
        lastState = new String[atts.size()-1];
        for (int i=0;i<state.length;i++)
        {
            lastState[i]= state[i];
        }
    }
    
    /**
     * get the percentage of instances that positively classify the last state
     * using the decision tree.
     * @return  the percentage of instances in the corresponding
     * leaf node in the decision tree
     * 
     */
    public double[] getProbability(String [] state)
    {
        /* The following commented out code may be use to test the 
         * performance without DT use for benchmarking purposes. 
         * Basically, instead of using the DT we can estimate 
         * the probability of success based on the number 
         * of experienced successes per total number of tries.
         * If we haven't seen this world then use default 0.5.
         */
        if (false) {
            double[] val = new double[2];
            String memoryKey = this.stringOfState(state);
            if ((this.experiences.size() > 0) && this.experiences.containsKey(memoryKey)) {
                Experience thisMemory = (Experience)this.experiences.get(memoryKey);
                double prob = thisMemory.getNumberOfSuccesses()/thisMemory.getNumberOfAttempts();
                val[0] = prob;
                val[1] = 1-prob;
                logger.writeLog("Plan "+this.getItem()+" has seen state "+memoryKey+" before so using experience probability="+prob);
                return val;
            }
            val[0] = 0.5;
            val[1] = 0.5;
            return val;
        }

        /* If we have had no experience yet then use the default probability */
        if (experiences.size() == 0) {
            double[] val = new double[2];
            val[0] = 0.5;
            val[1] = 0.5;
            return val;
        }
        
        /* Calculate the probability of success based on the DT classififcation.
         * The value returned represents the 'correct/total' ratio of instances
         * classified in the DT leaf node where this world will be classified.
         */
        double[] val = null;
        Instance instance = new Instance(atts.size()-1);
        instance.setDataset(data);
        for (int i=0;i<state.length;i++){
            if (state[i].equals("true"))
                instance.setValue(((Attribute) atts.elementAt(i)),"T");
            else if (state[i].equals("false"))
                instance.setValue(((Attribute) atts.elementAt(i)),"F");
            else {
                Attribute att = (Attribute) atts.elementAt(i);
                if (att.isNumeric()) {
                    instance.setValue(att,Double.parseDouble(state[i]));
                } else {
                    instance.setValue(att,state[i]);
                }
            }
        }
        try{
            val = decisionTree.distributionForInstance(instance);
            logger.writeLog("Plan "+this.getItem()+" is USING DT with "+data.numInstances()+" instances in state "+this.stringOfState(state)+". Probability of success p="+((double)((int)(val[0]*10000)))/10000);
            
        }
        catch(Exception e){
            System.err.println("something went wrong when the instance was classified \n" +e);
            System.exit(9);
        }
        return val;
    }
    
    public double[] getProbability()
    {
        return this.getProbability(this.lastState);
    }    
    
    
    /** 
     * print the decision tree and info about it.
     */
    public void printDT(){
        System.out.println("************> Outcomes for plan "+ getItem());
        System.out.println("number of instances : " + data.numInstances());
        System.out.println(decisionTree);
    }
        
    public String stringOfLastState() {
        return (lastState == null) ? "" : stringOfState(lastState);
    }
    
    public String stringOfState(String[] state) {
        String s = "";
        for(int i = 0; state.length>i;i++) {
            if(state[i]==null) {
                return null;
            } else if(state[i].equals("true")) {
                s += "1";
            } else if(state[i].equals("false")) {
                s += "0";
            } else {
                s += state[i];
            }
        }
        return s;    
    }
    
    
    /**
     * Used to determine if this node can be considered stable.
     * Perhaps will require a parameter indicating state as a node could be stable for some, yet no other states.
     * @return True if we consider this node stable, which ATM implies that its sub nodes are stable.
     */
    public boolean isStable(String[] state)
    {
        String lastStateReference = this.stringOfState(state);
        if (this.isFailedThresholdHandler) {
            logger.writeLog("Failed threshold handler plan "+this.getItem()+" assumed stable in state "+lastStateReference);
            return true;
        }
        //System.out.println("Checking Stability");
        boolean stable = true;
        
        logger.writeLog("Plan "+this.getItem()+" is checking stability for state "+lastStateReference);
        
        if (this.isSuccessful(state)) {
            //We have succeeded in this state before so consider this stable
            logger.writeLog("Plan "+this.getItem()+" has succeeded in state "+lastStateReference+" before, so will consider it stable");
            return true;
        }
        if(lastStateReference!=null)
        {   
            if(experiences.containsKey(lastStateReference))
            {
                //We have a record of this state being used before
                Experience thisRecord  = (Experience)experiences.get(lastStateReference);
                if(this.stableK()>thisRecord.getNumberOfAttempts())
                {
                    //We haven't yet tried enough attempts to be considered stable.
                    logger.writeLog("Plan "+this.getItem()+" does not satisfy minimum number of attempts "+thisRecord.getNumberOfAttempts()+">=K("+this.stableK()+")");
                    return false;
                }
                if(thisRecord.getDeltaProbability()>this.stableE())
                {
                    //Our rate of change is to high to be considered stable
                    logger.writeLog("Plan "+this.getItem()+" does not satisfy change in probability "+thisRecord.getDeltaProbability()+"<=E("+this.stableE()+")");
                    return false;
                }
                logger.writeLog("Plan "+this.getItem()+
                                " satisfies minimum number of attempts "+thisRecord.getNumberOfAttempts()+">=K("+this.stableK()+")"+
                                " and change in probability "+thisRecord.getDeltaProbability()+"<=E("+this.stableE()+")"+
                                " for state "+lastStateReference);
            }
            else
            {
                //We've not seen this record before...
                logger.writeLog("Plan "+this.getItem()+" has never seen state "+lastStateReference+" before");
                return false;
            }
        }
        else
        {
            //We have no last state...
            //We can't be considered stable if we've never been used before!
            return false; 
        }
        //I believe this now comes out in the wash....
        //We are stable. What about our sub-nodes?
        //if(!this.childrenStable())
        //{
        //return false;
        //}
        
        return stable;
    }
    
    public boolean isSuccessful(String[] state)
    {
        String lastStateReference = this.stringOfState(state);

        if(lastStateReference!=null)
        {   
            if(experiences.containsKey(lastStateReference))
            {
                //We have a record of this state being used before
                Experience thisRecord  = (Experience)experiences.get(lastStateReference);
                if (thisRecord.getNumberOfSuccesses() > 0) {
                    //We have succeeded in this state before
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean childrenStable()
    {
        if(this.children.size()>0)
        {
            for(int j = 0; this.children.size()>j;j++)
            {
                GoalNode thisNode = (GoalNode)this.children.elementAt(j);
                logger.writeLog("Plan "+this.getItem()+" is checking child goal "+thisNode.getItem()+" for stability for state "+this.stringOfState(lastState));
                String[] goalLastState = thisNode.calculateEndStateFrom(lastState);
                if(!thisNode.isStable(goalLastState))
                {
                    return false;
                } else if (!thisNode.isSuccessful(goalLastState) && ((j+1)<this.children.size())) {
                    /* Fine, so this child goal node is stable, but if this child
                     * always fails then there is no point continuing because 
                     * the next child (also a goal) would've never been tried 
                     * (since we would've stopped at the failure of this goal node)
                     * and therefore will always fail the stability test.
                     * The reality is that this plan is in fact stable, because
                     * there is nothing else to try in this state.
                     */
    				logger.writeLog("Goal "+thisNode.getItem()+" has previously failed in state "+this.stringOfState(goalLastState)+" so forego remaining children and consider us ("+this.getItem()+") stable");
                    return true;
                }
            }   
            return true;
        }
        else
        {
            //This node has no children.
            //No children means stable as far as children are concerned.
            return true;
        }
        
    }
    
    public Enumeration stableEnumeration()
    {
        return this.experiences.elements();
    }
    
    public void setStableE(double stableE) {
        this.stableE = stableE;
    }
    
    
    public boolean equals(Object obj)
    {
        if(obj instanceof PlanNode)
        {
            PlanNode compareTo = (PlanNode)obj;
            
            if(compareTo.getItem().equals(this.getItem()))
            {
                return true;
            }
        }
        return false;
    }

    public String toString(int pad)
    {
        String out="";
        for (int i=0;i<pad;i++)
        {
            out+=" ";
        }
        
        out += item.toString()+"\n";
        
        for (int i = 0; children.size()>i;i++)
        {
            out+=((Node) children.elementAt(i)).toString(pad+5); 
        }
        out += " prop memory size: "+this.experiences.size();
        return out;
    }
    
    private void buildDataset() {
         int added = 0;
        /* Delete previous entries, we will recreate the dataset now */
        data.delete();
        /* Add each experience to the dataset */
        for (Object val : experiences.values() ) {
            Experience thisMemory = (Experience)val;
            Instance instance = new Instance(atts.size());
            instance.setDataset(data);
            String[] state = thisMemory.getState();
            for (int i=0;i<state.length;i++){
                if (state[i].equals("true"))
                    instance.setValue(((Attribute) atts.elementAt(i)),"T");
                else if (state[i].equals("false"))
                    instance.setValue(((Attribute) atts.elementAt(i)),"F");
                else {
                    Attribute att = (Attribute) atts.elementAt(i);
                    if (att.isNumeric()) {
                        instance.setValue(att,Double.parseDouble(state[i]));
                    } else {
                        instance.setValue(att,state[i]);
                    }
                }
            }
            int successes = thisMemory.getNumberOfSuccesses();
            int failures = thisMemory.getNumberOfFailures();
            if (successes > 0) {
                Instance instance2 = (Instance)(instance.copy());
                instance2.setValue(((Attribute) atts.elementAt(lastState.length)),"+");
                instance2.setWeight(successes);
                data.add(instance2);
                added++;
            }  
            if (failures > 0) {
                instance.setValue(((Attribute) atts.elementAt(lastState.length)),"-");
                instance.setWeight(failures);
                data.add(instance);
                added++;
            }
        }
        logger.writeLog("Plan "+this.getItem()+" built training data set with "+added+" samples");
    }
}
