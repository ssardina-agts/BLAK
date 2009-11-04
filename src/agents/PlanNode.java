package agents;

import agents.Config.UpdateMode;
import trees.*;

import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import weka.core.Instances;
import java.util.*;
import java.lang.Math;

/**
 * Handy piece of info. Item is used to hold an object which is usually the name of the node.
 *
 */

public class PlanNode extends Node{
    
    public int goal_id;
    public String name;
    Hashtable memory;
    public J48 decisionTree;
    String[] options;
    String[] lastState;
    UpdateMode update_mode;
    public int successfulChildren;
    private FastVector atts;
    private FastVector classVal;
    private FastVector boolVal;
    int numAttributes;
    int startToUseDT;
    int minNumInstances=100;
    Hashtable experiences;
    
    private int stableK;
    private double stableEpsilon;
    
    Instances data;
    
    GoalNode topGoal;
    public boolean isDirty;
    public boolean isFailedThresholdHandler;
    private Random rand;
    
    /*
    public boolean isDoStable() {
        return doStable;
    }
    public void setDoStable(boolean doStable) {
        this.doStable = doStable;
    }
    */
    public String[] getLastState() {
        return lastState;
    }
    
    public void setLastState(String[] lastState) {
        this.lastState = lastState;
    }
    
    public FastVector getAtts() {
        return atts;
    }
    
    public void setAtts(FastVector atts) {
        this.atts = atts;
    }
    
    public FastVector getClassVal() {
        return classVal;
    }
    
    public void setClassVal(FastVector classVal) {
        this.classVal = classVal;
    }
    
    public FastVector getBoolVal() {
        return boolVal;
    }
    
    public void setBoolVal(FastVector boolVal) {
        this.boolVal = boolVal;
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
    
    /**
     * Constructor
     * @param id
     * @param pname
     * @param attributes
     * @param classValue
     * @param booleanValue
     */
    public PlanNode(int id, String pname, FastVector attributes,
                    FastVector classValue, FastVector booleanValue, 
                    int minNumInst, UpdateMode update_mode,
                    double epsilion, int kStable, boolean isFTH, Logger logger){
        super(pname, logger);
        name = pname;
        goal_id = id;
        atts = attributes;
        classVal = classValue;
        boolVal = booleanValue;
        numAttributes = atts.size()-1;
        minNumInstances = minNumInst;
        lastState = null;
        memory = new Hashtable();
        experiences = new Hashtable();
        this.update_mode = update_mode;
        stableK = kStable;
        stableEpsilon = epsilion;
        successfulChildren = 0;
        topGoal = null;
        isDirty = false;
        isFailedThresholdHandler = isFTH;
        data = new Instances(name, atts, 0);
        data.setClassIndex(numAttributes);
        rand = new Random();
        try{
            decisionTree = new J48();
            options = new String[1];
            options[0] = "-U";            // unpruned tree
            ((J48)decisionTree).setOptions(options);     // set the options
        }
        catch(Exception e){
            System.err.println("error with weka when creating the decision tree \n" + e);
            System.exit(9);
        }
    }
    
    public void resetLastState()
    {
        this.lastState = null;
    }
    
    public int getMinNumInstances() {
        return minNumInstances;
    }
    
    public void setMinNumInstances(int minNumInstances) {
        this.minNumInstances = minNumInstances;
    }
    
    public int getGoalID()
    {
        return goal_id;
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
    public void record(boolean res)
    {
        if (this.isFailedThresholdHandler) {
            logger.writeLog("Skipped recording for failed threshold handler plan "+this.getItem()+" in state "+this.stringOfLastState());
            return;
        }
        logger.writeLog("Recording result for plan "+this.getItem()+" for state "+this.stringOfLastState());
        if(!res && (update_mode == UpdateMode.STABLE) && (this.getNumberOfChildren() > 0))
        {
            //we failed...
            if(this.childrenStable())
            {
                //children are stable so we should update..
                logger.writeLog("Plan "+this.getItem()+" has "+this.children.size()+" stable children for state "+this.stringOfLastState());
            }
            else
            {
                //children are unstable and we have failed. do not update. This pulls us out of the recording process.
                logger.writeLog("Plan "+this.getItem()+" will not update on this failure since some of its "+this.children.size()+" children are unstable in state "+this.stringOfLastState());
                return;
            }
        }
        
        if(!(update_mode == UpdateMode.STABLE))
        {
            //Stephane's code to record the instance for use with a  Decision Tree.
            //we aren't doing stable selection so record.....
            String record = "";
            for (int i=0;i<lastState.length-1;i++)
                record += lastState[i] + ",";
            record += lastState[lastState.length-1]+":";
            if (res)
            {
                record += "+";
                logger.writeLog("Plan "+this.getItem()+" is updating (+) result in state "+this.stringOfLastState());
            }
            else
            {
                record += "-";
                logger.writeLog("Plan "+this.getItem()+" is updating (-) result in state "+this.stringOfLastState());
            }
            Integer num = (Integer)memory.remove(record);
            if (num !=null)
                memory.put(record, new Integer(num.intValue()+1));
            else
                memory.put(record, new Integer(1));
            
            // prepare the data to be used for a decision tree
            Instance instance = new Instance(numAttributes+1);
            instance.setDataset(data);
            for (int i=0;i<lastState.length;i++){
                if (lastState[i].equals("true"))
                    instance.setValue(((Attribute) atts.elementAt(i)),"T");
                else if (lastState[i].equals("false"))
                    instance.setValue(((Attribute) atts.elementAt(i)),"F");
                else {
                    Attribute att = (Attribute) atts.elementAt(i);
                    if (att.isNumeric()) {
                        instance.setValue(att,Double.parseDouble(lastState[i]));
                    } else {
                        instance.setValue(att,lastState[i]);
                    }
                }
            }
            if (res) {
                instance.setValue(((Attribute) atts.elementAt(lastState.length)),"+");
                instance.setWeight(1000);
                data.add(instance);
                //data.resampleWithWeights(rand);
            } else {
                instance.setValue(((Attribute) atts.elementAt(lastState.length)),"-");
                data.add(instance);
            }
        }
        else if((update_mode == UpdateMode.STABLE) && res)
        {
            //Stephane's code to record the instance for use with a  Decision Tree.     
            //We are doing stable selection, and had a success, so record
            String record = "";
            for (int i=0;i<lastState.length-1;i++)
                record += lastState[i] + ",";
            record += lastState[lastState.length-1]+":";
            if (res)
            {
                record += "+";
                logger.writeLog("Plan "+this.getItem()+" is updating (+) result for state "+this.stringOfLastState()+" as it was successful");
            }
            else
            {
                //should not reach this point, but it is left in case
                record += "-";
                logger.writeLog("ERROR: PlanNode Stable Updates, Negative Results, Should not reach here: "+this.getItem()+" is updating even though it failed. It must have stable (or zero!) children. State was: "+this.stringOfLastState());
            }
            Integer num = (Integer)memory.remove(record);
            if (num !=null)
                memory.put(record, new Integer(num.intValue()+1));
            else
                memory.put(record, new Integer(1));
            
            // prepare the data to be used for a decision tree
            Instance instance = new Instance(numAttributes+1);
            instance.setDataset(data);
            for (int i=0;i<lastState.length;i++){
                if (lastState[i].equals("true"))
                    instance.setValue(((Attribute) atts.elementAt(i)),"T");
                else if (lastState[i].equals("false"))
                    instance.setValue(((Attribute) atts.elementAt(i)),"F");
                else {
                    Attribute att = (Attribute) atts.elementAt(i);
                    if (att.isNumeric()) {
                        instance.setValue(att,Double.parseDouble(lastState[i]));
                    } else {
                        instance.setValue(att,lastState[i]);
                    }
                }
            }
            if (res) {
                instance.setValue(((Attribute) atts.elementAt(lastState.length)),"+");
                instance.setWeight(1000);
                data.add(instance);
                //data.resampleWithWeights(rand);
            } else {
                instance.setValue(((Attribute) atts.elementAt(lastState.length)),"-");
                data.add(instance);
            }
        }
        else if((update_mode == UpdateMode.STABLE) && this.childrenStable())
        {
            //Stephane's code to record the instance for use with a  Decision Tree.     
            //We are doing stable and our children are stable. So record.....
            //If we have no children then we are stable and can therefore update in this situation.
            String record = "";
            for (int i=0;i<lastState.length-1;i++)
                record += lastState[i] + ",";
            record += lastState[lastState.length-1]+":";
            if (res)
            {
                record += "+";
                logger.writeLog("Plan "+this.getItem()+" is updating (+) result for state "+this.stringOfLastState()+" since all of its "+children.size()+" children are stable");
            }
            else
            {
                record += "-";
                logger.writeLog("Plan "+this.getItem()+" is updating (-) result for state "+this.stringOfLastState()+" since all of its "+children.size()+" children are stable");
            }
            Integer num = (Integer)memory.remove(record);
            if (num !=null)
                memory.put(record, new Integer(num.intValue()+1));
            else
                memory.put(record, new Integer(1));
            
            // prepare the data to be used for a decision tree
            Instance instance = new Instance(numAttributes+1);
            instance.setDataset(data);
            for (int i=0;i<lastState.length;i++){
                if (lastState[i].equals("true"))
                    instance.setValue(((Attribute) atts.elementAt(i)),"T");
                else if (lastState[i].equals("false"))
                    instance.setValue(((Attribute) atts.elementAt(i)),"F");
                else {
                    Attribute att = (Attribute) atts.elementAt(i);
                    if (att.isNumeric()) {
                        instance.setValue(att,Double.parseDouble(lastState[i]));
                    } else {
                        instance.setValue(att,lastState[i]);
                    }
                }
            }
            if (res) {
                instance.setValue(((Attribute) atts.elementAt(lastState.length)),"+");
                instance.setWeight(1000);
                data.add(instance);
                //data.resampleWithWeights(rand);
            } else {
                instance.setValue(((Attribute) atts.elementAt(lastState.length)),"-");
                data.add(instance);
            }
        }
        
        /* Now record the experience */
        String memoryKey = this.stringOfState(lastState);
        Experience thisMemory = (this.experiences.containsKey(memoryKey)) ? (Experience)this.experiences.get(memoryKey) : new Experience(logger);
        String newold = (this.experiences.containsKey(memoryKey)) ? "EXISTING" : "NEW";
        if(res) {
            thisMemory.incrementAttempts();
            thisMemory.incrementSuccesses();
        } else {
            thisMemory.incrementAttempts();
        }
        thisMemory.updateProbability();
        
        /* Store this experience first, so that coverage can find it */ 
        this.experiences.put(memoryKey, thisMemory);
        /* Now calculate the coverage and store back */
        isDirty = true;
        String dpath;
        if (thisMemory.addCoverage(dpath = getDirtyPath())) {
            logger.writeLog("Plan "+this.getItem()+" added covered path ["+dpath+"] in state "+memoryKey+". Coverage is now "+thisMemory.coverage()+"/"+getPaths());
        }
        int pathsAdded;
        if ((pathsAdded = thisMemory.addCoverage(deadPaths(lastState))) > 0) {
            logger.writeLog("Plan "+this.getItem()+" added "+pathsAdded+" dead paths in state "+memoryKey+". Coverage is now "+thisMemory.coverage()+"/"+getPaths());
        }
        if (this.topGoal != null) {
            this.clearDirtyPath();
        }
        if (false && this.topGoal != null) {
            /* This is a root level plan so manually update siblings */
            logger.writeLog("Plan "+this.getItem()+" is a top level goal so will initialise coverage calculation for siblings");
            topGoal.calculateCoverage(lastState);
        }
        logger.writeLog("Plan "+this.getItem()+" is writing "+thisMemory.toString()+" to "+newold+" key "+memoryKey);
        this.experiences.put(memoryKey, thisMemory);
    }
    
    public void setTopGoal(GoalNode val) {
        topGoal = val;
    }

    public Vector getPathStringsMatching(String str) {
        Object[] allPaths = getPathStrings().toArray();
        Vector matches = new Vector();
        for (int i = 0; i < allPaths.length; i++) {
            String path = (String)(allPaths[i]);
            if (path.indexOf(str) != -1) {
                //logger.writeLog("Plan "+this.getItem()+" matched path ["+path+"]");
                matches.add(path);
            }
        }
        return matches;
    }

    public Vector getPathStrings() {
        if (pathStringsKnown) {
            return pathStrings;
        }
        int nChildren = this.children.size();
        if(nChildren == 0) {
            pathStrings.add(getItem()+":");
            return pathStrings;
        }
        for(int j = 0; nChildren > j; j++) {
            GoalNode thisNode = (GoalNode)children.elementAt(j);
            Vector goalPathStrings = thisNode.getPathStrings();
            if (pathStrings.size() == 0) {
                pathStrings = goalPathStrings; 
            } else {
                Vector oldPathStrings = pathStrings;
                pathStrings = new Vector();
                for (int k = 0; k < oldPathStrings.size(); k++) {
                    for (int m = 0; m < goalPathStrings.size(); m++) {
                        String str = (String)(oldPathStrings.elementAt(k))+(String)(goalPathStrings.elementAt(m));
                        pathStrings.add(str);
                    }
                }
            }
        }
        pathStringsKnown = true;
        return pathStrings;
    }
    
    public int getPaths() {
        if (this.pathsKnown) {
            return this.paths;
        }
        int p = 1;
        int nChildren = this.children.size();
        if(nChildren > 0) {
            for(int j = 0; nChildren > j; j++) {
                GoalNode thisNode = (GoalNode)this.children.elementAt(j);
                p *= thisNode.getPaths();
            }    
        }
        this.pathsKnown = true;
        this.paths = p;
        return p;
    }

    public String getDirtyPath() {
        String path = "";
        int append = 0;
        int nChildren = this.children.size();
        if(nChildren > 0) {
            for(int j = 0; nChildren > j; j++) {
                GoalNode thisNode = (GoalNode)this.children.elementAt(j);
                String dpath = thisNode.getDirtyPath();
                if (dpath != null) {
                    path += dpath;
                    append = j;
                }
            }
        }
        if (nChildren == 0 && isDirty) {
            path += this.getItem() + ":";
        } else if (append == nChildren-1) {
            /* do nothing */
        } else {
            path = null;
        }
        return path;
    }

    public void clearDirtyPath() {
        int nChildren = this.children.size();
        for(int j = 0; nChildren > j; j++) {
            GoalNode thisNode = (GoalNode)this.children.elementAt(j);
            thisNode.clearDirtyPath();
        }
        isDirty = false;
    }
    
    public String[] deadPaths(String[] state) {
        if (this.isFailedThresholdHandler) {
            return new String[0];
        }
        Vector paths = new Vector();
        int nChildren = this.children.size();
        String stateStr = stringOfState(state);
        if (this.experiences.size() > 0 && this.experiences.containsKey(stateStr) && nChildren>0) {
            Experience thisMemory = (Experience)this.experiences.get(stateStr);
            for(int j = 0; j < nChildren; j++) {
                GoalNode thisNode = (GoalNode)this.children.elementAt(j);
                state = thisNode.resultingState(state);
                stateStr = stringOfState(state);
                if (j < nChildren-1) {
                    for (int k = 0; k < thisNode.children.size(); k++) {
                        PlanNode thisPlan = (PlanNode)thisNode.children.elementAt(k);
                        if (!thisPlan.isFailedThresholdHandler) {
                            if (thisPlan.children.size() == 0) {
                                double cRatio = (double)(thisPlan.getCoverage(state))/thisPlan.getPaths();
                                if (cRatio==1.0 && !thisPlan.isSuccessful(state)) {
                                    /* This subplan did not succeed in the given state
                                     * AND it has full coverage. So
                                     * all subsequent subgoals (i.e paths with thisPlan in it) 
                                     * can be considered dead
                                     * since they will never execute in this state.
                                     */
                                    logger.writeLog("Child sub-plan "+thisPlan.getItem()+" is fully covered but has never succeeded in state "+stateStr+", so will consider all paths associated with this plan fully covered");
                                    Vector matches = getPathStringsMatching((String)(thisPlan.getItem())+":");
                                    for (int m = 0; m < matches.size(); m++) {
                                        paths.add((String)(matches.elementAt(m)));
                                    }
                                }
                            } else {
                                String[] dpaths = thisPlan.deadPaths(state);
                                for (int n = 0; n < dpaths.length; n++) {
                                    Vector matches = getPathStringsMatching(dpaths[n]);
                                    for (int m = 0; m < matches.size(); m++) {
                                        paths.add((String)(matches.elementAt(m)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return (String[])paths.toArray(new String[0]);
    }
    
    public double getAverageCoverageInAllStates() {
        double coverage = 0.0;
        if (this.isFailedThresholdHandler) {
            coverage = (double)getPaths(); 
            logger.writeLog("Failed threshold handler plan "+this.getItem()+" is using average coverage="+coverage);
            return coverage;
        }
        if (this.experiences.size() > 0) {
            Object[] earr = this.experiences.values().toArray();
            for (int i = 0; i < earr.length; i ++) {
                Experience m = (Experience)earr[i];
                coverage += m.coverage();
            }
            coverage /= earr.length;
            logger.writeLog("Plan "+this.getItem()+" is averaging previous worlds coverage="+((double)((int)(coverage*10000)))/10000);
        }
        return coverage;
    }

    public int getCoverage() {
        return this.getCoverage(this.lastState);
    }    

    public int getCoverage(String[] state) {
        String memoryKey = this.stringOfState(state);
        int coverage = 0;
        if (this.isFailedThresholdHandler) {
            coverage = getPaths(); 
            logger.writeLog("Failed threshold handler plan "+this.getItem()+" is using full coverage="+coverage+" in state "+memoryKey);
            return coverage;
        }
        if (this.experiences.size() > 0) {
            if (this.experiences.containsKey(memoryKey)) {
                Experience thisMemory = (Experience)this.experiences.get(memoryKey);
                coverage = thisMemory.coverage();
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
    
    protected int calculateCoverage(String[] state) {
        String stateStr = this.stringOfState(state);
        int coverage = 0;        
        if (this.isFailedThresholdHandler) {
            coverage = getPaths(); 
            logger.writeLog("Failed threshold handler plan "+this.getItem()+" is using full coverage="+coverage+" in state "+stateStr);
            return coverage;
        }
        int nChildren = this.children.size();
        if(nChildren > 0) {
            int cCoverage = 0;
            logger.writeLog("Plan "+this.getItem()+" is checking children for coverage in state "+stateStr);
            logger.indentRight();
            for(int j = 0; nChildren > j; j++) {
                GoalNode thisNode = (GoalNode)this.children.elementAt(j);
                int c = thisNode.calculateCoverage(state);
                double cRatio = (double)c/thisNode.getPaths();
                cCoverage += c;
                logger.writeLog("Child goal "+thisNode.getItem()+" has coverage="+c+" in state "+stateStr);
                if (cRatio==1.0 && !thisNode.isSuccessful(state)) {
                    /* This subgoal did not succeed in this state
                     * AND it has full coverage then
                     * all subsequent subgoals can be considered
                     * covered since they will never execute
                     * in this state.
                     */
                    logger.writeLog("Child goal "+thisNode.getItem()+" is fully covered but has never succeeded in state "+stateStr+", so will consider remaining subgoals also fully covered");
                    for (int k = j+1; k < nChildren; k++) {
                        thisNode = (GoalNode)this.children.elementAt(k);
                        cCoverage += thisNode.getPaths();
                    }
                    break;
                }
            }
            logger.indentLeft();
            coverage = cCoverage;
        } else {
            /* Plan has no children, so calculate coverage for this
             * node only. We do not care about stochasticity here.
             */
            coverage = (this.experiences.containsKey(stateStr)) ? 1 : 0;
            logger.writeLog("Plan "+this.getItem()+" is a leaf plan and has coverage="+coverage+" in state "+stateStr);
        }
        logger.writeLog("Plan "+this.getItem()+" calculated coverage="+coverage+"/"+this.getPaths()+" in state "+stateStr);
        return coverage;
    }
    
    /**
     * sets the state of the world each time the plan is chosen
     * @param state state of the world when the plan is chosen
     */
    public void setLastInstance(String[] state)
    {
        lastState = new String[numAttributes];
        for (int i=0;i<state.length;i++)
        {
            lastState[i]= state[i];
        }
    }
    
    /**
     * get the classification of the last state using the decision tree
     * @return true if the percentage of instances in the corresponding
     * leaf node in the decision tree is higher than 50%
     */
    public boolean getClassification(){
        Instance instance = new Instance(numAttributes);
        instance.setDataset(data);
        for (int i=0;i<lastState.length;i++){
            if (lastState[i].equals("true"))
                instance.setValue(((Attribute) atts.elementAt(i)),"T");
            else if (lastState[i].equals("false"))
                instance.setValue(((Attribute) atts.elementAt(i)),"F");
            else {
                Attribute att = (Attribute) atts.elementAt(i);
                if (att.isNumeric()) {
                    instance.setValue(att,Double.parseDouble(lastState[i]));
                } else {
                    instance.setValue(att,lastState[i]);
                }
            }
        }
        double val = 0;
        try{
            val = decisionTree.classifyInstance(instance);
        }
        catch(Exception e){
            System.err.println("something went wrong when the instance was classified \n" +e);
            System.exit(9);
        }
        if (val > 0.5)
            return false;
        else
            return true;
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
        double[] val = null;
        if (useDT(goal_id)){
            Instance instance = new Instance(numAttributes);
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
        System.out.println("************> Outcomes for plan "+  name 
                           + " built at iteration "+ startToUseDT);
        System.out.println("number of instances : " + data.numInstances());
        System.out.println(decisionTree);
        
    }
    
    public String getDT()
    {
        String returnString = "************> Outcomes for plan "+  name+ " built at iteration "+ startToUseDT+"\n";
        returnString +="number of instances : " + data.numInstances()+"\n";
        returnString +=decisionTree;
        return returnString;
    }
    
    /**
     * criterion that decides whether the decision tree should be used.  this 
     * method calls the criterion to decide whether the subtree are correct
     * @param it
     * @return
     */
    public boolean useDT(int it){
        if (data.numInstances() >= minNumInstances){
            if (startToUseDT ==0){
                startToUseDT = it;
                logger.writeLog("Plan "+this.getItem()+" is OK to use DT since minimum number of instances "+data.numInstances()+">=M("+minNumInstances+")");
            }
            try{
                decisionTree.buildClassifier(data);
            }
            catch(Exception e){
                System.err.println("Something went wrong during the construction of the decision tree\n" + e);
                System.exit(9);
            }
            return true;
        } else {
            return false;
        }
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
                if(this.getStableK()>thisRecord.getNumberOfAttempts())
                {
                    //We haven't yet tried enough attempts to be considered stable.
                    logger.writeLog("Plan "+this.getItem()+" does not satisfy minimum number of attempts "+thisRecord.getNumberOfAttempts()+">=K("+this.getStableK()+")");
                    return false;
                }
                if(thisRecord.getDeltaProbability()>this.getStableEpsilon())
                {
                    //Our rate of change is to high to be considered stable
                    logger.writeLog("Plan "+this.getItem()+" does not satisfy change in probability "+thisRecord.getDeltaProbability()+"<=E("+this.getStableEpsilon()+")");
                    return false;
                }
                logger.writeLog("Plan "+this.getItem()+
                                " satisfies minimum number of attempts "+thisRecord.getNumberOfAttempts()+">=K("+this.getStableK()+")"+
                                " and change in probability "+thisRecord.getDeltaProbability()+"<=E("+this.getStableEpsilon()+")"+
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
                Node thisNode = (Node)this.children.elementAt(j);
                logger.writeLog("Plan "+this.getItem()+" is checking child goal "+thisNode.getItem()+" for stability for state "+this.stringOfState(lastState));
                if(!thisNode.isStable(lastState))
                {
                    return false;
                } else if (!thisNode.isSuccessful(lastState)) {
                    /* Fine, so this child goal node is stable, but if this child
                     * always fails then there is no point continuing because 
                     * the next child (also a goal) would've never been tried 
                     * (since we would've stopped at the failure of this goal node)
                     * and therefore will always fail the stability test.
                     * The reality is that this plan is in fact stable, because
                     * there is nothing else to try in this state.
                     */
    				logger.writeLog("Goal "+thisNode.getItem()+" has previously failed in state "+this.stringOfState(lastState)+" so forego remaining children and consider us ("+this.getItem()+") stable");
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
    
    public Enumeration getStableEnumeration()
    {
        return this.experiences.elements();
    }
    
    /** criterion to decide whether I trust the subtrees below.  This version
     * only looks whether the decision trees of the plan-nodes below have been
     *  built and contain enough instances.
     * @return true when I consider that the subtree below are accurate
     */
    public boolean subTreeOK(){
        // check whether the decisicion tree for this node has enough instances.
        if (is_DT_Accurate()){
            /* for each sub-goal posted $sg$, check if *all* plan that satisfy 
             * $sg$ have a decision tree with enough instances.
             */
            int index_sg=0;
            boolean keepCheckingGoals = true;
            //System.out.println("Node "+ this.getClass() +" "+this.name + " "
            //      + data.numInstances() + " instances");
            while (index_sg<children.size() && keepCheckingGoals){
                //  it must be a GoalNode, representing either a subgoal or an action
                int index_sp=0;
                if (children.get(index_sg) instanceof GoalNode){            
                    GoalNode subgoal = (GoalNode) children.get(index_sg);
                    // for all plan that satisfy the subgoal, recursively check 
                    // whether the subtree is ok 
                    boolean keepCheckingPlans = true;
                    // if it is an action, there will not be any children
                    while (index_sp<subgoal.children.size() && keepCheckingPlans){
                        // it must be a plan node!
                        if (subgoal.children.get(index_sp) instanceof PlanNode){
                            PlanNode subplan = (PlanNode) subgoal.children.get(index_sp);
                            if (!subplan.subTreeOK())
                                keepCheckingPlans = false;
                        }
                        else{
                            System.err.println("we should have a plan node but we have an instance of "
                                               +children.get(index_sp).getClass()+"\n"+children.get(index_sp));
                            System.exit(9);
                        }
                        index_sp++;
                    }
                    if (!keepCheckingPlans)
                        keepCheckingGoals = false;
                }
                else{
                    System.err.println("we should have a goal node but we have an instance of "
                                       +children.get(index_sp).getClass()+"\n"+children.get(index_sp));
                    System.exit(9);
                }
                index_sg++;
            }//end if checking goals
            //if (keepCheckingGoals)
            //  System.out.println("Use the decision tree for Plan "+name);
            return keepCheckingGoals;
        }//end if data.numInstance > min 
        else
            return false;
    }
    
    public boolean is_DT_Accurate(){
        return data.numInstances() >= minNumInstances;
    }
    
    public void setStableEpsilon(double stableEpsilon) {
        this.stableEpsilon = stableEpsilon;
    }
    
    public double getStableEpsilon() {
        return stableEpsilon;
    }
    
    public void setStableK(int stableK) {
        this.stableK = stableK;
    }
    
    public int getStableK() {
        return stableK;
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
    
    public String stringBitSet(BitSet bs)
    {
        String returnString = "";
        for(int i= 0 ; atts.size()-1>i;i++)
        {
            if(bs.get(i)==true)
            {
                returnString +="1";
            }
            else if (bs.get(i)==false)
            {
                returnString +="0";
            }
        }
        return returnString;
    }
    
    public void classifyAllWorlds(Vector bitSetCollection)
    {
        /*
        int maxWorlds = bitSetCollection.size();
        String str;
        if (maxWorlds > 1024) {
            maxWorlds = 1024;
            str = "#Number of worlds: " + bitSetCollection.size() + "\n";
            str += "#Generation of classifications for all worlds will take too long.\n";
            str += "#Showing only the first " + maxWorlds + " classifications below.\n";
            writeCsv(str, "dt."+this.getItem());
        }
        for(int j = 0; maxWorlds>j;j++)
        {
            BitSet tempBitSet = (BitSet)bitSetCollection.elementAt(j);
            double[] val = null;
            if (useDT(goal_id))
            {
                Instance instance = new Instance(numAttributes);
                instance.setDataset(data);
                for (int i=0;i<atts.size()-1;i++)
                {
                    if (tempBitSet.get(i))
                    {
                        instance.setValue(((Attribute) atts.elementAt(i)),"T");
                    }
                    else if (!tempBitSet.get(i))
                    {
                        instance.setValue(((Attribute) atts.elementAt(i)),"F");
                    }
                }
                try
                {
                    val = (update_mode == UpdateMode.STABLE)?((Bagging)decisionTree).distributionForInstance(instance):((J48)decisionTree).distributionForInstance(instance);
                    writeCsv(j+","+val[0]+","+val[1], "dt."+this.getItem());
                }
                catch(Exception e)
                {
                    System.err.println("something went wrong when the instance was classified \n" +e); // TODO: Fix for Stable
                }
            }
            else
            {
                writeCsv("No DT available", "dt."+this.getItem());
            }
        }
        */
    }
    
    public void referenceAllWorlds(Vector bitSetCollection)
    {
        /*
        int maxWorlds = bitSetCollection.size();
        String str;
        if (maxWorlds > 1024) {
            maxWorlds = 1024;
            str = "#Number of worlds: " + bitSetCollection.size() + "\n";
            str += "#Generation of all worlds will take too long.\n";
            str += "#Showing only the first " + maxWorlds + " worlds below.\n";
            logger.writeLog(str, "worlds-key");
        }
        for(int j = 0; maxWorlds>j;j++)
        {
            BitSet tempBitSet = (BitSet)bitSetCollection.elementAt(j);
            logger.writeLog(j+"="+stringBitSet(tempBitSet), "worlds-key");
        }
        */
    }

    int hamming (String str1, String str2) {
        char[] s1 = str1.toCharArray();
        char[] s2 = str2.toCharArray();
        int m = (s1.length == s2.length) ? s1.length : Math.min(s1.length,s2.length);
        int c = 0;
        for (int i = 0; i < m; i++)
            if(s1[i] != s2[i]) {
                c++;
            }
        return c;
    }
}
