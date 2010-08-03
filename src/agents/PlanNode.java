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
    private final int buildThreshold = 2;
    
    /* Stores the world state for when this plan was called last */
    private String[] lastState;

    /* Configuration parameters */
    private UpdateMode update_mode;
    private PlanSelectMode select_mode;
    private RunMode run_mode;

    /* Stores information about each success/fail experience */
    private Hashtable experiences;

    private Vector<String> stateHistory;
    private Hashtable stateHash;
    private int stateHistoryWindow = 0;

    /* BUL related parameters */
    private int stableK;
    private double stableE;
    private int stableW;

    /* Used to indicate that this is a "dummy" plan that should be selected
     * when no other plans apply, for instance, if all plans have an 
     * expectation of success lower than a set threshold.
     */
    private boolean isFailedThresholdHandler;
    
    /* Coverage related. See also parent Node class for more. */
    private Hashtable isDirty;
    
    /* Complexity-based confidence measure related */
    private Hashtable complexity;
    private Hashtable decay;
    private Hashtable domainDecay;
    private double domainComplexityDecayMultiplier;

    /* !!!: Vars to sort out */

    /* Triggers cleanup operations if this is a root level plan.
     * This should instead be handled by the caller.
     */
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
                    int wStable, 
                    int wStateHistory,
                    boolean isFTH, 
                    Logger logger){
        super(pname, logger);
        atts = attributes;
        lastState = null;
        experiences = new Hashtable();
        stateHistory = new Vector<String>();
        stateHash = new Hashtable();
        countdownToRebuild = buildThreshold;
        this.update_mode = update_mode;
        this.select_mode = select_mode;
        this.run_mode = run_mode;
        stableK = kStable;
        stableE = epsilion;
        stableW = wStable;
        stateHistoryWindow = wStateHistory;
        topGoal = null;
        isDirty = new Hashtable();
        decay = new Hashtable();
        domainDecay = new Hashtable();
        complexity = new Hashtable();
        isFailedThresholdHandler = isFTH;
        data = new Instances(pname, atts, 0);
        data.setClassIndex(atts.size()-1);
        domainComplexityDecayMultiplier = 0.0;
        try{
            decisionTree = new J48();
            String[] options = new String[1];
            options[0] = "-U";
            //options[0] = "-C";
            //options[1] = "0.5";
            //options[2] = "-M";
            //options[3] = "1";
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
    public void setLastState(String[] val) { lastState = val; }
    
    public FastVector attributesForDT() { return atts; }
    public void setAttributesForDT(FastVector val) { atts = val; }
        
    public double stableE() { return stableE; }
    public void setStableE(double e) { stableE = e; }
    
    public int stableK() { return stableK; }
    public void setStableK(int k) { stableK = k; }

    public int stableW() { return stableW; }
    public void setStableW(int w) { stableW = w; }

    public int stateHistoryWindow() { return stateHistoryWindow; }
    public void setStateHistoryWindow(int w) { stateHistoryWindow = w; }

    public GoalNode topGoal() { return topGoal; }
    public void setTopGoal(GoalNode val) { topGoal = val; }
    
    public boolean isFailedThresholdHandler() { return isFailedThresholdHandler; }
    public void setFailedThresholdHandler(boolean val) { isFailedThresholdHandler = val; }

    public void resetLastState() { this.lastState = null; }
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Decision Tree related */
    /*-----------------------------------------------------------------------*/

    private void buildDataset() {
        int added = 0;
        String str = "";
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
                //str += "Plan "+name()+" added training instance: "+instance2+ "\n";
                added++;
            }  
            if ((failures > 0) && thisMemory.useForLearning()) {
                instance.setValue(((Attribute) atts.elementAt(lastState.length)),"-");
                instance.setWeight(failures);
                data.add(instance);
                //str += "Plan "+name()+" added training instance: "+instance+ "\n";
                added++;
            }
        }
        if (added > 0) {
            str += "Plan "+name()+" built training data set with "+added+" samples";
            /* Note: Logging out the training samples takes a lot of time and 
             * log space. Commenting them out above speeds things up significantly.
             */
            logger.writeLog(str);
        }
    }

    public double[] getProbability() {
        return this.getProbability(this.lastState);
    }    

    /**
     * get the percentage of instances that positively classify the last state
     * using the decision tree.
     * @return  the percentage of instances in the corresponding
     * leaf node in the decision tree
     * 
     */
    public double[] getProbability(String [] state) {
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
                logger.writeLog("Plan "+name()+" has seen state "+memoryKey+" before so using experience probability="+prob);
                return val;
            }
            val[0] = 0.5;
            val[1] = 0.5;
            return val;
        }
        
        /* If we have had no experience yet then use the default probability */
        if (data.numInstances() == 0) {
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
            logger.writeLog("Plan "+name()+" is USING DT with "+data.numInstances()+" instances in state "+this.stringOfState(state)+". Probability of success p="+((double)((int)(val[0]*10000)))/10000);
            
        }
        catch(Exception e){
            System.err.println("something went wrong when the instance was classified \n" +e);
            System.exit(9);
        }
        return val;
    }
    
    /** 
     * print the decision tree and info about it.
     */
    public void printDT(){
        System.out.println(getDT());
    }
    
    public String getDT() {
        String str = "";
        str += "************> Outcomes for plan "+ name() + "\n";
        str += "number of instances : " + data.numInstances() + "\n";
        str += decisionTree;
        return str;
    }

    /* Caller responsibilities:
     * 1. Ensure this is not a failed threshold handler plan
     * 2. If using BUL recording, to ensure that active execution trace
     *    below this plan is stable.
     */
    public void record(boolean res) {
        int[] val = new int[2];
        val[0] = 1;
        val[1] = 1;
        record(res, val/*traceStability*/);
    }
    public void record(boolean res, int[] traceStability) {   
        record(res, traceStability, 0 /*depth*/, (topGoal!=null) /*isRoot*/, 1.0/*failureNodeComplexity*/);
    }
    public void record(boolean res,
                       int[] traceStability,
                       int depth, 
                       boolean isRoot, 
                       double failureNodeComplexity)
    {
        boolean isStableBelow = (traceStability[0] != 0) && (traceStability[0] == traceStability[1]);
        
        logger.writeLog("Plan "+name()+" is recording result "+(res?"(+)":"(-)")+" for state "+this.stringOfLastState());
        
        /* !!!: Coverage calculation for reposted goals is very tricky. 
         * does not work as it is now.
         * Needs more working out.
         */
        
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
        if(res && (thisMemory.getNumberOfSuccesses() == 0) && isRoot) {
            /* This is the first time we resolved the top level goal */
            logger.writeLog("Agent recorded first success for top level goal (state "+this.stringOfLastState()+", plan "+this.name()+")");
        }
        thisMemory.setState(lastState);
        thisMemory.incrementAttempts();
        if(res) {
            thisMemory.incrementSuccesses();
        }
                
        /* Set if the trace was stable below.
         * Done for all modes since we use this info for 
         * stability-based confidence calculation. */
        thisMemory.setHasStableChildren(isStableBelow);

        /* Reset failures on first success */
        if(res && (thisMemory.getNumberOfSuccesses() == 1)) {
            /* This is the first tme we succeeded in this state. 
             * At this point we consider this a succesful plan for this state, 
             * so we will count any future failures afresh.
             */
            thisMemory.setNumberOfSuccesses(1);
            thisMemory.setNumberOfAttempts(1);
            logger.writeLog("Plan "+name()+" had success in state "+memoryKey+" so will reset all previous failures now.");
        }
        
        /* Now update the rate of change of success.
         * Done for all modes since we use this info for 
         * stability-based confidence calculation. 
         */
        thisMemory.updateProbability();
        
        /* Finally, save this experience */
        this.experiences.put(memoryKey, thisMemory);
        
        /* BUL: Check if this experience should be used in learning and store back */
        if ((update_mode == UpdateMode.STABLE) && !res && (!isStableBelow || !isStable())) {
            thisMemory.setUseForLearning(false);
        } else {
            thisMemory.setUseForLearning(true);
        }
        this.experiences.put(memoryKey, thisMemory);
        
        /* Now calculate experience stability and store back.
         * This is done for all modes.
         */
        //int stability = (traceStability[0] != traceStability[1]) ? traceStability[0] : 
        //res ? 1 : 
        //isStable() ? 1 : 
        //0;
        
        int nStable = 0;
        if (isLeaf()) {
            nStable = res ? 1 : isStable() ? 1 : 0;
        } else {
            nStable = res ? traceStability[1]+1 : isStableBelow ? traceStability[1]+1 : traceStability[0];
        }
        int nTotal = isLeaf() ? 1 : traceStability[1]/*plans below*/ + 1/*this plan*/;
        thisMemory.addStableHistory(nStable, nTotal);
        logger.writeLog("Plan "+name()+" recorded stability "+nStable+"/"+nTotal+" for state "+this.stringOfLastState());
        this.experiences.put(memoryKey, thisMemory);
        
        /* Coverage related updates */
        if (select_mode == PlanSelectMode.COVERAGE) {
            if (topGoal() != null) {
                /* Mark only the first tried plans as dirty for coverage calculation */
                isDirty.put(depth,true);
            }
            /* Now calculate the coverage and store back */
            int pathsAdded;
            String dp = getDirtyPath(depth);
            logger.writeLog("Plan "+name()+" has dirty path ["+dp+"] in state "+memoryKey);
            if ((pathsAdded = thisMemory.addCoverage(deadPaths(dp,depth),depth)) > 0) {
                logger.writeLog("Plan "+name()+" added "+pathsAdded+" dead paths in state "+memoryKey+". Coverage is now "+thisMemory.coverage(depth)+"/"+getPaths(depth));
            }
            this.experiences.put(memoryKey, thisMemory);
        }
        
        /* !!!: This should be handled by the caller instead */
        if (isRoot) {
            this.clearDirtyPath(depth);
        }
        
        /* Confidence related updates */
        addStateHistory(memoryKey);
        if (false/*superceded by stability-based confidence*/ && (select_mode == PlanSelectMode.CONFIDENCE) && (getConfidence(depth,lastState) != 1.0)/*stop decay when full confidence*/) {
            double f = Math.max(2,getComplexity(depth)-failureNodeComplexity);
            setComplexityDecay(memoryKey, depth, getComplexityDecay(memoryKey,depth) * (1.0 - (1.0/(Math.log(f)/Math.log(2.0)))));
            setDomainDecay(depth, getDomainDecay(depth) * domainComplexityDecayMultiplier);
            logger.writeLog("Plan "+name()+" at depth "+depth
                            +" with complexity "+ getComplexity(depth)
                            +" calculated new decays="+getComplexityDecay(memoryKey, depth)
                            +","+getDomainDecay(depth));
        }
        
        logger.writeLog("Plan "+name()+" recorded "+(res?"(+)":"(-)")+" experience ["+thisMemory.toString()+"] to "+newold+" key "+memoryKey);
        
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
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - BUL related */
    /*-----------------------------------------------------------------------*/

    public boolean isStable() {
        return isStable(lastState);
    }
    public boolean isStable(String[] state)
    {
        boolean stable = false;
        String lastStateReference = this.stringOfState(state);

        if(isFailedThresholdHandler) {
            logger.writeLog("Plan "+name()+" IS STABLE for state "+lastStateReference+": It is a dummy plan");
            stable = true;
        } else if(experiences.containsKey(lastStateReference)) {
            /*We have a record of this state being used before */
            Experience thisRecord  = (Experience)experiences.get(lastStateReference);
            if (thisRecord.hasStableChildren()) {
                if(this.stableK()<=thisRecord.getNumberOfAttempts()) {
                    if(thisRecord.getDeltaProbability()<=this.stableE()) {
                        stable = true;
                        logger.writeLog("Plan "+name()+" is stable for state "+lastStateReference+":"+
                                        " number of attempts "+thisRecord.getNumberOfAttempts()+">=K("+this.stableK()+")"+
                                        " and change in probability "+thisRecord.getDeltaProbability()+"<=E("+this.stableE()+")");
                    } else {
                        logger.writeLog("Plan "+name()+" is NOT stable for state "+lastStateReference+": change in probability "+thisRecord.getDeltaProbability()+">E("+this.stableE()+")");
                    }
                } else {
                    logger.writeLog("Plan "+name()+" is NOT stable for state "+lastStateReference+": number of attempts "+thisRecord.getNumberOfAttempts()+"<K("+this.stableK()+")");
                }
            } else {
                logger.writeLog("Plan "+name()+" is NOT stable for state "+lastStateReference+": choices below are not stable");
            }
        } else {
            logger.writeLog("Plan "+name()+" is NOT stable for state "+lastStateReference+": has never witnessed this state before");
        }
        return stable;
    }

    public double averageExperiencedStability() {
        return averageExperiencedStability(lastState,stableW);
    }

    public double averageExperiencedStability(String[] state) {
        return averageExperiencedStability(state,stableW);
    }
    
    public double averageExperiencedStability(String[] state, int window) {
        double stable = 0.0;
        String lastStateReference = this.stringOfState(state);
        
        if(isFailedThresholdHandler) {
            logger.writeLog("Plan "+name()+" assumes average experience stability 1.0 for state "+lastStateReference+": It is a dummy plan");
            stable = 1.0;
        } else if(experiences.containsKey(lastStateReference)) {
            /*We have a record of this state being used before */
            Experience thisRecord  = (Experience)experiences.get(lastStateReference);
            stable = thisRecord.averageStability(window);
            logger.writeLog("Plan "+name()+" has average stability "+stable+" over last "+window+" experiences for state "+lastStateReference);
        } else {
            logger.writeLog("Plan "+name()+" assumes average experience stability 0.0 for state "+lastStateReference+": has never witnessed this state before");
            stable = 0.0;
        }
        return stable;
    }
    
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Coverage related */
    /*-----------------------------------------------------------------------*/

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
                //logger.writeLog("Plan "+name()+" matched ["+fullPath+"]");
            }
        }
        logger.writeLog("Plan "+name()+"("+depth+") found "+matches.size()+" matches for ["+str+"]");
        return matches;
    }
    
    public String pathID() {
        return (String)(name())+":";
    }
    
    public boolean isDirty(int depth) {
        return isDirty.containsKey(depth);
    }
    public void setDirty(int depth) {
        isDirty.put(depth,true);
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
            logger.writeLog("Plan "+name()+"("+depth+") built "+pathStrings(depth).size()+" paths");
        }
        if(nChildren == 0 || terminate) {
            addPathString(depth,pathID());
        }
        Vector ps = pathStrings(depth);
        int sz = ps.size();
        logger.writeLog("Plan "+name()+"("+depth+") has "+sz+" paths");
        /*
         for (int k = 0; k < sz; k++) {
         logger.writeLog("Plan "+name()+" has path ["+ps.elementAt(k)+"]");
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
            logger.writeLog("Failed threshold handler plan "+name()+" is using full coverage="+coverage+" in state "+memoryKey);
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
            logger.writeLog("Plan "+name()+" is averaging previous worlds coverage="+((double)((int)(coverage*10000)))/10000);
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
            logger.writeLog("Failed threshold handler plan "+name()+" is using full coverage="+coverage+" in state "+memoryKey);
            return coverage;
        }
        if (this.experiences.size() > 0) {
            if (this.experiences.containsKey(memoryKey)) {
                Experience thisMemory = (Experience)this.experiences.get(memoryKey);
                coverage = thisMemory.coverage(depth);
            } else {
                coverage = 0;
                logger.writeLog("Plan "+name()+" has not seen state "+memoryKey+" before, so will use coverage c="+coverage);
            }
        } else {
            coverage = 0;
            logger.writeLog("Plan "+name()+" has not seen state "+memoryKey+" before and has no previous state to leverage, so will use coverage c="+coverage);
        }
        return coverage;
    }    
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Confidence related */
    /*-----------------------------------------------------------------------*/

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
                logger.writeLog("Plan "+name()+" using experience confidence c="+c);
                return c;
            } else {
                cdecay = thisMemory.getDecay(depth);
                logger.writeLog("Plan "+name()+" using experience decay="+cdecay);
            }
        }
        /* Otherwise confidence is a decaying proposition */
        logger.writeLog("Plan "+name()+" using confidence c="+(1 - cdecay)+"*"+(1 - getDomainDecay(depth)));
        return (1 - cdecay) * (1 - getDomainDecay(depth));
    }
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Domain Confidence related */
    /*-----------------------------------------------------------------------*/
    
    public void addStateHistory(String state) {
        stateHistory.add(state);
        if (stateHistory.size() > stateHistoryWindow) {
            /* Pop the oldest element if we have exceeded window size 
             * and store it in the hash of old states. */
            String removed = stateHistory.remove(0);
            stateHash.put(removed, new Boolean(true));
        }
    }
    
    public double stateExperienceConfidence() {
        return stateExperienceConfidence(lastState);
    }
    public double stateExperienceConfidence(String[] state) {
        /* Param 'window' should not exceed stateHistory.size().
         * Returns the fraction of last 'window' states that have also
         * been witnessed prior to that.
         */
        String lastStateReference = this.stringOfState(state);
        if (this.experiences.containsKey(lastStateReference)) { return 1.0;}
        int sz = stateHistory.size();
        if (stateHistoryWindow < 1) { return 1.0;}
        if (stateHash.size() < 1) { return 0.0;}
        double sum = 0.0;
        int n = (sz < stateHistoryWindow) ? sz : stateHistoryWindow;
        String[] hist = stateHistory.toArray(new String[0]);
        for (int i = hist.length-1; i > hist.length-1-n; i--) {
            if (stateHash.containsKey(hist[i])) {
                sum ++;
            }
        }
        return sum/stateHistoryWindow;
    }
    
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Misc */
    /*-----------------------------------------------------------------------*/
    
    /**
     * sets the state of the world each time the plan is chosen
     * @param state state of the world when the plan is chosen
     */
    public void setLastInstance(String[] state) {
        lastState = new String[atts.size()-1];
        for (int i=0;i<state.length;i++) {
            lastState[i]= state[i];
        }
    }
    
    public String stringOfLastState() {
        return (lastState == null) ? "" : stringOfState(lastState);
    }
        
    public boolean isSuccessful(String[] state) {
        String lastStateReference = this.stringOfState(state);
        if(lastStateReference!=null) {
            if(experiences.containsKey(lastStateReference)) {
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
    
    public Enumeration stableEnumeration() {
        return this.experiences.elements();
    }
}
