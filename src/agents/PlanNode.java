package agents;
import trees.*;

import java.util.Hashtable;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import weka.core.Instances;
import java.util.*;

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
    boolean doStable;
    public int successfulChildren;
    private FastVector atts;
    private FastVector classVal;
    private FastVector boolVal;
    int numAttributes;
    private boolean waitForSubTree = true;
    int startToUseDT;
    int minNumInstances=100;
    Hashtable probabilityMemory;
    public String targetDir;
	
    private int stableK;
    private double stableEpsilon;
    
    
 
    
    public boolean isDoStable() {
		return doStable;
	}

	public void setDoStable(boolean doStable) {
		this.doStable = doStable;
	}

	public String[] getLastState() {
		return lastState;
	}

	public void setLastState(String[] lastState) {
		this.lastState = lastState;
	}
	
	
	Instances data;
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
    		boolean waitST, int minNumInst, boolean doStablePlanning,
    		double epsilion, int kStable, String outputDir){
	super(pname);
	

	
	name = pname;
	goal_id = id;
	atts = attributes;
	classVal = classValue;
	boolVal = booleanValue;
	numAttributes = atts.size()-1;
	waitForSubTree = waitST;
	minNumInstances = minNumInst;
	lastState = new String[numAttributes];
	memory = new Hashtable();
	probabilityMemory = new Hashtable();
	this.doStable = doStablePlanning;
	stableK = kStable;
    stableEpsilon = epsilion;
    successfulChildren = 0;
	targetDir = outputDir;
    
	
	data = new Instances(name, atts, 0);
	data.setClassIndex(numAttributes);
	try{
	    decisionTree = new J48();
	    options = new String[1];
	    options[0] = "-U";            // unpruned tree
	    decisionTree = new J48();
	    decisionTree.setOptions(options);     // set the options
	}
	catch(Exception e){
		System.err.println("error with weka when creating the decision tree \n" + e);
		System.exit(9);
	}
    }

    public boolean isWaitForSubTree() {
		return waitForSubTree;
	}

	public void setWaitForSubTree(boolean waitForSubTree) {
		this.waitForSubTree = waitForSubTree;
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
	writeLog("Record Method for PlanNode: "+this.getItem()+". Last state was: "+this.stringOfLastState(), "Stability-Updates");
    if(!res && this.doStable)
	{
		//we failed...
		if(this.childrenStable())
		{
			//children are stable so we should update..
			writeLog("PlanNode: "+this.getItem()+" has stable children. Children size: "+this.children.size()+". State was: "+this.stringOfLastState(), "Stability-Updates");
		}
		else
		{
			//children are unstable and we have failed. do not update. This pulls us out of the recording process.
			writeLog("PlanNode: "+this.getItem()+" is not updating as we are handling a failure, and it's children are unstable. State was: "+this.stringOfLastState(), "Stability-Updates");
			return;
		}
	}
	
    if(!this.doStable)
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
			writeLog("PlanNode, No Stable Updates: "+this.getItem()+" is updating as  it was successful! State was: "+this.stringOfLastState(), "Stability-Updates");
		}
		else
		{
			record += "-";
			writeLog("PlanNode, No Stable Updates: "+this.getItem()+" is updating even though it failed. It must have stable (or zero!) children. State was: "+this.stringOfLastState(), "Stability-Updates");
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
		    else
			instance.setValue(((Attribute) atts.elementAt(i)),lastState[i]);
		}
		if (res)
		    instance.setValue(((Attribute) atts.elementAt(lastState.length)),"+");
		else
		    instance.setValue(((Attribute) atts.elementAt(lastState.length)),"-");
		data.add(instance);
    }
    else if(this.doStable && res)
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
			writeLog("PlanNode Stable Updates, Positive Result : "+this.getItem()+" is updating as  it was successful! State was: "+this.stringOfLastState(), "Stability-Updates");
		}
		else
		{
			//should not reach this point, but it is left in case
			record += "-";
			writeLog("PlanNode Stable Updates, Negative Results, should not reach: "+this.getItem()+" is updating even though it failed. It must have stable (or zero!) children. State was: "+this.stringOfLastState(), "Stability-Updates");
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
		    else
			instance.setValue(((Attribute) atts.elementAt(i)),lastState[i]);
		}
		if (res)
		    instance.setValue(((Attribute) atts.elementAt(lastState.length)),"+");
		else
		    instance.setValue(((Attribute) atts.elementAt(lastState.length)),"-");
		data.add(instance);


    }
    else if(this.doStable && this.childrenStable())
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
			writeLog("PlanNode Stable Updates, Children Stable, Positive result: "+this.getItem()+" is updating as  it was successful! State was: "+this.stringOfLastState(), "Stability-Updates");
		}
		else
		{
			record += "-";
			writeLog("PlanNode Stable Updates, Children Stable Negative Result: "+this.getItem()+" is updating even though it failed. It must have stable (or zero!) children. State was: "+this.stringOfLastState(), "Stability-Updates");
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
		    else
			instance.setValue(((Attribute) atts.elementAt(i)),lastState[i]);
		}
		if (res)
		    instance.setValue(((Attribute) atts.elementAt(lastState.length)),"+");
		else
		    instance.setValue(((Attribute) atts.elementAt(lastState.length)),"-");
		data.add(instance);

		
    }
    
	//update the stable Probability selection.
	if(this.doStable)
	{

    	String memoryKey = this.stringOfState(lastState);
		if(this.probabilityMemory.containsKey(memoryKey))
		{
			StableMemory thisMemory = (StableMemory)this.probabilityMemory.get(memoryKey);
			
			if(res)
			{
				writeLog("-----------------------------------\nSuccessful.Incrementing Suc, Att", "NodeUpdates");
				thisMemory.incrementAttempts();
				thisMemory.incrementSuccesses();
			}
			else
			{
				writeLog("-----------------------------------\nFailure.Incrementing Att", "NodeUpdates");
				thisMemory.incrementAttempts();
			}
			writeLog("Updating numbers, Att", "NodeUpdates");
			thisMemory.updateProbability();
			writeLog("PlanNode: "+this.getItem()+" is writing "+thisMemory.toString(this.getStableK(), this.getStableEpsilon())+" to key: "+memoryKey+". We had already seen this state.", "Stability-Updates");
			this.probabilityMemory.put(memoryKey, thisMemory);
		}
		else
		{
			StableMemory thisMemory = new StableMemory(targetDir);
			if(res)
			{
				writeLog("-----------------------------------\nSuccessful.Incrementing Suc, Att", "NodeUpdates");
				thisMemory.incrementAttempts();
				thisMemory.incrementSuccesses();
			}
			else
			{
				writeLog("-----------------------------------\nFailure.Incrementing Att", "NodeUpdates");
				thisMemory.incrementAttempts();
			}
			writeLog("Updating numbers, Att", "NodeUpdates");
			thisMemory.updateProbability();
			writeLog("PlanNode: "+this.getItem()+" is writing "+thisMemory.toString(this.getStableK(), this.getStableEpsilon())+" to key: "+memoryKey+". We had NOT seen this state before.", "Stability-Updates");
			this.probabilityMemory.put(memoryKey, thisMemory);
		}
	}
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
	    else
		instance.setValue(((Attribute) atts.elementAt(i)),lastState[i]);
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
public double[] getProbability()
{
	/*if(doStable)
	{
		String memoryKey = this.stringOfState(lastState);
		if(this.probabilityMemory.containsKey(memoryKey))
		{
			StableMemory thisMemory = (StableMemory)this.probabilityMemory.get(memoryKey);
			double [] returnArray = {thisMemory.getPreviousProbability(), 1-thisMemory.getPreviousProbability()};
			System.out.println("Returning: "+returnArray[0]+" and: "+returnArray[1]);
			return returnArray;
		}
	}*/
	
	double[] val = null;
	if (useDT(goal_id)){
		Instance instance = new Instance(numAttributes);
		instance.setDataset(data);
		for (int i=0;i<lastState.length;i++){
			if (lastState[i].equals("true"))
				instance.setValue(((Attribute) atts.elementAt(i)),"T");
			else if (lastState[i].equals("false"))
				instance.setValue(((Attribute) atts.elementAt(i)),"F");
			else
				instance.setValue(((Attribute) atts.elementAt(i)),lastState[i]);
		}
		try{
			val = decisionTree.distributionForInstance(instance);
		}
		catch(Exception e){
			System.err.println("something went wrong when the instance was classified \n" +e);
			System.exit(9);
		}
	}
	
	/*String probDump  = "";
	for(int i = 0; val.length>i;i++)
	{
		probDump = probDump + val[i] + ",";
	}
	System.out.print("Plan Node:" +name+", goal id "+goal_id+", Prob Dump: "+probDump);
	if(this.isStable(lastState))
	{
		System.out.print(". I am considered stable though.\n");
		//System.exit(0);
	}
	else
	{
		System.out.print(". I am unstable.\n");
	}
	
	writeLog("PlanNode: "+this.getItem()+" Probability:"+probDump,"Stability-Updates");
	*/
	return val;

}

	/** 
	 * print the decision tree and info about it.
	 */
    public void printDT(){
	System.out.println("************> Outcomes for plan "+  name 
			+ "built at iteration "+ startToUseDT);
	System.out.println("number of instances : " + data.numInstances());
	System.out.println(decisionTree);

    }
    
    public String getDT()
    {
    	String returnString = "************> Outcomes for plan "+  name+ "built at iteration "+ startToUseDT+"\n";
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
    	
	boolean subtreeOK = true;
	if (waitForSubTree)
	    subtreeOK = subTreeOK();
	
	if (subtreeOK && data.numInstances() >= minNumInstances){
	    if (startToUseDT ==0){
	    	startToUseDT = it;
	    	System.out.println("--> Start to use the DT for plan " + name 
	    			+ " at iteration " + startToUseDT + " after having seen "
	    			+data.numInstances() + " instances");
	    	writeLog("--> Start to use the DT for plan " + name+ " at iteration " + startToUseDT + " after having seen "+data.numInstances() + " instances", "Stability-Updates");
	    }
	    try{
		//System.out.println("print data " + data);
	    	//this.writeLog("This Node: "+this.getItem()+" is refreshing its D-Tree", "Stability-Tree");
		decisionTree.buildClassifier(data);
	    }
	    catch(Exception e){
		System.err.println("Something went wrong during the construction of the decision tree\n" + e);
		System.exit(9);
	    }
	    return true;
	}
	else 
	    return false;
	    
    }
    
    public String stringOfLastState()
    {
    	String returnMe = "";
    	for(int i = 0; lastState.length>i;i++)
    	{
    		if(lastState[i]==null)
    		{
    			return null;
    		}
    		else if(lastState[i].equals("true"))
    		{
    			returnMe = returnMe+"1";
    		}
    		else if(lastState[i].equals("false"))
    		{
    			returnMe = returnMe+"0";
    		}
    	}
    	return returnMe;	
    }
    
    public String stringOfState(String[] state)
    {
    	String returnMe = "";
    	
    	for(int i = 0; state.length>i;i++)
    	{
    		if(state[i]==null)
    		{
    			return null;
    		}
    		if(state[i].equals("true"))
    		{
    			returnMe = returnMe+"1";
    		}
    		else if(state[i].equals("false"))
    		{
    			returnMe = returnMe+"0";
    		}
    	}
    	return returnMe;	
    }
    
    
    /**
     * Used to determine if this node can be considered stable.
     * Perhaps will require a parameter indicating state as a node could be stable for some, yet no other states.
     * @return True if we consider this node stable, which ATM implies that its sub nodes are stable.
     */
    public boolean isStable(String[] state)
    {
       	//System.out.println("Checking Stability");
    	boolean stable = true;
    	
    	String lastStateReference = this.stringOfState(state);
    	String stableMessage = "PlanNode: "+this.getItem()+" is checking stability for state: "+lastStateReference;
    	writeLog(stableMessage,"Stability-Updates");
    	
    	if(lastStateReference!=null)
    	{	
	    	if(probabilityMemory.containsKey(lastStateReference))
	    	{
	    		//We have a record of this state being used before
	    		StableMemory thisRecord  = (StableMemory)probabilityMemory.get(lastStateReference);
	    		if(this.getStableK()>thisRecord.getNumberOfAttempts())
	    		{
	    			//We haven't yet tried enough attempts to be considered stable.
	    			//System.out.println("Node: "+this.goal_id+" has not reached the minimmum number of attempts. K"+this.getStableK()+"> att: "+thisRecord.getNumberOfAttempts());
	    			return false;
	    		}
	    		if(thisRecord.getDeltaProbability()>this.getStableEpsilon())
	    		{
	    			//Our rate of change is to high to be considered stable
	    			//System.out.println("Node: "+this.goal_id+" has not reached the minimmum change in probability, "+thisRecord.getDeltaProbability()+">"+this.getStableEpsilon());
	    			return false;
	    		}
	    		else
	    		{
	    			//System.out.println(this.getStableEpsilon()+">"+thisRecord.getDeltaProbability());
	    		}
	    	}
	    	else
	    	{
	    		
	    		return false;
	    		//We've not seen this record before...
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
    
    public boolean childrenStable()
    {
    	if(this.children.size()>0)
    	{
    		for(int j = 0; this.children.size()>j;j++)
    		{
    			Node thisNode = (Node)this.children.elementAt(j);
    			writeLog("Node: "+this.getItem()+", will check node "+thisNode.getItem()+" for stability","Stability-Updates");
    		}
    		
    		for(int j = 0; this.children.size()>j;j++)
    		{
    			Node thisNode = (Node)this.children.elementAt(j);
    			if(!thisNode.isStable(lastState))
    			{
    				return false;
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
    	return this.probabilityMemory.elements();
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
		//		+ data.numInstances() + " instances");
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
		//	System.out.println("Use the decision tree for Plan "+name);
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
	
	public void writeCsv(String msg, String postPend)
	{
		try
		{
			PrintWriter prnOut = new PrintWriter(new FileOutputStream(targetDir + "/" + postPend + ".csv", true), true);
			prnOut.println(msg);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created.");
		}
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
    	
    	out += " prop memory size: "+this.probabilityMemory.size();
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
    	//writeCsv("Plan: "+this.getItem(), "planClass/"+this.getItem());
    	for(int j = 0; bitSetCollection.size()>j;j++)
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
	    			val = decisionTree.distributionForInstance(instance);
	    			writeCsv(j+","+val[0]+","+val[1], "planClass/"+this.getItem());
	    		}
	    		catch(Exception e)
	    		{
	    			System.err.println("something went wrong when the instance was classified \n" +e);
	    			System.exit(9);
	    		}
	    	}
	    	else
	    	{
    			writeCsv("No DT available", "planClass/"+this.getItem());
	    		
	    	}
    	
    	}
    }
    
    public void referenceAllWorlds(Vector bitSetCollection)
    {
    	writeLog("Plan: "+this.getItem(), "planClass/key");
    	for(int j = 0; bitSetCollection.size()>j;j++)
    	{
    		BitSet tempBitSet = (BitSet)bitSetCollection.elementAt(j);
	    	writeLog(j+"="+stringBitSet(tempBitSet), "planClass/key");
	    		
	    	
    	
    	}
    }
}
