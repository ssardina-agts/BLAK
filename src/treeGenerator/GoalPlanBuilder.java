package treeGenerator;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Random;
import java.util.Vector;

public class GoalPlanBuilder 
{

	private int depth;
	private int goalBreadth;
	 
	private int actionCounter;
	private int planBreadth;
	private int numberOfAttributes;
	private int breadth;
	private int idealSequenceLength;
	private Vector<Attribute> attributeCollection;
	private Vector<BitSet> bitSetCollection;
	private BitSet goalState;
	private int numberOfPreconditions;
	private Vector<Action> actionCollection;
	private String[] alphabet;
	private Random randGen;
	private Vector<ActionSequence> sequenceCollection;
	private Vector<ActionSequence> extendedLowerSequenceCollection;
	private Vector<ActionSequence> extendedUpperSequenceCollection;

	private Vector<Vector<Vector<Action>>> sortedSets;
	private Vector<Plan> planCollection;
	private Vector<Goal> goalCollection;
	private Goal topLevelGoal;
	private String outputFile;
	private String[] sillyString;
	private int sillyCount;
	private boolean flatOr;
	private boolean dropOr;
	private boolean guidedTree;
	private boolean extendedDrop;
	private int splitValue;
	private String goalMask;
	
	private boolean idealLowIsOdd;
	private int idealSequenceLengthLower;
	private int idealSequenceLengthUpper;
	
	
	public GoalPlanBuilder()
	{
		setDepth(-1);
		setNumberOfAttributes(-1);
		setBreadth(-1);
		attributeCollection = new Vector<Attribute>();
		bitSetCollection = new Vector<BitSet>();
		actionCollection = new Vector<Action>();
		sequenceCollection = new Vector<ActionSequence>();
		extendedLowerSequenceCollection = new Vector<ActionSequence>();
		extendedUpperSequenceCollection = new Vector<ActionSequence>();
		sortedSets = new Vector<Vector<Vector<Action>>>();
		planCollection = new Vector<Plan>();
		goalCollection = new Vector<Goal>();
		topLevelGoal = new Goal("InitialGoal");
		topLevelGoal.setDepth(0);
		setOutputFile("");
		randGen = new Random();
		sillyString = new String[]{"I","\'","m"," ","a"," ","l","u","m","b","e","r"," ","j","a","c","k"," ","a","n","d"," ","I","\'","m"," ","o","k","a","y",".","I"," ","s","l","e","e","p"," ","a","l","l"," ","n","i","g","h","t"," ","a","n","d"," ","w","o","r","k"," ","a","l","l"," ","d","a","y","."," "};
		sillyCount = 0;		
		this.numberOfPreconditions = -1;
		goalMask = null;
		actionCounter = 1; 

		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		//System.out.println("Args: "+args);
				
		GoalPlanBuilder treeBuilder = new GoalPlanBuilder();
		
		if (args.length >0)
		{
			int index =0;
			while (index < args.length)
			{
				String arg = args[index];
				index++;
				char option = arg.charAt(1);
				switch(option)
				{
					case 'd': // depth of tree
						treeBuilder.setDepth(new Integer(args[index]).intValue());
						index++;
						break;
					case 'v':// number of variables/attributes
						treeBuilder.setNumberOfAttributes(new Integer(args[index]).intValue());
						index++;
						break;
					case 'w': //w is for width, in this case for goals
						treeBuilder.setGoalBreadth(new Integer(args[index]).intValue());
						index++;
						break;
					case 'h': //h is for horizontal cuts...dubious
						treeBuilder.setPlanBreadth(new Integer(args[index]).intValue());
						break;
					case 'b': //minimum breadth/body size 
						treeBuilder.setBreadth(new Integer(args[index]).intValue());
						index++;
						break;
					case 'g'://generation style of tree building
						if(args[index].equals("flat"))
						{
							treeBuilder.setFlatOr(true);
						}
						else if(args[index].equals("drop"))
						{
							treeBuilder.setDropOr(true);
						}
						else if(args[index].equals("guide"))
						{
							treeBuilder.setGuidedTree(true);
						}
						else if(args[index].equals("edrop"))
						{
							treeBuilder.setExtendedDrop(true);
						}
						else
						{
							System.out.println("Unknown Tree Building Style");
						}
						index++;
						break;
					case 's'://split levels by
						treeBuilder.setSplitValue(new Integer(args[index]).intValue());
						index++;
						break;
					case 'p': //number of preconditions
						treeBuilder.setNumberOfPreconditions(new Integer(args[index]).intValue());
						index++;
						break;
					case 'm': //set the mask too
						treeBuilder.setGoalMask(args[index]);
						index++;
						break;
					case 'o'://output file, 
						treeBuilder.setOutputFile(args[index]);
						index++;
						break;
					case 'i': //ideal sequence length
						treeBuilder.setIdealSequenceLength(new Integer(args[index]).intValue());
						index++;
						break;
				}
			}
			if(treeBuilder.getBreadth()==-1 || treeBuilder.getDepth()==-1 || treeBuilder.getNumberOfAttributes()==-1)
			{
				System.out.println("Breadth, depth or number of attributes not specified.");
				System.exit(0);
			}
			
			if(0>treeBuilder.getNumberOfPreconditions())
			{
				treeBuilder.setNumberOfPreconditions(treeBuilder.getNumberOfAttributes()/2);
			}
			System.out.println("Depth: "+treeBuilder.getDepth()+", Number of Attributes: "+
			treeBuilder.getNumberOfAttributes()+", breadth: "+treeBuilder.getBreadth());
			treeBuilder.mainExecution();
			
			for(int i = 0; args.length>i;i++)
			{
				treeBuilder.writeArgumentLog(args[i]);
			}
		}
	}
	
	/**
	 * Returns a number representing the number of actions that have been created. 
	 * Because we can create and drop actions when navigating between states, we ave a mechanism for creating dynamic unique identification of each action. 
	 * @return Number to use to identify a new action. 
	 */
	public int getNewActionNumber()
	{
		return this.actionCounter++;
		
	}

	/**
	 * Prints the next action in a silly string. Used to show operations being completed.
	 */
	public void printSilly()
	{
		if(this.sillyCount>=this.sillyString.length)
		{
			this.sillyCount = 0;
		}
		System.out.print(sillyString[sillyCount]);
		sillyCount++;
	}

	
	/**
	 * Sorts a grid of Vectors with Action Sequences. 
	 * Sorting groups the same actions together, to get cleaner splits when building trees.
	 * Appends a log to the {fileName}-gridSorted.txt file. 
	 * @param sortMe The Vector of ActionSequences to sort
	 * @return The sorted ActionSequences, in a Vector
	 */
	public Vector<ActionSequence> sortGrid(Vector<ActionSequence> sortMe)
	{
		Vector<ActionSequence> sorted = new Vector<ActionSequence>();
		for(int i =0; sortMe.size()>i;i++)
		{
			ActionSequence thisSequence = (ActionSequence)sortMe.elementAt(i).clone();
			Action firstAction = thisSequence.getAction(0);
			int insertionIndex = -1;
			for(int j = 0; sorted.size()>j;j++)
			{
				ActionSequence sortingSequence = sorted.elementAt(j);
				Action sortedFirstAction = sortingSequence.getAction(0);
				if(sortedFirstAction.equals(firstAction))
				{
					insertionIndex = j;
					break;
				}
			}
			if(insertionIndex>-1)
			{
				sorted.insertElementAt(thisSequence, insertionIndex);
			}
			else
			{
				sorted.add(thisSequence);
			}
			
		}
		this.writeGridSort(sortMe, sorted);
		return sorted;
	}
	
	
	/**
	 * Compresses a Vector of ActionSequences where possible into a smaller Vector.
	 * We are removing  multiple ActionSequences that have the same series of actions.
	 * A log is generated {fileName}-compress.txt
	 * @param compressMe The Vector of Action Sequences to compress
	 * @return The compressed Vector of action sequences
	 */
	public Vector<ActionSequence> compressGrid(Vector<ActionSequence> compressMe)
	{
		//Assumes all ActionSequences in the Vector are of equal length.
		Vector<ActionSequence> compressed = new Vector<ActionSequence>();
		for(int i =0; compressMe.size()>i;i++)
		{
			ActionSequence thisSequence = (ActionSequence)compressMe.elementAt(i).clone();
			boolean eqiv = false;
			
			for(int j = 0; compressed.size()>j;j++)
			{
				ActionSequence compressingSequence = compressed.elementAt(j);
				if(thisSequence.compressionEqual(compressingSequence))
				{
					eqiv = true; 
					break;
				}
			}
			
			if(!eqiv)
			{
				compressed.add(thisSequence);
			}
			
		}
		this.writeGridCompress(compressMe, compressed);
		return compressed;
	}
	
	
	/**
	 * Writes a log of a Grid sort to a file
	 * @param sortMe The initial state of the grid
	 * @param sorted the sorted state of the grid
	 */
	public void writeGridSort(Vector<ActionSequence> sortMe, Vector<ActionSequence> sorted)
	{
		String fileInfo = "---------------------------------\nSequence was: \n";
		for(int i =0; sortMe.size()>i;i++)
		{
			//this.printSilly();
			fileInfo = fileInfo + sortMe.elementAt(i)+"\n";
		}
		fileInfo = fileInfo + "\nSequence is sorted too\n";
		for(int i =0; sorted.size()>i;i++)
		{
			//this.printSilly();
			fileInfo = fileInfo + sorted.elementAt(i)+"\n";
		}
		
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-Grid-Sort.txt", true));
	    	//PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"_fail.txt", true));

	    	prnOut.println(fileInfo);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	
	/**
	 * Writes a record of a Grid compression to a log. 
	 * @param compressMe The initial state of the Grid.
	 * @param compressed The final state of the Grid.
	 */
	public void writeGridCompress(Vector<ActionSequence> compressMe, Vector<ActionSequence> compressed)
	{
		String fileInfo = "---------------------------------\nSequence was: \n";
		for(int i =0; compressMe.size()>i;i++)
		{
			fileInfo = fileInfo + compressMe.elementAt(i)+"\n";
		}
		fileInfo = fileInfo + "\nSequence is compressed too\n";
		for(int i =0; compressed.size()>i;i++)
		{
			fileInfo = fileInfo + compressed.elementAt(i)+"\n";
		}
		
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-Compress.txt", true));
	     	prnOut.println(fileInfo);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	
	/**
	 * Writes a log to the file {fileName}-Grid{Extended}.txt
	 * These files are intended to describe the number of sequences generated of a certain length.
	 * @param msg The message to include.
	 * @param extended True if the file is to be logged in the {fileName}-Grid-Extended.txt, false if otherwise.
	 */
	public void writeGridStates(String msg, boolean extended)
	{
		if(extended)
		{
			try
		    {
		    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-Grid-Extended.txt", true));
		       	prnOut.println(msg);
				prnOut.close();
			}
			catch(Exception e)
			{
				System.out.println("Error: File could not be created."); 
			}
		}
		else
		{
			try
		    {
		    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-Grid.txt", true));
		       	prnOut.println(msg);
				prnOut.close();
			}
			catch(Exception e)
			{
				System.out.println("Error: File could not be created."); 
			}
		}
	}
	
	
	/**
	 * Checks if the mask has been defined and if so, if its length matches the number of variables defined.
	 */
	public void checkMask()
	{
		if(this.goalMask==null)
		{
			//not set...use default, which is all variables set to false as the goal.
		}
		else
		{
			if(goalMask.length()!=this.numberOfAttributes)
			{
				System.out.println("The Goal mask and the number of attributes do not match.\n Please adjust either to match");
				System.exit(0);
			}
			else
			{
				for(int i = 0; goalMask.length()>i;i++)
				{
					if(goalMask.charAt(i)=='1')
					{
						this.goalState.set(i);
					}
					else if(goalMask.charAt(i)=='0')
					{
						this.goalState.clear(i);
					}
					else
					{
						System.out.println("Goal mask contains a dont care \'x\' @:"+i);
					}
				}
			}
		}
	}
	

	/**
	 * The main method of this class. It details the order in which the the other methods are executed. 
	 * This means we can change the entire order of execution by modifying this method.
	 */
	public void mainExecution()
	{
		generateAlphabet();
		goalState = new BitSet(this.getNumberOfAttributes());
		goalState.clear();
		generateAttributes(this.getNumberOfAttributes());
		checkMask();
		generateAllWorldStates();
		worldStatesEqualToGoal();
		generateActionsSmart();
		boolean sorted = false;
		while(!sorted)
		{
			sortSequences();
			sorted = this.checkSorting();
		}

		writeSequencesFile();
		extendSequences();
		writeWorldStatesFile();
		boolean actionsClean = false;
		while(!actionsClean)
		{
			actionsClean = scrubActions();
		}
		buildTree();
		boolean cutMade = true;
		while(cutMade)
		{
			cutMade = this.pruneTree();
		}
		writeTreeFile();
		checkSequences();
		checkExtendedSequences();
	}
	
	/*
	 * Use this in action generation to allow for goals states not made up of the entire attribute set.
	 * above. Also includes mechanism that sets these to observable or not based upon the mask provided!
	 * @param index The value to check
	 * @return boolean True is this value is a "Don't Care", false otherwise
	 */
	public boolean isDontCare(int index)
	{
		if(this.goalMask!=null)
		{
			if(this.goalMask.charAt(index)=='x' || this.goalMask.charAt(index)=='X')
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Selects the appropriate Tree Building method as defined by the user arguments.
	 * Flat Or Tree is the default if no valid argument was provided
	 */
	public void buildTree()
	{
		if(this.isDropOr())
		{
			this.buildDropOrTree();
		}
		else if(this.isFlatOr())
		{
			this.buildFlatOrTree();
		}
		else if(this.isGuidedTree())
		{
			this.buildGuidedDropTree();
		}
		else if(this.isExtendedDrop())
		{
			this.buildDropOrTreeExtended();
		}
		else
		{
			System.out.println("No tree style set, building Flat OR tree");
			this.buildFlatOrTree();
		}
		
		
	}
	
	/**
	 * Returns a String representation of the Tree. This method does not generate the tree, just shows the current generated form.
	 * @return A String representation of the generated tree.
	 */
	public String getBuiltTree()
	{
		String returnString = new String();
		for(int i = 0; attributeCollection.size()>i;i++)
		{
			printSilly();
			returnString = returnString + attributeCollection.elementAt(i)+"\n";
		}
		goalCollection.insertElementAt(this.topLevelGoal, 0);
		for(int g=0; goalCollection.size()>g;g++)
		{
			printSilly();
			returnString = returnString + goalCollection.elementAt(g)+"\n";
		}
		for(int a = 0; actionCollection.size()>a;a++)
		{
			printSilly();
			returnString = returnString +actionCollection.elementAt(a)+"\n";
		}
		for(int p=0; planCollection.size()>p;p++)
		{
			printSilly();
			returnString = returnString + planCollection.elementAt(p)+"\n";
		}
		return returnString;
	}
	
	/**
	 * Writes a message to a file.
	 * @param msg The msg to write
	 * @param postPend The postpended portion of the file name.
	 */
	public void writeLog(String msg, String postPend)
	{
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+postPend+".txt", true), true);
			prnOut.println(msg);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	/**
	 * Writes all elements included in the Action, Plans and and goal collections into a tree file. 
	 */
	public void writeTreeFile()
	{
		String fileInfo = this.getBuiltTree();
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+".txt"), true);
			prnOut.println(fileInfo);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}

	/*
	 * Writes a message to the file {fileName}-fail.txt
	 * @param msg The message to write into the file.
	 */
	public void writeFail(String msg)
	{
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"_fail.txt", true));
			prnOut.println(msg);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	/**
	 * Writes a  message to the file {fileName}-fail.txt 
	 * @param msg The message to write into the file.
	 */
	public void writeSuccess(String msg)
	{
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"_success.txt", true));
			prnOut.println(msg);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	/**
	 * Writes all the basic action Sequences into a file {fileName}-Action-Sequences.txt
	 * Sequences are taken from the sequenceCollection Vector.
	 */
	public void writeSequencesFile()
	{
		String fileInfo = "";
		for(int i =0; this.sequenceCollection.size()>i;i++)
		{
			this.printSilly();
			fileInfo = fileInfo + this.sequenceCollection.elementAt(i)+"\n";
		}
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-Action-Sequences.txt"), true);
			prnOut.println(fileInfo);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	/**
	 * Examines all the possible world states and compares them to the goal state (or states if a mask is set).
	 * If they are equal to the goal(s) then they are logged.
	 */
	public void worldStatesEqualToGoal()
	{
		for(int i = 0; this.bitSetCollection.size()>i;i++)
		{
			BitSet thisState = (BitSet)this.bitSetCollection.elementAt(i).clone();
			if(this.equalsGoalState(thisState))
			{
				this.writeWorldStateEqualToGoalFile(thisState);
			}
			
		}
	}

	
	/**
	 * Writes a Bit set into the {fileName}-World-States-Equal-To-Goal.txt file as being equal to the goal state
	 * @param bs The BitSet to write into the file.
	 */
	public void writeWorldStateEqualToGoalFile(BitSet bs)
	{
		String fileInfo = this.stringBitSet(bs)+" is equal to goal state: "+this.getGoalMask();
		
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-World-States-Equal-To-Goal.txt", true));
			prnOut.println(fileInfo);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	/**
	 * Writes to the file {fileName}-Argument.txt
	 * @param msg The message to write
	 */
	public void writeArgumentLog(String msg)
	{
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-Argument.txt", true));
			prnOut.print(msg+" ");
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	
	/**
	 * Writes all BitSets that make up all possible world states into the file {fileName}-World-States.txt  
	 */
	public void writeWorldStatesFile()
	{
		String fileInfo = "";
		for(int i =0; this.bitSetCollection.size()>i;i++)
		{
			this.printSilly();
			fileInfo = fileInfo + this.stringBitSet(this.bitSetCollection.elementAt(i))+"\n";
		}
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-World-States.txt"), true);
			prnOut.println(fileInfo);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	/**
	 * Writes all of the actions in the action collection to the the file {fileName}-Actions.txt
	 */
	public void writeActions()
	{
		String fileInfo = "";
		for(int i =0; this.actionCollection.size()>i;i++)
		{
			fileInfo = fileInfo + this.actionCollection.elementAt(i)+"\n";
		}
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-Actions.txt"), true);
			prnOut.println(fileInfo);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	
	/**
	 * Writes all the sequences in the Upper and Lower extended sequences collections to a file {fileName}-Extended-Action-Sequences.txt
	 */
	public void writeExtendedSequncesFile()
	{
		String fileInfo = "Lower Bound\n";
		for(int i =0; this.extendedLowerSequenceCollection.size()>i;i++)
		{
			this.printSilly();
			fileInfo = fileInfo + this.extendedLowerSequenceCollection.elementAt(i)+"\n";
		}
		fileInfo = fileInfo+"Upper Bound\n";
		for(int i =0; this.extendedUpperSequenceCollection.size()>i;i++)
		{
			this.printSilly();
			fileInfo = fileInfo + this.extendedUpperSequenceCollection.elementAt(i)+"\n";
		}
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(this.getOutputFile()+"-Extended-Action-Sequences.txt"), true);
			prnOut.println(fileInfo);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	
	/**
	 * Will Build a Guided Drop Tree.
	 * Needs to be completed, if indeed this is a valid approach. 
	 */
	public void buildGuidedDropTree()
	{
		
		
		
	}
	
	
	/**
	 * Builds the middle part of a tree with a specified depth...
	 * @param topLevelPlan The Plans which is sitting at depth 1.
	 * @param bottomLevelGoals The goals which will sit at the bottom level
	 * @param bottomLevelPlans The plans whcih will sit at the bottom level
	 */
	public void buildTreeMiddle(Plan topLevelPlan,Vector<Goal> bottomLevelGoals, Vector<Plan> bottomLevelPlans)
	{
		int depthCount = 2;//we've already built the highest layer....
		Vector<Goal> currentGoals = new Vector<Goal>();
		Vector<Plan> currentPlans = new Vector<Plan>();
		currentPlans.add(topLevelPlan);
		
		while (this.getDepth()>(depthCount))
		{
			//System.out.println("Building Depth is:"+depthCount+ " getDepth(): "+this.getDepth());
			Vector<Goal> imedGoals = new Vector<Goal>(); 
			for(int i = 0; currentPlans.size()>i;i++)
			{
				//for all plans at our depth....
				
				Plan thisPlan =currentPlans.elementAt(i); 
				if(thisPlan.getDepth()==(depthCount-1))
				{
					//build the number of goals required....
					for(int j = 0; this.getBreadth()>j;j++)
					{
						Goal newGoal = new Goal(thisPlan.getIDNumber()+"_"+j);
						newGoal.setDepth(depthCount);
						currentGoals.add(newGoal);
						imedGoals.add(newGoal);
						thisPlan.addObjectToBody(newGoal);
					}
					currentPlans.set(i, thisPlan);//update the plan so we have update its plan body...
				}
			}
			//then add new plans to all the new goals..Use imed goals as they are at this depth....
			for(int i = 0; imedGoals.size()>i;i++)
			{
				Goal thisGoal = imedGoals.elementAt(i);
				for(int j = 0; this.getBreadth()>j;j++)
				{
					Plan newPlan = new Plan(thisGoal.getIDNumber()+"_"+j);
					newPlan.setDepth(depthCount);
					newPlan.setHandleGoal(thisGoal.toNameString());
					newPlan.setInitPreState(true);
					newPlan.setPreState(true);
					currentPlans.add(newPlan);
				}
				
			}
			depthCount++;
		}
		depthCount--;//lifts to the bottom layer generated....
		//System.out.println("Done building the guts of our deep tree....");
		//System.out.println("Goals built: "+currentGoals.size()+", Plans built: "+currentPlans.size());
		Vector<Plan> deepestPlans = new Vector<Plan>();
		for(int i = 0; currentPlans.size()>i;i++)
		{
			Plan thisPlan = currentPlans.elementAt(i);
			if(thisPlan.getDepth()==depthCount)
			{
				deepestPlans.add(thisPlan);
			}
			
		}
		
		//for all the goals we need to graft to the grown tree...deep plans...
		int planCounter = 0;
		Vector<Goal> subGoals = new Vector<Goal>();
		for(int i = 0; bottomLevelGoals.size()>i ;i++)
		{
			
			Goal thisGoal = bottomLevelGoals.elementAt(i);
			subGoals.add(thisGoal);
			if(subGoals.size()==this.getBreadth())
			{
				//Then we have filled this required sub-allocation.
				Plan deepPlan = deepestPlans.elementAt(planCounter);
				for(int k = 0;subGoals.size()>k;k++)
				{
					//add the bottom level goals to a plan we generated.
					Goal thisSubGoal = subGoals.elementAt(k);
					//System.out.println("link Plan: "+deepPlan.getIDNumber()+" with goal: "+thisSubGoal.getIDNumber());
					deepPlan.addObjectToBody(thisSubGoal);
				}
				subGoals.clear();
				deepestPlans.set(planCounter,deepPlan);
				planCounter++;
			}
		}
		//write in the left overs...
		for(int i =0; subGoals.size()>i;i++)
		{
			Plan deepPlan = deepestPlans.elementAt(planCounter);
			for(int k = 0;subGoals.size()>k;k++)
			{
				//add the bottom level goals to a plan we generated.
				Goal thisSubGoal = subGoals.elementAt(k);
				deepPlan.addObjectToBody(thisSubGoal);
			}
			deepestPlans.set(planCounter,deepPlan);
			
		}
		
		/*System.out.println("Printing built deep plans");
		for (int i = 0; deepestPlans.size()>i;i++)
		{
			System.out.println(i+" -" +deepestPlans.elementAt(i));
		}*/
		
		//done building tree elements...now dump them to the general structrues...
		for(int i = 0; deepestPlans.size()>i;i++)
		{
			Plan deepPlan = deepestPlans.elementAt(i);
			if(currentPlans.contains(deepPlan))
			{
				//System.out.println("Containment");
				currentPlans.set(currentPlans.indexOf(deepPlan),deepPlan);
			}
		}
		currentPlans.remove(topLevelPlan);
		//maybe add duplicate checking....
		for(int i = 0; currentPlans.size()>i;i++)
		{
			Plan thisPlan = currentPlans.elementAt(i);
			this.planCollection.add(thisPlan);
		}
		for(int i = 0; currentGoals.size()>i;i++)
		{
			Goal thisGoal = currentGoals.elementAt(i);
			this.goalCollection.add(thisGoal);
		}
		
		for(int i = 0; bottomLevelPlans.size()>i;i++)
		{
			Plan thisPlan = bottomLevelPlans.elementAt(i);
			this.planCollection.add(thisPlan);
		}
		for(int i = 0; bottomLevelGoals.size()>i ;i++)
		{
			Goal thisGoal = bottomLevelGoals.elementAt(i);
			this.goalCollection.add(thisGoal);
		}
		
		
		
	}
	
	
	/**
	 * Removes plans/goals that don't lead to actions..
	 */

	public boolean pruneTree()
	{
		boolean cutMade = false;
		Vector<Plan> excessPlans = new Vector<Plan>();
		Vector<Goal> excessGoals = new Vector<Goal>();
		//find plans with no body plans which perform nothing...
		for(int i = 0; this.planCollection.size()>i;i++)
		{
			//this.printSilly();
			if(this.planCollection.elementAt(i).getBodySize()==0)
			{
				//planCollection.remove(i);
				//i--;
				excessPlans.add(this.planCollection.elementAt(i));
			}
		}
		//remove plans with no body.
		for(int i = 0; excessPlans.size()>i;i++)
		{	
			System.out.println("We should remove plan: "+excessPlans.elementAt(i).toNameString());
			planCollection.remove(excessPlans.elementAt(i));
			cutMade = true;
			if(this.planCollection.contains(excessPlans.elementAt(i)))
			{
				System.out.println("Plan Collection still contains loose plan: "+excessPlans.elementAt(i).toNameString());
			}
			else
			{
				System.out.println("Removal successful");
			}
		}
		
		//remove goals which are not handled.
		for(int i  = 0; this.goalCollection.size()>i; i++)
		{
			this.printSilly();
			Goal thisGoal = this.goalCollection.elementAt(i);
			boolean handled = false;
			for(int j =0 ; this.planCollection.size()>j;j++)
			{
				Plan thisPlan = this.planCollection.elementAt(j);
				if(thisPlan.getHandleGoal().equals(thisGoal.toNameString()))
				{
					handled = true;
					break;
				}
			}
			if(!handled)
			{
				System.out.println("Looks like we should delete: \n"+thisGoal+"\n as it is not handled");
				excessGoals.add(thisGoal);
				cutMade = true;
			}
		}
		
		for(int i = 0; excessGoals.size()>i;i++)
		{
			Goal thisGoal = excessGoals.elementAt(i);
			this.goalCollection.remove(thisGoal);
			for(int j = 0; planCollection.size()>j;j++)
			{
				Plan thisPlan = this.planCollection.elementAt(j);
				thisPlan.removeElementFromBody(thisGoal);
				this.planCollection.set(j,thisPlan);
			}
		}
		return cutMade;
	}

	/**
	 * Returns a Goal with the specified String as an ID
	 * @param id The ID of the goal
	 * @return The goal with the specified ID, null if not found.
	 */
	public Goal getGoal(String id)
	{
		for(int i = 0; this.goalCollection.size()>i;i++)
		{
			if(this.goalCollection.elementAt(i).getIDNumber().equals(id))
			{
				return this.goalCollection.elementAt(i);
			}
		}
		return null;
	}
	
	
	/**
	 * Returns the amount of plans that handle the specified goal 
	 * @param handleMe The goal to determine the number of handles.
	 * @return The number of times this goal is handled.
	 */
	public int goalHandles(Goal handleMe)
	{
		int handleCount = 0;
		for(int i = 0; this.planCollection.size()>i;i++)
		{
			Plan thisPlan = this.planCollection.elementAt(i);
			if(thisPlan.getHandleGoal().equals(handleMe.toNameString()))
			{
				handleCount++;
			}
		}
		return handleCount;
	}
	
	
	/**
	 * Returns the first Plan found to handle the specified Goal
	 * @param handle The goal to use in the search query
	 * @return The first plan found which handles this goal. Null if none found
	 */
	public Plan findPlanByGoalHandle(Goal handle)
	{
		Plan returnPlan = null;
		for(int i = 0; this.planCollection.size()>i;i++)
		{
			Plan thisPlan = this.planCollection.elementAt(i);
			if(thisPlan.getHandleGoal().equals(handle.toNameString()))
			{
				return thisPlan;
			}
		}
		
		return returnPlan;
	}
	
	//TODO - Need to adjust depths if a removal is made, in turn the names of the goals and plans affected.
	/**
	 * Removes all one to one relationships found within a tree
	 */
	public boolean removeOneToOne()
	{
		boolean cutMade = false;
		for(int i = 0; this.planCollection.size()>i;i++)
		{
			Plan thisPlan  =this.planCollection.elementAt(i);
			if(thisPlan.getBodySize()>0)
			{
				Vector<TreeGenElement> bodySpace = thisPlan.getBodySpace();
				for(int j = 0; bodySpace.size()>j ;j++)
				{
					TreeGenElement thisElement = bodySpace.elementAt(j);
					if(thisElement instanceof Goal)
					{
						Goal thisGoal = (Goal)thisElement;
						int handleCount = this.goalHandles(thisGoal);
						if(handleCount >1)
						{
							//handled more than once do nothing
							break;
						}
						else
						{
							Plan lowerPlan = this.findPlanByGoalHandle(thisGoal);
							Vector<TreeGenElement> lowerPlanBody = lowerPlan.getBodySpace();
							cutMade = true;
							bodySpace.remove(j);//remove the plan
							for(int k = 0; lowerPlanBody.size()>k;k++)
							{
								bodySpace.insertElementAt(lowerPlanBody.elementAt(k), (j+k));
							}
						}
					}
				}
				thisPlan.setBodySpace(bodySpace);
			}
			this.planCollection.set(i, thisPlan);
			
		}
		return cutMade;
		
	}
	

	/**
	 * Builds a drop or style tree from the standard sequence sets. Assumption is that these are populated.
	 */
	public void buildDropOrTree()
	{
		boolean sorted = false;
		while(!sorted)//sort all sequences by length
		{
			this.sortSequences();
			sorted = this.checkSorting();
		}
		
		//Build up our sequence grids...
		Vector<Vector<ActionSequence>> sequenceGrids = new Vector<Vector<ActionSequence>>();
		for( int i = 0; (this.getNumberOfAttributes()+1) >i;i++)
		{
			Vector<ActionSequence> sequenceGrid = new Vector<ActionSequence>();
			sequenceGrids.add(sequenceGrid);
		}
		//Put the action sequences into our grids...
		for(int i = 0; this.sequenceCollection.size()>i;i++)
		{
			ActionSequence thisSequence = this.sequenceCollection.elementAt(i);
			int seqLength = thisSequence.getSequenceLength();
			sequenceGrids.elementAt(seqLength).add(thisSequence);
		}
		//Grids organised now.
		
		System.out.println("Grid properties.");
		System.out.println("Collection has "+sequenceGrids.size()+" grids");
		for(int i = 0; sequenceGrids.size()>i;i++)
		{
			Vector<ActionSequence> grid = sequenceGrids.elementAt(i);
			int numberOfActions = i*grid.size();
			System.out.println("Grid "+i+" contains "+sequenceGrids.elementAt(i).size()+" sequences with "+numberOfActions+" actions total");
			String msg = "Grid "+i+" contains "+sequenceGrids.elementAt(i).size()+" sequences with "+numberOfActions+" actions total";
			this.writeGridStates(msg, true);
		}
		//Finished sorting and set up of grids.
	
		for(int i = 0; sequenceGrids.size()>i;i++)
		{
			System.out.println("i: "+i+"-------------------------------");
			//for all our grids
			//sequence length should be equal to i
			//for each set of sorted by length 
			Vector<ActionSequence> grid = sequenceGrids.elementAt(i);
			Vector<Goal> newGoals = new Vector<Goal>();//this will hold all the new goals generated.
			boolean doSplit = false;//tells if we can split this sequence length into multiple goals.
			
			Plan topPlan  = new Plan("_"+i);
			topPlan.setDepth(1);
			topPlan.setHandleGoal(this.topLevelGoal.toNameString());
			System.out.println("We have minimum breadth: "+this.getBreadth()+" and a sequence length of :"+i);
			for(int g = 0; grid.size()>g;g++)
			{
				System.out.println("Grid element: "+g+", sequence: "+grid.elementAt(g));
			}
						
			//Determine if we can split this sequence grid into the required minimum.
			//Make the appropriate number of goals.
			if(i >= this.getBreadth()) 
			{
				System.out.println("We can split this into more than a single goal");
				doSplit = true;
				for(int j = 0; this.getBreadth() >j;j++)//use breadth as we are making this many goals below.
				{
					Goal newGoal = new Goal(topPlan.getIDNumber()+"_"+(j+1));
					this.goalCollection.add(newGoal);
					topPlan.addObjectToBody(newGoal);
					newGoal.setDepth(2);
					newGoals.add(newGoal);
				}
			}
			else
			{
				doSplit = false;
				System.out.println("This sequence length cannot facilitate goal spliting");
				Goal newGoal  = new Goal(topPlan.getIDNumber()+"_1");
				this.goalCollection.add(newGoal);
				topPlan.addObjectToBody(newGoal);
				newGoal.setDepth(2);
				newGoals.add(newGoal);
			}
			//we now have the required number of goals.
			System.out.println("We generated: "+newGoals.size()+" new Goals...");
			
			
			if(doSplit)
			{
				//We have a collection of action sequences with length i. 
				//We need to split this evenly between the goals generated..
				//this is how big each portion should be.
				int portionSize = i/this.getBreadth();
				//this is how many 'remainders' we have
				int spares = i-(portionSize*this.getBreadth());
				int[] portionSizeIndex = new int[this.getBreadth()];
				for(int psi=0; portionSizeIndex.length>psi;psi++)
				{
					portionSizeIndex[psi] = portionSize;
				}
				
				System.out.println("i: "+i+", newGoals.size():"+newGoals.size()+",Portions will be: "+portionSize+" with spares: "+spares);
				
				int [] portionIndex = new int[this.getBreadth()];//the index for the cutting
				for(int j =0; newGoals.size()>j;j++)
				{
					for(int k = 0; this.getBreadth() > k;k++)
					//define the cut of sequence numbers this goal will determine
					{
						portionIndex[k] = (k*portionSize);
					}
				}
				
				while(spares>0)
				{
					//Have spares
					for(int spa = 0; this.getBreadth() >spa;spa++)
					{
						for(int cut = (spa+1); this.getBreadth()>cut;cut++)
						{
							portionIndex[cut] = portionIndex[cut]+1;
						}
						portionSizeIndex[spa] = portionSizeIndex[spa]+1;
						spares--;
						if(0>=spares)
						{
							break;
						}
					}
				}
				//else no spares, clean cut..
						
				for(int j = 0; newGoals.size() >j;j++)
				{
					//for each new goal..
					for(int k = 0; this.getBreadth()>k;k++)
					{
						//generate a new plan
						System.out.println("For grid: "+i+", portion index: "+j+" is at sequence Index: "+portionIndex[j]+" with length: "+portionSizeIndex[j]);
					}
					
					
					Goal thisGoal = newGoals.elementAt(j);
					System.out.println("Generating plan:" +(j+1)+ " for goal: "+thisGoal.getIDNumber());

					Vector<ActionSequence> subGrid = new Vector<ActionSequence>();
					for(int g = 0; grid.size()>g;g++)
					{
						//for each action sequence in the grid...
						ActionSequence thisSequence =  new ActionSequence(this.getNumberOfAttributes());
						ActionSequence longSequence = grid.elementAt(g);
						thisSequence.setStartState(longSequence.getStartState());
						for(int c = 0; portionSizeIndex[j]>c;c++ )
						{
							int cutoffSet = portionIndex[j];
							thisSequence.addAction(longSequence.getAction(cutoffSet+c));
						}
						subGrid.add(thisSequence);
					}
					if(subGrid.size()>0)
					{
						System.out.println("Baseline Build Plan level pass off");
						generateTreePlanLevel(topPlan, thisGoal, subGrid);
					}
					this.goalCollection.remove(thisGoal);
					this.goalCollection.add(thisGoal);
				}
			}
			else //We can't split at the desired breadth...
			{
				int portionSize = new Double(Math.ceil(i/newGoals.size())).intValue();//this is how big each portion should be.
				int spares = i-(portionSize*this.getBreadth());
				System.out.println("i: "+i+", newGoals.size():"+newGoals.size()+",Portions will be: "+portionSize+" with spares: "+spares);
				for(int j =0; newGoals.size()>j;j++)
				{
					//for each new goal take our portion then pass it off..
					Goal thisGoal  = newGoals.elementAt(j);
					Vector<ActionSequence> thisGoalDetermines = new Vector<ActionSequence>();
					//int [] portionIndex = new int[portionSize];
					int [] portionIndex = new int[this.getBreadth()];
					for(int k = 0; portionIndex.length > k;k++)//define the sequence numbers this goal will determine
					{
						portionIndex[k] = (j*portionSize)+k;
					}
									
					//for every sequence in the grid
					for(int k = 0; grid.size()>k;k++)
					{
						//We need to take the sequences determined
						ActionSequence masterSequence = grid.elementAt(k);
						ActionSequence newSequence = (ActionSequence)masterSequence.clone();
						newSequence.clearActions();
						//take the actions in the master sequence and add them into the new Sequence..
						/*System.out.println("------------");
						for(int a = 0; portionIndex.length>a;a++)
						{
							System.out.println("a:"+a+", pI: "+portionIndex[a]);
						}
						System.out.println("------------");*/
						
						for(int a = portionIndex[j]; (portionIndex[j]+portionSize)>a ;a++)
						{
							//System.out.println("a:"+a+", upper lim: "+(portionIndex[j]+this.getBreadth()));
							Action thisAction = masterSequence.getAction(a);
							newSequence.addAction(thisAction);
						}
						thisGoalDetermines.add(newSequence);
					}
					if(thisGoalDetermines.size()>0)
					{
						generateTreePlanLevel(topPlan, thisGoal,thisGoalDetermines);
					}
					this.goalCollection.remove(thisGoal);
					this.goalCollection.add(thisGoal);
				}
			}
		}
	}
	
	/**
	 * Returns the length (in Actions) of the longest extended sequence generated.
	 * @return The length of the longest sequence.
	 */
	public int getGreatestExtendedSequenceLength()
	{
		int greatest = 0;
		for(int i = 0; this.extendedLowerSequenceCollection.size()>i;i++)
		{
			if(this.extendedLowerSequenceCollection.elementAt(i).getSequenceLength()>greatest)
			{
				greatest = this.extendedLowerSequenceCollection.elementAt(i).getSequenceLength();
			}
		}
		for(int i = 0; this.extendedUpperSequenceCollection.size()>i;i++)
		{
			if(this.extendedUpperSequenceCollection.elementAt(i).getSequenceLength()>greatest)
			{
				greatest = this.extendedUpperSequenceCollection.elementAt(i).getSequenceLength();
			}
		}
		
		return greatest;
		
	}

	/**
	 * Builds a Tree using the drop or method, using the extended sequences.
	 * Assumes that the extended sequences have been populated.
	 */
	public void buildDropOrTreeExtended()
	{
		//Assumes extended sets have been made...
		System.out.println("Building Drop OR Tree from Extended Sequences");
		
		
		//Build up our sequence grids...
		Vector<Vector<ActionSequence>> sequenceGrids = new Vector<Vector<ActionSequence>>();
		for( int i = 0; (this.getGreatestExtendedSequenceLength()+2) >i;i++)
		{
			Vector<ActionSequence> sequenceGrid = new Vector<ActionSequence>();
			sequenceGrids.add(sequenceGrid);
		}
		//Put the action sequences into our grids...
		for(int i = 0; this.extendedLowerSequenceCollection.size()>i;i++)
		{
			ActionSequence thisSequence = this.extendedLowerSequenceCollection.elementAt(i);
			int seqLength = thisSequence.getSequenceLength();
			sequenceGrids.elementAt(seqLength).add(thisSequence);
		}
		for(int i = 0; this.extendedUpperSequenceCollection.size()>i;i++)
		{
			ActionSequence thisSequence = this.extendedUpperSequenceCollection.elementAt(i);
			int seqLength = thisSequence.getSequenceLength();
			System.out.println("i: "+i+" seq length: "+seqLength+" sequenceGrid Size: "+sequenceGrids.size());
			sequenceGrids.elementAt(seqLength).add(thisSequence);
		}
		//Grids organised now.
		
		System.out.println("Grid properties.");
		System.out.println("Collection has "+sequenceGrids.size()+" grids");
		for(int i = 0; sequenceGrids.size()>i;i++)
		{
			Vector<ActionSequence> grid = sequenceGrids.elementAt(i);
			int numberOfActions = i*grid.size();
			System.out.println("Grid "+i+" contains "+sequenceGrids.elementAt(i).size()+" sequences with "+numberOfActions+" actions total");
			String msg = "Grid "+i+" contains "+sequenceGrids.elementAt(i).size()+" sequences with "+numberOfActions+" actions total";
			this.writeGridStates(msg, true);
			
		}
		//Finished sorting and set up of grids.
	
		
		//TODO Here we could remove Sequence Grids with zero size... 
		
		for(int i = 0; sequenceGrids.size()>i;i++)
		{
			System.out.println("i: "+i+"-------------------------------");
			//for all our grids
			//sequence length should be equal to i
			//for each set of sorted by length 
			Vector<ActionSequence> grid = sequenceGrids.elementAt(i);
			Vector<Goal> newGoals = new Vector<Goal>();//this will hold all the new goals generated.
			boolean doSplit = false;//tells if we can split this sequence length into multiple goals.
			
			Plan topPlan  = new Plan("_"+i);
			topPlan.setDepth(1);
			topPlan.setHandleGoal(this.topLevelGoal.toNameString());
			System.out.println("We have minimum breadth: "+this.getBreadth()+" and a sequence length of :"+i);
			for(int g = 0; grid.size()>g;g++)
			{
				System.out.println("Grid element: "+g+", sequence: "+grid.elementAt(g));
				
			}
			
			
			//Determine if we can split this sequence grid into the required minimum.
			//Make the appropriate number of goals.
			if(i >= this.getBreadth()) 
			{
				System.out.println("We can split this into more than a single goal");
				doSplit = true;
				for(int j = 0; this.getBreadth() >j;j++)//use breadth as we are making this many goals below.
				{
					Goal newGoal = new Goal(topPlan.getIDNumber()+"_"+(j+1));
					this.goalCollection.add(newGoal);
					topPlan.addObjectToBody(newGoal);
					newGoal.setDepth(2);
					newGoals.add(newGoal);
				}
			}
			else
			{
				doSplit = false;
				System.out.println("This sequence length cannot facilitate goal spliting");
				Goal newGoal  = new Goal(topPlan.getIDNumber()+"_1");
				this.goalCollection.add(newGoal);
				topPlan.addObjectToBody(newGoal);
				newGoal.setDepth(2);
				newGoals.add(newGoal);
			}
			//we now have the required number of goals.
			System.out.println("We generated: "+newGoals.size()+" new Goals...");
		
			
			if(doSplit)
			{
				//We have a collection of action sequences with length i. 
				//We need to split this evenly between the goals generated..
				//this is how big each portion should be.
				int portionSize = i/this.getBreadth();
				//this is how many 'remainders' we have
				int spares = i-(portionSize*this.getBreadth());
				int[] portionSizeIndex = new int[this.getBreadth()];
				for(int psi=0; portionSizeIndex.length>psi;psi++)
				{
					portionSizeIndex[psi] = portionSize;
				}
				
				System.out.println("i: "+i+", newGoals.size():"+newGoals.size()+",Portions will be: "+portionSize+" with spares: "+spares);
				
				int [] portionIndex = new int[this.getBreadth()];//the index for the cutting
				for(int j =0; newGoals.size()>j;j++)
				{
					for(int k = 0; this.getBreadth() > k;k++)
					//define the cut of sequence numbers this goal will determine
					{
						portionIndex[k] = (k*portionSize);
					}
				}
				
				while(spares>0)
				{
					//Have spares
					for(int spa = 0; this.getBreadth() >spa;spa++)
					{
						for(int cut = (spa+1); this.getBreadth()>cut;cut++)
						{
							portionIndex[cut] = portionIndex[cut]+1;
						}
						portionSizeIndex[spa] = portionSizeIndex[spa]+1;
						spares--;
						if(0>=spares)
						{
							break;
						}
					}
				}
				//else no spares, clean cut..
				for(int j = 0; newGoals.size() >j;j++)
				{
					//for each new goal..
					for(int k = 0; this.getBreadth()>k;k++)
					{
						//generate a new plan
						System.out.println("For grid: "+i+", portion index: "+j+" is at sequence Index: "+portionIndex[j]+" with length: "+portionSizeIndex[j]);
					}
					
					
					Goal thisGoal = newGoals.elementAt(j);
					System.out.println("Generating plan:" +(j+1)+ " for goal: "+thisGoal.getIDNumber());

					Vector<ActionSequence> subGrid = new Vector<ActionSequence>();
					for(int g = 0; grid.size()>g;g++)
					{
						//for each action sequence in the grid...
						ActionSequence thisSequence =  new ActionSequence(this.getNumberOfAttributes());
						ActionSequence longSequence = grid.elementAt(g);
						thisSequence.setStartState(longSequence.getStartState());
						for(int c = 0; portionSizeIndex[j]>c;c++ )
						{
							int cutoffSet = portionIndex[j];
							thisSequence.addAction(longSequence.getAction(cutoffSet+c));
						}
						subGrid.add(thisSequence);
					}
					if(subGrid.size()>0)
					{
						System.out.println("Baseline Build Plan level pass off");
						generateTreePlanLevel(topPlan, thisGoal, subGrid);
					}
					this.goalCollection.remove(thisGoal);
					this.goalCollection.add(thisGoal);
				}
			}
			else //We can't split at the desired breadth...
			{
				int portionSize = new Double(Math.ceil(i/newGoals.size())).intValue();//this is how big each portion should be.
				int spares = i-(portionSize*this.getBreadth());
				System.out.println("i: "+i+", newGoals.size():"+newGoals.size()+",Portions will be: "+portionSize+" with spares: "+spares);
				for(int j =0; newGoals.size()>j;j++)
				{
					//for each new goal take our portion then pass it off..
					Goal thisGoal  = newGoals.elementAt(j);
					Vector<ActionSequence> thisGoalDetermines = new Vector<ActionSequence>();
					//int [] portionIndex = new int[portionSize];
					int [] portionIndex = new int[this.getBreadth()];
					for(int k = 0; portionIndex.length > k;k++)//define the sequence numbers this goal will determine
					{
						portionIndex[k] = (j*portionSize)+k;
					}
									
					//for every sequence in the grid
					for(int k = 0; grid.size()>k;k++)
					{
						//We need to take the sequences determined
						ActionSequence masterSequence = grid.elementAt(k);
						ActionSequence newSequence = (ActionSequence)masterSequence.clone();
						newSequence.clearActions();
						//take the actions in the master sequence and add them into the new Sequence..
						/*System.out.println("------------");
						for(int a = 0; portionIndex.length>a;a++)
						{
							System.out.println("a:"+a+", pI: "+portionIndex[a]);
						}
						System.out.println("------------");*/
						
						for(int a = portionIndex[j]; (portionIndex[j]+portionSize)>a ;a++)
						{
							//System.out.println("a:"+a+", upper lim: "+(portionIndex[j]+this.getBreadth()));
							Action thisAction = masterSequence.getAction(a);
							newSequence.addAction(thisAction);
						}
						thisGoalDetermines.add(newSequence);
					}
					if(thisGoalDetermines.size()>0)
					{
						generateTreePlanLevel(topPlan, thisGoal,thisGoalDetermines);
					}
					this.goalCollection.remove(thisGoal);
					this.goalCollection.add(thisGoal);
				}
			}
			if(!this.planCollection.contains(topPlan))
			{
				this.planCollection.add(topPlan);
			}
		}
	}

	
	
	/**
	 * Generates a new Level of plans anchored to the specified root goal, built with the specified contents included in the Vector of ActionSeqeuences.
	 * @param rootPlan - No longer used
	 * @param rootGoal - The goal that the generated plans will handle
	 * @param contents - The actions that will be used by these plans and their potential children
	 * @return True if generations 
	 */
	public boolean generateTreePlanLevel(Plan rootPlan, Goal rootGoal, Vector<ActionSequence> contents)
	{
		//include the sorting command to allow for organisation of  sub rows.
		
		//contents = this.sortActionSequences(contents);
		contents = this.compressGrid(contents);
		contents = this.sortGrid(contents);
		
		System.out.println("generateTreePlanLevel. Generate Plans contents size: "+contents.size());
		
		boolean finalDepth = false;
		if(rootGoal.getDepth()==this.getDepth())
		{
			finalDepth  = true;
		}
		for(int g = 0; contents.size()>g;g++)
		{
			System.out.println("Index: "+g+", depth: "+rootGoal.getDepth()+" ,"+contents.elementAt(g));
		}	
	
		System.out.println("Building Drop OR Tree, Plan Level, seq length:" +contents.firstElement().getSequenceLength());
		//for our grid, contents
		//Number of sequences is what we gain plan breadth from.
		int iLength = contents.size();
		
		if(1>=contents.size())
		{
			if(contents.size()==1)
			{
				ActionSequence thisSequence = contents.elementAt(0);
				//System.out.println("OH YEAH!");
				Plan newPlan = new Plan(rootGoal.getIDNumber()+"_"+1);
				newPlan.setDepth(rootGoal.getDepth());
				newPlan.setHandleGoal(rootGoal.toNameString());
				for(int i=0; thisSequence.getSequenceLength()>i;i++)
				{
					//System.out.println("Adding action "+thisSequence.getAction(i).getNameString()+ " to plan: "+newPlan.getIDNumber());
					newPlan.addObjectToBody(thisSequence.getAction(i));
				}
				this.planCollection.add(newPlan);
			}
			else
			{
				
			}
			return true;
		}
		
		if(contents.firstElement().getSequenceLength()<=1)
		{
			//System.out.println("Monkey");
			//System.out.println("Hand forced add each to a plan");
			iLength = 0;
			
			for(int i = 0; contents.size()>i;i++)
			{
				ActionSequence thisSequence = contents.elementAt(i);
				
				if(thisSequence.getSequenceLength()>0)
				{
					//for each remaining action in the sequence, make a plan for it.
					Plan newPlan = new Plan(rootGoal.getIDNumber()+"_"+(i+1));
					newPlan.setDepth(rootGoal.getDepth());
					newPlan.setHandleGoal(rootGoal.toNameString());
					newPlan.addObjectToBody(thisSequence.getAction(0));
					this.planCollection.add(newPlan);
					
				}
				
			}
			return true;
		}
		
		boolean doSplit = false;//tells if we can split this sequence length into multiple plans.
			
		System.out.println("We have minimum breadth: "+this.getBreadth()+" and a sequence length of :"+iLength);
		Vector<Plan> newPlans = new Vector<Plan>();//to store the new plans	
		//Determine if we can split this sequence grid into the required minimum. 
		//Make the appropriate number of plans.
		//if we are the the bottom depth then we need to place the actions with the appropriate number of plans...
		
		if(finalDepth)
		{
			System.out.println("Depth requirement attained. Dumping final plans from grids");
			
			for(int j =0; iLength>j ;j++)
			{
				Plan newPlan = new Plan(rootGoal.getIDNumber()+"_"+(j+1));
				newPlan.setDepth(rootGoal.getDepth());
				newPlan.setHandleGoal(rootGoal.toNameString());
				
				ActionSequence thisSequence = contents.elementAt(j);
				for(int k = 0; thisSequence.getSequenceLength()>k;k++)
				{
					newPlan.addObjectToBody(thisSequence.getAction(k));
				}
				newPlans.add(newPlan);
				this.planCollection.add(newPlan);
			}
		}
		
		else if(iLength >= this.getBreadth()) 
		{
			System.out.println("We can split this into more than a single plan");
			doSplit = true;
			for(int j = 0; this.getBreadth() >j;j++)//use breadth as we are making this many plans below.
			{
					Plan newPlan = new Plan(rootGoal.getIDNumber()+"_"+(j+1));
					newPlan.setDepth(rootGoal.getDepth());
					newPlan.setHandleGoal(rootGoal.toNameString());
					newPlans.add(newPlan);
					this.planCollection.add(newPlan);
			}
		}
		else
		{
			doSplit = false;
			System.out.println("This sequence length cannot facilitate plan spliting");
			Plan newPlan  = new Plan(rootGoal.getIDNumber()+"_1");
			this.planCollection.add(newPlan);
			newPlan.setDepth(rootGoal.getDepth());
			newPlan.setHandleGoal(rootGoal.toNameString());
			newPlans.add(newPlan);
		}
		
	
		//we now have the required number of plans.
		System.out.println("We generated: "+newPlans.size()+" new plans  for goal: "+rootGoal.getIDNumber());
		if(doSplit)
		{
			if(contents.firstElement().getSequenceLength()<=1)
			{
				System.out.println("We cannont generate any new goals beyond here");
				return true;
			}
			//we have a collection of action sequences with length i. 
			//We need to split them evenly between the plans generated..
			//this is how big each portion should be.
			int portionSize = iLength/this.getBreadth();
			//this is how many 'remainders' we have
			int spares = iLength-(portionSize*this.getBreadth());
			int[] portionSizeIndex = new int[this.getBreadth()];
			for(int psi=0; portionSizeIndex.length>psi;psi++)
			{
				portionSizeIndex[psi] = portionSize;
			}
				
			System.out.println("iLength: "+iLength+", newPlans.size():"+newPlans.size()+",Portions will be: "+portionSize+" with spares: "+spares);
				
			int [] portionIndex = new int[this.getBreadth()];//the index for the cutting
			for(int j =0; newPlans.size()>j;j++)
			{
				for(int k = 0; this.getBreadth() > k;k++)
				//define the cut of sequence numbers this goal will determine
				{
					portionIndex[k] = (k*portionSize);
				}
			}
				
			while(spares>0)
			{
				//Have spares
				for(int spa = 0; this.getBreadth() >spa;spa++)
				{
					for(int cut = (spa+1); this.getBreadth()>cut;cut++)
					{
						portionIndex[cut] = portionIndex[cut]+1;
					}
					portionSizeIndex[spa] = portionSizeIndex[spa]+1;
					spares--;
					if(0>=spares)
					{
						break;
					}
				}
			}
			//else no spares, clean cut..
			System.out.println("New Funk: We have: "+newPlans.size()+" plans");
			for(int j = 0; newPlans.size() >j;j++)
			{
				System.out.println("Iteration j: "+j+" portionIndex: "+portionIndex[j]+", portionSize: "+portionSizeIndex[j]);
				
			}
			
			for(int j = 0; newPlans.size() >j;j++)
			{
				//for our plans
				System.out.println("For plan: "+j+" of goal: "+rootGoal.toNameString());
				System.out.println("Build sequence from elements starting: "+portionIndex[j]+" with size: "+portionSizeIndex[j]);
				Plan thisPlan = newPlans.elementAt(j);
				Vector<ActionSequence> subGrid = new Vector<ActionSequence>();
				//Determine the actions which will be passed down the tree...
				//for(int g = 0; contents.size()>g;g++)
				
				//for(int c = portionIndex[j]; portionSizeIndex[j]>c;c++ )
				for(int c = 0; portionSizeIndex[j]>c;c++ )
				{
					//for each action sequence in the grid...
					int cutoffSet = portionIndex[j];
					ActionSequence longSequence = (ActionSequence)contents.elementAt(cutoffSet+c).clone();
					subGrid.add(longSequence);
				}
				if(subGrid.size()>0)
				{
					System.out.println("Create goal level for plan: "+thisPlan.getIDNumber());
					generateTreeGoalLevel(thisPlan, rootGoal, subGrid);
				}
				this.planCollection.remove(thisPlan);
				this.planCollection.add(thisPlan);
				System.out.println("End da funk");
			}
		}
		
		else if (!finalDepth)
		{
			
			int portionSize = new Double(Math.ceil(iLength/newPlans.size())).intValue();//this is how big each portion should be.
			int spares = iLength-(portionSize*this.getBreadth());
			System.out.println("Hand forced");
			System.out.println("iLength: "+iLength+", newPlans.size():"+newPlans.size()+",Portions will be: "+portionSize+" with spares: "+spares);
			//System.exit(0);
			
			
			
			for(int j =0; newPlans.size()>j;j++)
			{
				//for each new plan take our portion then pass it off to generate a new goal.
				Plan thisPlan  = newPlans.elementAt(j);
				Vector<ActionSequence> thisGoalDetermines = new Vector<ActionSequence>();
				
				int [] portionIndex = new int[this.getBreadth()];
				for(int k = 0; portionIndex.length > k;k++)//define the sequence numbers the new goal. will determine
				{
					portionIndex[k] = (j*portionSize)+k;
				}
				
				//for every sequence in the grid
				for(int k = 0; contents.size()>k;k++)
				{
					//We need to take the sequences determined
					ActionSequence masterSequence =  contents.elementAt(k);
					ActionSequence newSequence = new ActionSequence(this.getNumberOfAttributes());
					newSequence.setStartState(masterSequence.getStartState());
				
					for(int a = portionIndex[j]; (portionIndex[j]+portionSize)>a ;a++)
					{
						Action thisAction = masterSequence.getAction(a);
						newSequence.addAction(thisAction);
					}
					if(newSequence.getSequenceLength()>0)
					{
						thisGoalDetermines.add(newSequence);
					}
				}
			
				if(thisGoalDetermines.size()>0)
				{
					//generateTreeGoalLevel(thisPlan, rootGoal,thisGoalDetermines);
				}
				this.planCollection.remove(thisPlan);
				this.planCollection.add(thisPlan);
			}
		}
		return true;
	}
	
	public int getHammingDistance(BitSet bs1, BitSet bs2)
	{
		int distance = 0;
		for(int i = 0; this.getNumberOfAttributes()>i;i++)
		{
			if(bs1.get(i)!=bs2.get(i))
			{
				distance++;
			}
			
		}
		return distance;
	}
	
	//find start state with hamming distance...
	public Vector<ActionSequence> findStateWithHammingDistance(BitSet startState, int requiredDistance)
	{
		Vector<ActionSequence> collection  = new Vector<ActionSequence>();
		for(int i = 0; this.sequenceCollection.size()>i;i++)
		{
			ActionSequence thisSequence = this.sequenceCollection.elementAt(i);
			BitSet thisState = thisSequence.getStartState();
			if(this.getHammingDistance(thisState, startState)==requiredDistance)
			{
				collection.add(thisSequence);
			}
			
		}
		
		return collection;
	}
	
	public ActionSequence getActionSequence(BitSet bs)
	{
		//System.out.println("Looking for sequence : "+this.stringBitSet(bs)+ "sequence collection size: "+this.sequenceCollection.size());
		for(int i = 0; this.sequenceCollection.size()>i;i++)
		{
			if(this.sequenceCollection.elementAt(i).getStartState().equals(bs))
			{
				//System.out.println("Sequence found at :"+i);
				return (ActionSequence)this.sequenceCollection.elementAt(i).clone();
			}
			
		}
		return null;
	}
	
	public int numberOfSetValuesInGoal()
	{
		if(this.getGoalMask()!=null)
		{
			String rep = this.getGoalMask();
			int setValues = 0;
			for(int i = 0; rep.length()>i;i++)
			{
				if(rep.charAt(i)=='1' || rep.charAt(i)=='0' )
				{
					setValues++;
				}
			}
			return setValues;
		}
		else
		{
			return this.getNumberOfAttributes();
		}
	
		
	}
	

	
	/*public Action makeFreeAction(BitSet bs, int changeBit)
	{
		//Action thisAction = new Action(this.getActionCounter());
		
		
	}*/
	
	
	/**
	 * Looks at all the sequences we have,
	 * Combines their length to goal and distance between the start state and their start states.
	 * @param startState
	 * @param desiredLength
	 */
	public Vector<ActionSequence> totalWalkDistanceSequences(BitSet startState, int desiredLength)
	{
		Vector<ActionSequence> newCollection = new Vector<ActionSequence>();//all the states that sequences between the specifed state state and all other states
		Vector<BitSet> validDestinations =  new Vector<BitSet>();
		
		int makeItThisLength = this.getIdealSequenceLength();
		
		int weightToGoal = this.getHammingWeightToGoal((BitSet)startState.clone());
		if(weightToGoal==this.numberOfAttributes)
		//if(weightToGoal==this.numberOfSetValuesInGoal())
		//TODO What would we do if the ideal sequence length was less that the number of variables....
		{
			newCollection.add(this.getActionSequence(startState));
			return newCollection;
		}
		if(this.isOdd(weightToGoal))
		{
			//start state is odd..
			if(this.isIdealLowIsOdd())
			{
				makeItThisLength = this.getIdealSequenceLengthLower();
			}
			else
			{
				makeItThisLength = this.getIdealSequenceLengthUpper();
			}
		}
		else
		{
			//start state is even...
			if(this.isIdealLowIsOdd())
			{
				makeItThisLength = this.getIdealSequenceLengthUpper();
			}
			else
			{
				makeItThisLength = this.getIdealSequenceLengthLower();
			}
		}
		
		BitSet nextClosest = null;
		int nextClosestDistance = Integer.MAX_VALUE;
		for(int i = 0 ; this.bitSetCollection.size()>i;i++)
		{
			//We check every world state, determine its distance from start and then goal state..
			BitSet endState = (BitSet)this.bitSetCollection.elementAt(i).clone();
			if(!this.equalsGoalState(endState))
			{
				System.out.println("Checking: "+this.stringBitSet(startState)+" to "+this.stringBitSet(endState));
				ActionSequence thisSequence = this.getActionSequence((BitSet)endState.clone());
				if(thisSequence==null)
				{
					//this.writeSequncesFile();
				}
			
				thisSequence.populateVisitedStates();
				if(0>=this.getHammingDistance((BitSet)startState.clone()))
				{
					break;
				}
				else if(startState.equals(endState))
				{
					//start and goal the same, skip it.
					//this.writeFail("Start and end states are the same");
				}
				else
				{
					int hammingFromStart = this.getHammingDistance((BitSet)endState.clone(), (BitSet)startState.clone());
					int hammingOfDash = this.getHammingDistance((BitSet)endState.clone()); 
					int totalHammingDistance = hammingFromStart + hammingOfDash;
					int differenceBetween = makeItThisLength- totalHammingDistance;
					
					//ActionSequence temp = new ActionSequence(this.getNumberOfAttributes());
					if(totalHammingDistance==makeItThisLength)
					{
						//this can give us the desired length...
						//get the actions we will use...
						validDestinations.add((BitSet)endState.clone());
					}
					else if(validDestinations.size()==0 && nextClosestDistance>differenceBetween && differenceBetween >0)
					{
						nextClosestDistance = differenceBetween;
						nextClosest = (BitSet)endState.clone();
					}
					else
					{
						//this path is not the right length....
						//this.writeFail("FAIL - Start @: "+this.stringBitSet(startState)+"->"+totalHammingDistance+"!="+desiredLength+" to -> "+this.stringBitSet((BitSet)endState.clone()));
						//Extend logic here perhaps. 
					}
				}
			}
			else
			{
				System.out.println("This bitset is equal to the end state...."+this.stringBitSet(endState));
				//System.exit(0);
			}
		}
		
		if(validDestinations.size()==0)
		{
			writeFail("@ start point "+this.stringBitSet(startState)+" no path was found. We found "+validDestinations.size()+" valid destinations. We were trying path of length: "+makeItThisLength+".Trying the other limit value");
			if(this.isOdd(weightToGoal))
			{
				//start state is odd
				if(this.isIdealLowIsOdd())
				{
					makeItThisLength = this.getIdealSequenceLengthUpper();
				}
				else
				{
					makeItThisLength = this.getIdealSequenceLengthLower();
				}
			}
			else
			{
				//start state is even...
				if(this.isIdealLowIsOdd())
				{
					makeItThisLength = this.getIdealSequenceLengthLower();
				}
				else
				{
					makeItThisLength = this.getIdealSequenceLengthUpper();
				}
			}
			
			writeFail("@ start point "+this.stringBitSet(startState)+" other value attempt is: "+makeItThisLength);
		}
		
		
		//Try again, with less ideal sequence length.
		for(int i = 0 ; this.bitSetCollection.size()>i;i++)
		{
			//We check every world state, determine its distance from start and then goal state..
			BitSet endState = (BitSet)this.bitSetCollection.elementAt(i).clone();
			if(!this.equalsGoalState(endState))
			{
				System.out.println("Checking: "+this.stringBitSet(startState)+" to "+this.stringBitSet(endState));
				ActionSequence thisSequence = this.getActionSequence((BitSet)endState.clone());
				if(thisSequence==null)
				{
					//this.writeSequncesFile();
				}
			
				thisSequence.populateVisitedStates();
				if(0>=this.getHammingDistance((BitSet)startState.clone()))
				{
					break;
				}
				else if(startState.equals(endState))
				{
					//start and goal the same, skip it.
					//this.writeFail("Start and end states are the same");
				}
				else
				{
					int hammingFromStart = this.getHammingDistance((BitSet)endState.clone(), (BitSet)startState.clone());
					int hammingOfDash = this.getHammingDistance((BitSet)endState.clone()); 
					int totalHammingDistance = hammingFromStart + hammingOfDash;
					int differenceBetween = makeItThisLength- totalHammingDistance;

					
					//ActionSequence temp = new ActionSequence(this.getNumberOfAttributes());
					if(totalHammingDistance==makeItThisLength)
					{
						//this can give us the desired length...
						//get the actions we will use...
						validDestinations.add((BitSet)endState.clone());
					}
					else if(validDestinations.size()==0 && nextClosestDistance>differenceBetween && differenceBetween >0)
					{
						nextClosestDistance = differenceBetween;
						nextClosest = (BitSet)endState.clone();
					}
					else
					{
						//this path is not the right length....
						//this.writeFail("FAIL - Start @: "+this.stringBitSet(startState)+"->"+totalHammingDistance+"!="+desiredLength+" to -> "+this.stringBitSet((BitSet)endState.clone()));
						//Extend logic here perhaps. 
					}
				}
			}
			else
			{
				System.out.println("This bitset is equal to the end state...."+this.stringBitSet(endState));
				//System.exit(0);
			}
		}
		
		//We now have all the paths of the length we want. Theoretically....
		System.out.println("Found "+validDestinations.size()+" valid stopping points");
		boolean additionalActions = false;
		
		if(1 > validDestinations.size() && nextClosest!=null && Integer.MAX_VALUE > nextClosestDistance)
		{
			//we've found no paths with either of the ideal limited sequence lengths...
			writeFail("@ start point "+this.stringBitSet(startState)+" no path was found. We found "+validDestinations.size()+" valid destinations. Instead we are using the next closest value, to destination: "+this.stringBitSet(nextClosest)+" which has a distance of : "+nextClosestDistance+" from the ideal path length");
			writeFail("No Ideal lengths paths found for  "+this.stringBitSet(startState)+", however we have "+numberOfFreeMoves()+" free moves");
			validDestinations.add(nextClosest);
			additionalActions = true;
		}
		for(int i = 0; validDestinations.size()>i;i++)
		{
			
			BitSet thisDestination = validDestinations.elementAt(i);
			ActionSequence newPath = new ActionSequence(this.getNumberOfAttributes());
			newPath.setStartState((BitSet)thisDestination.clone());
			System.out.println("Break 1, startState: "+this.stringBitSet((BitSet)startState.clone())+", new path: "+newPath);
			newPath = this.generateActionsBetweenStates((BitSet)startState.clone(), newPath);
			
			ActionSequence newDashPath;
			if(!additionalActions)
			{
				newDashPath = this.generatePathToGoal((BitSet)thisDestination.clone(), newPath, 0);
			}
			else
			{
				writeFail("Using "+nextClosestDistance+" free Moves");
				newDashPath = this.generatePathToGoal((BitSet)thisDestination.clone(), newPath, nextClosestDistance);
				//System.out.println("Breaker  1");
				//System.exit(0);
			}
			
			for(int j = 0; newDashPath.getSequenceLength()>j; j++)
			{
				newPath.addAction(newDashPath.getAction(j));
			}
			newCollection.add(newPath);
		}
		
		if(newCollection.size()==0)
		{
			writeFail("@ start point "+this.stringBitSet(startState)+" no path was found. We found "+validDestinations.size()+" valid destinations");
		}
		
		System.out.println("End method - totalWalkDistanceSequences");
		return newCollection;
		
	}
	
	
	public int[] indexOfFreeMoves()
	{
		int[] indexes = new int[this.numberOfFreeMoves()];
		if(this.getGoalMask()!=null)
		{
			String rep = this.getGoalMask();
			int freeMoves = 0;
			for(int i = 0; rep.length()>i;i++)
			{
				if(rep.charAt(i)=='x' || rep.charAt(i)=='X')
				{
					indexes[freeMoves] = i;
					freeMoves++;
				}
			}
		}
		return indexes;
	}
	
	/**
	 * Returns how many variables that describe the goal state are "Do not care".
	 * @return
	 */
	public int numberOfFreeMoves()
	{
		if(this.getGoalMask()!=null)
		{
			String rep = this.getGoalMask();
			int freeMoves = 0;
			for(int i = 0; rep.length()>i;i++)
			{
				if(rep.charAt(i)=='x' || rep.charAt(i)=='X')
				{
					freeMoves++;
				}
			}
			return freeMoves;
		}
		else
		{
			return 0;
		}
	}
	
	
	public BitSet actionConsequence(BitSet startState, Action actor)
	{
		BitSet result = (BitSet)startState.clone();
		String[] stringRep = actor.getStringRepresentation();
		boolean cleared = true;
		int changeBit = -1;
		for (int i = 0; stringRep.length>i;i++)
		{
			if(stringRep[i].equals("c") || stringRep[i].equals("C"))
			{
				//result.flip(i);
				//skip this we either don't care or will change it.
				changeBit = i;
			}
			else if(stringRep[i].equals("X") || stringRep[i].equals("x"))
			{
				//skip this we either don't care or will change it.
			}
			else if(result.get(i))
			{
				if(stringRep[i].equals("1"))
				{
					
				}
				else
				{
					cleared = false;
				}
			}
			else if(!result.get(i))
			{
				if(stringRep[i].equals("0"))
				{
					
				}
				else
				{
					cleared = false;
				}
			}
			
		}
		
		if(cleared)
		{
			if(changeBit>-1)
			{
				if(actor.isToTrue())
				{
					result.set(changeBit);
				}
				else
				{
					result.clear(changeBit);
				}
			}
		}
		return result;
	}
	
	
	public Vector<Action> findAllActions(BitSet modifyMe, int changeBit)
	{
		Vector<Action> applicableActions = new Vector<Action>();
		boolean changeFrom = modifyMe.get(changeBit);
		for(int i = 0; actionCollection.size()>i;i++)
		{
			Action thisAction = actionCollection.elementAt(i);
			String[] actionProfile = thisAction.getStringRepresentation();
			BitSet matchup = new BitSet(this.getNumberOfAttributes());
			matchup.clear();
			if(actionProfile[changeBit].equals("c"))
			{
				if(!changeFrom && thisAction.isToTrue())
				{
					matchup.set(changeBit);
				}
				else if(changeFrom && !thisAction.isToTrue())
				{
					matchup.set(changeBit);
				}
				if(matchup.get(changeBit))
				//this action changes the required variable
				//check preconditions.
				{
					for (int j = 0; actionProfile.length>j; j++)
					{
						if(actionProfile[j].equals("x"))
						{
							matchup.set(j);
							//ignore
						}
						else if(actionProfile[j].equals("c"))
						{
							//ignore.. bool should already be set...
						}
						else if(actionProfile[j].equals("1"))
						{
							//the action requires this variable to be true.
							if(modifyMe.get(j)==true)
							{
								//this works
								matchup.set(j);
							}
							else
							{
								//next action
								break;
							}
						}
						else if(actionProfile[j].equals("0"))
						{
							//action requires this variable to be false...
							if(modifyMe.get(j)==false)
							{
								//this works
								matchup.set(j);
							}
							else
							{
								//next action..
								break;
							} 
						}
					}
					//end action inspection.
					if(matchup.cardinality()==this.getNumberOfAttributes())
					{
						//matches up 100%
						applicableActions.add(this.actionCollection.elementAt(i));
					}
				}
				else
				{
				
				}
			}
		}
		return applicableActions;
	}
	

	
	public ActionSequence generateActionsBetweenStates(BitSet passedStartState, ActionSequence dashSequence)
	{
		BitSet startState = (BitSet)passedStartState.clone();
		BitSet endState = (BitSet)dashSequence.getStartState().clone();
		dashSequence.populateVisitedStates();
		
		ActionSequence mySequence = new ActionSequence(this.getNumberOfAttributes());
		
		if(this.equalsGoalState(endState))
		{
			System.out.println("Shouldn't have reached here. Looking for a path to the goalstate");
			System.exit(0);
		}
		
		mySequence.setStartState((BitSet)startState.clone());
		BitSet workingSet = (BitSet)startState.clone();
		int thisHammingDistance = this.getHammingDistance(workingSet, endState);
		System.out.println("Beginning Work on p.StartSequence: "+this.stringBitSet(passedStartState));
		
		while(thisHammingDistance> 0)
		{
			System.out.println("Hamming distance is currently: "+thisHammingDistance);
			
			boolean actionUsed = false;
			for(int j = 0; this.getNumberOfAttributes()>j; j++)
			{	//Try and use any existing action....
				if(workingSet.get(j)!=endState.get(j))
				{
					//We need to change this variable.
					//So get all the actions that change this bit in the right way.
					Vector<Action> usefulActions = findAllActions((BitSet)workingSet.clone(), j);//does this method take transistion direction into account?
					System.out.println("Found "+usefulActions.size()+" useful actions to change bit: "+j);
					if(usefulActions.size()>0)
					{
						System.out.println("j: "+j+". Working StartSet: "+this.stringBitSet(startState)+". Found useful actions found for: "+this.stringBitSet(workingSet)+ " too "+this.stringBitSet(endState));
						for(int k = 0; usefulActions.size()>k;k++)
						{
							System.out.println("Useful action :"+k+" is : "+usefulActions.elementAt(k).toNameString());
							//we have an action we can use.
							Action useMe = usefulActions.elementAt(k);
							int modifiedBit = this.findChangeBit(useMe);//redundant use, but a bit more modular
							if(modifiedBit>-1)
							{
								workingSet.flip(modifiedBit);
								if(workingSet.equals(passedStartState))
								{
									//we've already visited this state.
									workingSet.flip(modifiedBit);//flip back
								}
								else if(0>=this.getHammingWeightToGoal(workingSet))
								{
									System.out.println("Action :"+useMe.getNameString()+" takes us to the goal prematurely");
									workingSet.flip(modifiedBit);//flip back
								}
								/*else if (dashSequence.getVisitedStates().contains(workingSet))
								{
									//we've already visited this state.
									this.writeFail("Already visited: Included in dash sequence "+this.stringBitSet(workingSet)+" from start state"+this.stringBitSet(startState));
									workingSet.flip(modifiedBit);//flip back
								}*/
								else if(mySequence.getVisitedStates().contains(workingSet))
								{
									//we've already visited this state.
									this.writeFail("Already visited state "+this.stringBitSet(workingSet)+" from start state"+this.stringBitSet(startState));
									workingSet.flip(modifiedBit);//flip back
								}
								else
								{
									mySequence.addAction(useMe);
									System.out.println("Starting mySequence: "+mySequence);
									//System.out.println("Break 3");
									mySequence.populateVisitedStates();
									//System.out.println("Break 4");
									actionUsed = true;
									break;
								}
							}
						}
					}
					else
					{
						System.out.println("No Actions were found, so none were used");
						
					}
				}
			}
			
			System.out.println("Finished Looking at existing Actions");
			
			if(!actionUsed)
			{
				System.out.println("No useful actions found for: "+this.stringBitSet(workingSet)+ " too "+this.stringBitSet(endState)+". Lets Make some.");

				//we have absolutely no actions we can use to modify the world state...
				for(int j = 0; this.getNumberOfAttributes()>j; j++)
				{
					//find the first point of difference...
					if(workingSet.get(j)!=endState.get(j))
					{
						System.out.println("Point of difference is at: "+j);
						System.out.println("Start state: "+this.stringBitSet(startState));
						System.out.println("Working Set: "+this.stringBitSet(workingSet));
												
						//startState.set(j, !startState.get(j));
						
						workingSet.flip(j);
						if(workingSet.equals(passedStartState))
						{
							//we've already visited this state.
							workingSet.flip(j);//flip back
						}
						else if(0>=this.getHammingWeightToGoal(workingSet))
						{
							System.out.println("1 .Changing bit:" +j+" takes us to the goal prematurely");
							workingSet.flip(j);//flip back
						}
						else if(mySequence.getVisitedStates().contains(workingSet))
						{
							System.out.println("My Sequence. Already visited state: "+this.stringBitSet(workingSet));
							//Already been here. Flip back.
							workingSet.flip(j);
						}
						/*else if(dashSequence.getVisitedStates().contains(workingSet))
						{
							System.out.println("Dash Sequence: Already visited state: "+this.stringBitSet(workingSet));
							//Already been here. Flip back.
							workingSet.flip(j);
						}*/
						else
						{
							System.out.println("This path has not yet gone to location: "+this.stringBitSet(workingSet));
							workingSet.flip(j);//Put the set back t the modify state...
							//We are going to make an action so we can change this variable...
							int actionNumber = -1;
							this.generateAction(workingSet, j);//make it
							actionNumber = findAction(workingSet, j);//find it...
							System.out.println("Using Created Action: "+this.actionCollection.elementAt(actionNumber));
							if(actionNumber>-1)
							{
								//cool, now we have one
								Action useMe = actionCollection.elementAt(actionNumber);
								useMe.setNewTemp(true);
								actionCollection.set(actionNumber, useMe);
								int modifiedBit = this.findChangeBit(useMe);
								
								if(modifiedBit>-1)
								{
									workingSet.flip(modifiedBit);
									//Check the values again...
									if(workingSet.equals(passedStartState))
									{
										//we've already visited this state.
										workingSet.flip(modifiedBit);//flip back
									}
									//else if (dashSequence.getVisitedStates().contains(startState))
									/*else if (dashSequence.getVisitedStates().contains(workingSet))
									{
										//we've already visited this state.
										this.writeFail("Already visited state "+this.stringBitSet(workingSet)+" from start state"+this.stringBitSet(passedStartState));
										workingSet.flip(modifiedBit);//flip back
									}*/
									else if(mySequence.getVisitedStates().contains(workingSet))
									{
										//we've already visited this state.
										this.writeFail("Already visited state "+this.stringBitSet(workingSet)+" from start state"+this.stringBitSet(passedStartState));
										workingSet.flip(modifiedBit);//flip back
									}
									System.out.println("Adding action: " +useMe.getNameString()+" to sequence. ws has become: "+this.stringBitSet(workingSet));
									mySequence.addAction(useMe);
									mySequence.populateVisitedStates();
									System.out.println("Always Updating haming distance between working set: "+this.stringBitSet(workingSet)+" end state: "+this.stringBitSet(endState));
									thisHammingDistance =  this.getHammingDistance(workingSet, endState);
									actionUsed= true;
									break;
								}
							}
						}
					}
					else
					{
						System.out.println("No difference @ point: "+j);
						
					}
				}
			}
			
			if(!actionUsed)
			{
				System.out.println("Kick and scream");
				System.out.println("We have reaches a point where attempts to change any variable has failed. Not good");
				System.out.println("Start State: "+this.stringBitSet(startState));
				System.out.println("Work  State: "+this.stringBitSet(workingSet));
				System.out.println("End   State: "+this.stringBitSet(endState));
				System.out.println("Goal  State: "+this.stringBitSet(this.goalState));
				System.out.println("Goal   Mask: "+this.goalMask);
				//System.exit(0);
				int changeIndex = randGen.nextInt(this.numberOfAttributes);
				System.out.println("We will randomly try to change bit: "+changeIndex);
				
				this.generateAction(workingSet, changeIndex);
				int actionNumber = findAction(workingSet, changeIndex);
				Action randomAction = this.actionCollection.elementAt(actionNumber);
				
				/**
				 * Drop in code
				 */
				
				workingSet.flip(changeIndex);
				if(workingSet.equals(passedStartState))
				{
					//we've already visited this state.
					workingSet.flip(changeIndex);//flip back
				}
				else if(0>=this.getHammingWeightToGoal(workingSet))
				{
					System.out.println("Changing bit:" +changeIndex+" takes us to the goal prematurely");
					workingSet.flip(changeIndex);//flip back
				}
				else if(mySequence.getVisitedStates().contains(workingSet))
				{
					System.out.println("My Sequence. Already visited state: "+this.stringBitSet(workingSet));
					//Already been here. Flip back.
					workingSet.flip(changeIndex);
				}
				else
				{
					System.out.println("This path has not yet gone to location: "+this.stringBitSet(workingSet));
					System.out.println("Using Created Action: "+randomAction);
					//cool, now we have one
					randomAction.setNewTemp(true);
					System.out.println("Adding action: " +randomAction.getNameString()+" to sequence. ws has become: "+this.stringBitSet(workingSet));
					mySequence.addAction(randomAction);
					mySequence.populateVisitedStates();
					System.out.println("Always Updating haming distance between working set: "+this.stringBitSet(workingSet)+" end state: "+this.stringBitSet(endState));
					thisHammingDistance =  this.getHammingDistance(workingSet, endState);
					actionUsed= true;
					//this.actionCollection.add(randomAction);						
				}
				
				/**
				 * End drop in code
				 */
					
			}
			actionUsed = false;
			//recalculate the ham distance to hopefully escape the loop!
			System.out.println("Updating haming distance between working set: "+this.stringBitSet(workingSet)+" end state: "+this.stringBitSet(endState));
			thisHammingDistance =  this.getHammingDistance(workingSet, endState);
			
		}
		System.out.println("Hamming Distance reduced to 0");
		
		return mySequence;
		
	}



	
	public int[] getRandomInspectionArray()
	{
		int[] returnArray = new int[this.getNumberOfAttributes()];
		
		
		
		return returnArray;
	}
	
	public ActionSequence generatePathToGoal(BitSet passedStartState, ActionSequence previousSequence, int freeActions)
	{
		
		BitSet startState = (BitSet)passedStartState.clone();
		
		
		ActionSequence mySequence = new ActionSequence(this.getNumberOfAttributes());
		mySequence.setStartState((BitSet)startState.clone());
		//int thisHammingDistance = this.getHammingDistance(startState); 
		int thisHammingDistance = getHammingWeightToGoal(startState);
		System.out.println("Generate Path To Goal. "+this.stringBitSet(passedStartState)+", will use: "+freeActions+" free actions.");
		
		while(thisHammingDistance> 0)
		{
			System.out.println("Hamming Distance: "+thisHammingDistance);
			boolean actionUsed = false;
			if(freeActions>0 && thisHammingDistance==1)
			{
				writeFail("Packing in the addition actions.");
				System.out.println("Packing in the addition actions.");
				int[] dontCareIndex = this.indexOfFreeMoves();
				for(int dc = 0; dontCareIndex.length>dc;dc++)
				{
					this.generateAction(startState, dontCareIndex[dc]);//make it
					int actionNumber = findAction(startState, dontCareIndex[dc]);//find it...
					Action useMe = this.actionCollection.elementAt(actionNumber);
					writeFail("trying to use action: "+useMe.toString());
					
					int modifiedBit = this.findChangeBit(useMe);//redundant use, but a bit more modular
					if(modifiedBit>-1 && this.canUseAction(startState, useMe))
					{
						startState.flip(modifiedBit);
						if(startState.equals(passedStartState))
						{
							//we've already visited this state.
							startState.flip(modifiedBit);//flip back
						}
						else if (previousSequence.getVisitedStates().contains(startState))
						{
							//we've already visited this state.
							startState.flip(modifiedBit);//flip back
						}
						else
						{
							mySequence.addAction(useMe);
							actionUsed = true;
							freeActions--;
							
							writeFail("Action "+useMe.getNameString()+" used successfully");
							break;
						}
					}
				}
			}
			
			
			
			if(!actionUsed)
			{
				for(int j = 0; this.getNumberOfAttributes()>j; j++)
				{
					if(this.isDontCare(j))
					{}
					else if(startState.get(j)!=this.goalState.get(j))
					{
						//We need to change this variable.
						Vector<Action> usefulActions = findAllActions(startState, j);
						for(int k = 0; usefulActions.size()>k;k++)
						{
							///we have an action we can use.
							Action useMe = usefulActions.elementAt(k);
							int modifiedBit = this.findChangeBit(useMe);//redundant use, but a bit more modular
							if(modifiedBit>-1 && this.canUseAction(startState, useMe))
							{
								startState.flip(modifiedBit);
								if(startState.equals(passedStartState))
								{
									//we've already visited this state.
									startState.flip(modifiedBit);//flip back
								}
								else if (previousSequence.getVisitedStates().contains(startState))
								{
									//we've already visited this state.
									startState.flip(modifiedBit);//flip back
								}
								else
								{
									mySequence.addAction(useMe);
									actionUsed = true;
									break;
								}
							}
						}
						if(actionUsed)
						{
							break;
						}
					}
					
				}
			}
				
			if(!actionUsed)
			{
				//we have absolutely no actions we can use to modify the world state...
				for(int j = 0; this.getNumberOfAttributes()>j; j++)
				{
					if(this.isDontCare(j))
					{}
					//find the first point of difference...
					if(startState.get(j)!=this.goalState.get(j))
					{
						//we are going to change this variable...
							
						int actionNumber = -1;
						this.generateAction(startState, j);//make it
						actionNumber = findAction(startState, j);//find it...
						if(actionNumber>-1)
						{
							//cool, now we have one
							Action useMe = actionCollection.elementAt(actionNumber);
							useMe.setNewTemp(true);
							actionCollection.set(actionNumber, useMe);
							int modifiedBit = this.findChangeBit(useMe);
							if(modifiedBit>-1)
							{
								startState.flip(modifiedBit);
								mySequence.addAction(useMe);
								break;
							}
						}
					}
				}
			}
			actionUsed = false;
			//recalculate the ham distance to hopefully escape the loop!
			thisHammingDistance =  getHammingWeightToGoal(startState);
		}
		return mySequence;
		
	}

	
	public void removeTempActions()
	{
		for(int i = 0; this.actionCollection.size()>i;i++)
		{
			Action temp = this.actionCollection.elementAt(i);
			if(temp.isNewTemp())
			{
				this.actionCollection.remove(temp);
				i--;
			}
			
		}
		
	}
	
	public int getHammingWeight(BitSet bs)
	{
		int weight = 0;
		for(int i =0; bs.length()>i;i++)
		{
			if(bs.get(i))
			{
				weight++;
			}
		}
		return weight++;
		
	}
	
	public int getHammingWeightToGoal(BitSet bs)
	{
		int weight = 0;
		for(int i =0; this.getNumberOfAttributes()>i;i++)
		{
			if(this.getGoalMask()!=null)
			{
				//we have a goal mask string...
				String rep = this.getGoalMask();
				if(rep.charAt(i)=='X' || rep.charAt(i)=='x')
				{
					//Skip
				}
				else if(rep.charAt(i)=='1')
				{
					if(bs.get(i))
					{}
					else
					{
						weight++;
					}
				}
				else if(rep.charAt(i)=='0')
				{
					if(bs.get(i))
					{
						weight++;
					}
					else{}
				}
			}
			else if(bs.get(i))
			{
				weight++;
			}
		}
		return weight++;
		
	}
	
	
	public boolean isOdd(int value)
	{
		boolean odd = false;
		if( (value %2)>0)
		{
			odd = true;
		}
		return odd;
		
		
	}
	
	public boolean equalsGoalState(BitSet bs)
	{
		boolean equal = true;
		
		if(this.getGoalMask()!=null)
		{
			for(int i = 0; this.numberOfAttributes>i;i++)
			{
				if(this.getGoalMask().charAt(i)=='x' || this.getGoalMask().charAt(i)=='X')
				{
					//skip
				}
				else if(this.getGoalMask().charAt(i)=='1')
				{
					if(!bs.get(i))
					{
						return false;
					}
				}
				else if(this.getGoalMask().charAt(i)=='0')
				{
					if(bs.get(i))
					{
						return false;
					}
				}
			}
		}
		else//We aren't using a mask...
		{
			BitSet tempGoal = (BitSet)this.getGoalState().clone();
			for(int i = 0; this.getNumberOfAttributes()>i; i++)
			{
				if(tempGoal.get(i)!=bs.get(i))
				{
					return false;
				}
				
			}
		}
		return equal;
	}
	
	/**
	 * Used to extend the generated sequences to a user specified length.
	 */
	public void extendSequences()
	{
		if(!this.isExtendedDrop())
		{
			return;
		}
		//We should know what we want the sequences to be a certain length. 
		//We call this our ideal sequence length
		
		int failCount = 0;
		for(int i = 0; this.bitSetCollection.size()>i;i++)
		{
			//this.printSilly();
			if(this.equalsGoalState(this.bitSetCollection.elementAt(i)))
			{
				//skip it....
				System.out.println("Should skip this state as it already equals the goal state.");
			}
			else
			{
				//for all the world states we have...
				ActionSequence newExtSeq = new ActionSequence(this.getNumberOfAttributes());//This is our new extend sequence
				newExtSeq.setStartState((BitSet)this.bitSetCollection.elementAt(i).clone());
				System.out.print("Checking Sequence: "+this.stringBitSet(newExtSeq.getStartState())+" i: "+i+" tot: "+this.bitSetCollection.size()+ " failed: "+failCount+"\n");
				//BitSet workingState = (BitSet)newExtSeq.getStartState().clone();
				
				//get all combined hamming distance sequences location thingie mc bobs, start at this state, to each other state...
				Vector<ActionSequence> appropriateDestinations = totalWalkDistanceSequences((BitSet)this.bitSetCollection.elementAt(i).clone(), this.getIdealSequenceLength());
				if(0>=appropriateDestinations.size())
				{
					System.out.print("Panic no extended path found for start state: "+this.stringBitSet(this.bitSetCollection.elementAt(i)));
					failCount++;
					writeFail("Fail @ i:" +i+" weight: "+this.getHammingWeightToGoal((BitSet)this.bitSetCollection.elementAt(i).clone()));
				}
				else
				{
					System.out.println(appropriateDestinations.size()+" possbile paths for starting point "+this.stringBitSet(this.bitSetCollection.elementAt(i)));
					ActionSequence useMe = appropriateDestinations.elementAt(randGen.nextInt(appropriateDestinations.size()));
					if(this.isOdd(this.getHammingWeightToGoal(newExtSeq.getStartState())))
					{
						if(this.isIdealLowIsOdd())
						{
							this.extendedLowerSequenceCollection.add((ActionSequence)useMe.clone());
						}
						else
						{
							this.extendedUpperSequenceCollection.add((ActionSequence)useMe.clone());
						}
					}
					else
					{//new sequence is even...
						if(this.isIdealLowIsOdd())
						{
							this.extendedUpperSequenceCollection.add((ActionSequence)useMe.clone());
						}
						else
						{
							this.extendedLowerSequenceCollection.add((ActionSequence)useMe.clone());
						}
					}
					
					//useMe.setStartState((BitSet)this.bitSetCollection.elementAt(i).clone());
					
					
					for(int j = 0; useMe.getSequenceLength()>j;j++)
					{
						Action thisAction = useMe.getAction(j);
						int index = this.actionCollection.indexOf(thisAction);
						if(index>0)
						{
							thisAction.setNewTemp(false);
							this.actionCollection.set(index, thisAction);
						}
						else
						{
							thisAction.setNewTemp(false);
							
							this.actionCollection.add(thisAction);
						}
					}
				}
				this.removeTempActions();
			}	
		}
		
		System.out.println("Dumping extended Sequences");
		this.writeActions();
		writeExtendedSequncesFile();
		//System.exit(0);
		
	}
	
	public String arrayToString(String[] array)
	{
		String rep = "";
		for(int i = 0; array.length>i;i++)
		{
			rep = rep+array[i];			
		}
		return rep;
	}
	
	public void checkExtendedSequences()
	{
		//this.extendedLowerSequenceCollection
		this.writeLog("Lower Set Proof:", "-Extend-Proof");
		for(int i = 0; this.extendedLowerSequenceCollection.size()>i;i++)
		{
			ActionSequence thisSequence = this.extendedLowerSequenceCollection.elementAt(i);
			BitSet workingSet = (BitSet)thisSequence.getStartState().clone();
			this.writeLog("Proving extended sequence for start state: "+this.stringBitSet((BitSet)thisSequence.getStartState().clone()), "-Extend-Proof");
			String msgLine = "";
			for(int j = 0; thisSequence.getSequenceLength()>j;j++)
			{
				Action thisAction = (Action)thisSequence.getAction(j).clone();
				msgLine = "From state: "+this.stringBitSet(workingSet)+ " action: "+thisAction.getNameString()+ " with profile: "+this.arrayToString(thisAction.getStringRepresentation())+" gives state: ";
				workingSet = this.applyAction(workingSet, thisAction);
				msgLine = msgLine +" state: "+this.stringBitSet(workingSet);
				this.writeLog(msgLine, "-Extend-Proof");
			}
			if(this.getHammingWeightToGoal(workingSet)==0)
			{
				writeLog("Sequence Reaches Goal","-Extend-Proof");
			}
			else
			{
				writeLog("Sequence Failed","-Extend-Proof");
			}
		}
		
		this.writeLog("Upper Set Proof:", "-Extend-Proof");
		for(int i = 0; this.extendedUpperSequenceCollection.size()>i;i++)
		{
			ActionSequence thisSequence = this.extendedUpperSequenceCollection.elementAt(i);
			BitSet workingSet = (BitSet)thisSequence.getStartState().clone();
			this.writeLog("Proving extended sequence for start state: "+this.stringBitSet((BitSet)thisSequence.getStartState().clone()), "-Extend-Proof");
			String msgLine = "";
			for(int j = 0; thisSequence.getSequenceLength()>j;j++)
			{
				Action thisAction = (Action)thisSequence.getAction(j).clone();
				msgLine = "From state: "+this.stringBitSet(workingSet)+ " action: "+thisAction.getNameString()+ " with profile: "+this.arrayToString(thisAction.getStringRepresentation())+" gives state: ";
				workingSet = this.applyAction(workingSet, thisAction);
				msgLine = msgLine +" state: "+this.stringBitSet(workingSet);
				this.writeLog(msgLine, "-Extend-Proof");
			}
			if(this.getHammingWeightToGoal(workingSet)==0)
			{
				writeLog("Sequence Reaches Goal","-Extend-Proof");
			}
			else
			{
				writeLog("Sequence Failed","-Extend-Proof");
			}
		}
	}
	
	
	public void checkSequences()
	{
		for(int i = 0; this.sequenceCollection.size()>i;i++)
		{
			ActionSequence thisSequence = this.sequenceCollection.elementAt(i);
			BitSet workingSet = (BitSet)thisSequence.getStartState().clone();
			this.writeLog("Proving sequence for start state: "+this.stringBitSet((BitSet)thisSequence.getStartState().clone()), "-Sequence-Proof");
			String msgLine = "";
			for(int j = 0; thisSequence.getSequenceLength()>j;j++)
			{
				Action thisAction = (Action)thisSequence.getAction(j).clone();
				msgLine = "From state: "+this.stringBitSet(workingSet)+ " action: "+thisAction.getNameString()+ " with profile: "+this.arrayToString(thisAction.getStringRepresentation())+" gives state: ";
				workingSet = this.applyAction(workingSet, thisAction);
				msgLine = msgLine +" state: "+this.stringBitSet(workingSet);
				this.writeLog(msgLine, "-Sequence-Proof");
			}
			if(this.getHammingWeightToGoal(workingSet)==0)
			{
				writeLog("Sequence Reaches Goal","-Sequence-Proof");
			}
			else
			{
				writeLog("Sequence Failed","-Sequence-Proof");
			}
		}
	}
	
	public BitSet applyAction(BitSet bs, Action applyMe)
	{
		BitSet workingSet = (BitSet)bs.clone();
		String[] rep = applyMe.getStringRepresentation();
		
		boolean doesNotApply = false;
		int flipIndex = -1;
		for(int i = 0; this.getNumberOfAttributes()>i;i++)
		{
			if(rep[i].equals("c") || (rep[i].equals("C")))
			{
				flipIndex = i;
				if(applyMe.isToTrue())
				{
					//Action 0->1
					if(!workingSet.get(i))
					{}
					else
					{
						doesNotApply = true;
						break;
					}
				}
				else if(!applyMe.isToTrue())
				{
					//Action 1->0
					if(workingSet.get(i))
					{}
					else
					{
						doesNotApply = true;
						break;
					}
				}
			}
			else if(rep[i].equals("1"))
			{
				if(workingSet.get(i))
				{}
				else
				{
					doesNotApply = true;
					break;
				}
			}
			else if(rep[i].equals("0"))
			{
				if(workingSet.get(i))
				{
					doesNotApply = true;
					break;
				}
			}
			else if(rep[i].equals("X") || rep[i].equals("x"))
			{}
			else
			{
				System.out.println("String representation "+rep[i]+"unknown");
			}
		}
		
		if(!doesNotApply)
		{
			workingSet.flip(flipIndex);
		}
		return workingSet;
	}
	
	public boolean scrubActions()
	{
		boolean cleanScrub = true;
		for(int i = 0; this.actionCollection.size()>i; i++)
		{
			Action thisAction = this.actionCollection.elementAt(i);
			boolean used = false;
			for(int j = 0; this.sequenceCollection.size()>j;j++)
			{
				ActionSequence thisSequence = this.sequenceCollection.elementAt(j);
				if(thisSequence.usesAction(thisAction))
				{
					used = true;
					break;
				}
			}
			for(int j =0; this.extendedLowerSequenceCollection.size()>j;j++)
			{
				ActionSequence thisSequence = this.extendedLowerSequenceCollection.elementAt(j);
				if(thisSequence.usesAction(thisAction))
				{
					used = true;
					break;
				}
			}
			
			for(int j =0; this.extendedUpperSequenceCollection.size()>j;j++)
			{
				ActionSequence thisSequence = this.extendedUpperSequenceCollection.elementAt(j);
				if(thisSequence.usesAction(thisAction))
				{
					used = true;
					break;
				}
			}
			if(!used)
			{
				this.actionCollection.remove(thisAction);
				cleanScrub = false;
				//i--;
			}
			int numberInstances = 0;
			for(int j = 0; this.actionCollection.size()>j;j++)
			{
				if(this.actionCollection.elementAt(j).equals(thisAction))
				{
					numberInstances++;
				}
			}
			if(numberInstances>1)
			{
				numberInstances = numberInstances -1;
				cleanScrub=false;
				for(int j =0; numberInstances>j;j++)
				{
					this.actionCollection.remove(thisAction);
					
				}
				
			}
			
		}
		return cleanScrub;
	}
	
	public ActionSequence findSequence(BitSet theStartState)
	{
		for(int i = 0; this.sequenceCollection.size()>i;i++)
		{
			if(this.sequenceCollection.elementAt(i).getStartState().equals(theStartState))
			{
				return this.sequenceCollection.elementAt(i);
			}
		}
		return null;
		
	}
	
	public boolean generateTreeGoalLevel(Plan rootPlan, Goal rootGoal, Vector<ActionSequence> contents)
	{
		//set depth to plus 1.
		System.out.println("generateTreeGoalLevel. Goal Level tree builder. Content Size: "+contents.size());
		/*for(int g = 0; contents.size()>g;g++)
		{
			System.out.println("Index: "+g+" ,"+contents.elementAt(g));
		}*/
		if(contents.size()<=0)
		{
			return false;
		}
		
		//for all our grids
		//sequence length should be equal to iLength
		//for each set of sorted by length 
		
		Vector<Goal> newGoals = new Vector<Goal>();//this will hold all the new goals generated.
		boolean doSplit = false;//tells if we can split this sequence length into multiple goals.
		int iLength = contents.firstElement().getSequenceLength();
		
		System.out.println("We have minimum breadth: "+this.getBreadth()+" and a sequence length of :"+iLength);
			
		//Determine if we can split this sequence grid into the required minimum. 
		//Make the appropriate number of goals.
		if(iLength >= this.getBreadth()) 
		{
			System.out.println("We can split this into more than a single goal");
			doSplit = true;
			for(int j = 0; this.getBreadth() >j;j++)//use breadth as we are making this many goals below.
			{
				Goal newGoal = new Goal(rootPlan.getIDNumber()+"_"+(j+1));
				this.goalCollection.add(newGoal);
				rootPlan.addObjectToBody(newGoal);
				newGoal.setDepth(rootPlan.getDepth()+1);
				newGoals.add(newGoal);
			}
		}
		else
		{
			doSplit = false;
			System.out.println("This sequence length cannot facilitate goal spliting");
			Goal newGoal  = new Goal(rootPlan.getIDNumber()+"_1");
			this.goalCollection.add(newGoal);
			rootPlan.addObjectToBody(newGoal);
			newGoal.setDepth(rootPlan.getDepth()+1);
			newGoals.add(newGoal);
		}
		//we now have the required number of goals.
		
		
		if(doSplit)
		{
			//we have a collection of action sequences with length i. 
			//We need to split this evenly between the goals generated..
			//this is how big each portion should be.
			int portionSize = iLength/this.getBreadth();
			//this is how many 'remainders' we have
			int spares = iLength-(portionSize*this.getBreadth());
			int[] portionSizeIndex = new int[this.getBreadth()];
			for(int psi=0; portionSizeIndex.length>psi;psi++)
			{
				portionSizeIndex[psi] = portionSize;
			}
				
			System.out.println("i: "+iLength+", newGoals.size():"+newGoals.size()+",Portions will be: "+portionSize+" with spares: "+spares);
			
			int [] portionIndex = new int[this.getBreadth()];//the index for the cutting
			for(int j =0; newGoals.size()>j;j++)
			{
				for(int k = 0; this.getBreadth() > k;k++)
				//define the cut of sequence numbers this goal will determine
				{
					portionIndex[k] = (k*portionSize);
				}
			}
			
			while(spares>0)
			{
				//Have spares
				for(int spa = 0; this.getBreadth() >spa;spa++)
				{
					for(int cut = (spa+1); this.getBreadth()>cut;cut++)
					{
						portionIndex[cut] = portionIndex[cut]+1;
					}
					portionSizeIndex[spa] = portionSizeIndex[spa]+1;
					spares--;
					if(0>=spares)
					{
						break;
					}
				}
			}
			//else no spares, clean cut..
						
			for(int j = 0; this.getBreadth() >j;j++)
			{
				System.out.println("For grid: "+iLength+", portion index: "+j+" is at sequence Index: "+portionIndex[j]+" with length: "+portionSizeIndex[j]);
				
				Goal thisGoal = newGoals.elementAt(j);
				Vector<ActionSequence> subGrid = new Vector<ActionSequence>();
				for(int g = 0; contents.size()>g;g++)
				{
					//for each action sequence in the grid...
					ActionSequence thisSequence =  new ActionSequence(this.getNumberOfAttributes());
					ActionSequence longSequence = contents.elementAt(g);
					thisSequence.setStartState(longSequence.getStartState());
					for(int c = 0; portionSizeIndex[j]>c;c++ )
					{
						int cutoffSet = portionIndex[j];
						thisSequence.addAction(longSequence.getAction(cutoffSet+c));
					}
					subGrid.add(thisSequence);
				}
				if(subGrid.size()>0)
				{
					System.out.println("Generating plan:" +(j+1)+ " for goal: "+thisGoal.getIDNumber());
					generateTreePlanLevel(rootPlan, thisGoal, subGrid);
				}
				this.goalCollection.remove(thisGoal);
				this.goalCollection.add(thisGoal);
			}
		}
		else //We can't split at the desired breadth...
		{
			int portionSize = new Double(Math.ceil(iLength/newGoals.size())).intValue();//this is how big each portion should be.
			int spares = iLength-(portionSize*this.getBreadth());
			System.out.println("iLength: "+iLength+", newGoals.size():"+newGoals.size()+",Portions will be: "+portionSize+" with spares: "+spares);
			for(int j =0; newGoals.size()>j;j++)
			{
				//for each new goal take our portion then pass it off..
				Goal thisGoal  = newGoals.elementAt(j);
				Vector<ActionSequence> thisGoalDetermines = new Vector<ActionSequence>();
				
				int [] portionIndex = new int[this.getBreadth()];
				for(int k = 0; portionIndex.length > k;k++)//define the sequence numbers this goal will determine
				{
					portionIndex[k] = (j*portionSize)+k;
				}
									
				//for every sequence in the grid
				for(int k = 0; contents.size()>k;k++)
				{
					//We need to take the sequences determined
					ActionSequence masterSequence = contents.elementAt(k);
					ActionSequence newSequence = (ActionSequence)masterSequence.clone();
					newSequence.clearActions();
					//take the actions in the master sequence and add them into the new Sequence..
						
					for(int a = portionIndex[j]; (portionIndex[j]+portionSize)>a ;a++)
					{
						//System.out.println("a:"+a+", upper lim: "+(portionIndex[j]+this.getBreadth()));
						Action thisAction = masterSequence.getAction(a);
						newSequence.addAction(thisAction);
					}
					thisGoalDetermines.add(newSequence);
				}
				if(thisGoalDetermines.size()>0)
				{
					generateTreePlanLevel(rootPlan, thisGoal,thisGoalDetermines);
				}
				this.goalCollection.remove(thisGoal);
				this.goalCollection.add(thisGoal);
				
			}
		}
		
		return true;
	}
	
	public boolean checkSorting()
	{
		boolean sorted = true;
		int preState = -1;
		for(int i = 0; this.sequenceCollection.size()>i;i++ )
		{
			if(this.sequenceCollection.elementAt(i).getSequenceLength()>=preState)
			{
				//cool seems sorted
				preState = this.sequenceCollection.elementAt(i).getSequenceLength();
			}
			else
			{
				sorted = false;
				break;
			}
		}
		return sorted;
	}
	
	
	/**
	 * You may have to run this multiple times, reason being that I didn't want to have multiple copies of the actions sequences stored. Seeing memory could become an issue and processing time isn't in this case, this was implemented.
	 */
	public void sortSequences()
	{
		for(int i = 0; this.sequenceCollection.size()>i ;i++)
		{
			//System.out.println("Sorting element "+ i);
			ActionSequence thisSequence =  this.sequenceCollection.elementAt(i);
			this.sequenceCollection.remove(i);
			boolean inserted = false; 
			for(int j = 0; this.sequenceCollection.size()>j;j++)
			{
				ActionSequence sortSequence = this.sequenceCollection.elementAt(j);
				//System.out.println("Comparing "+sortSequence.getSequenceLength()+">" +thisSequence.getSequenceLength());
				if(sortSequence.getSequenceLength()>=thisSequence.getSequenceLength())
				{
					//System.out.println("Insert Point Found");
					sequenceCollection.insertElementAt(thisSequence, j);
					inserted = true;
					break;
				}
			}
			if(!inserted)
			{
				this.sequenceCollection.add(thisSequence);
			}
		}
	}
	
	
	
	/**
	 * This method breaks the logic of the Goal-Plan tree.
	 */
	public void buildDropOrTreeBroken()
	{
		System.out.println("Building Broken Drop OR Tree");
		for(int i = 0; this.sortedSets.size()>i;i++)
		{
			Vector<Vector<Action>> thisRow = this.sortedSets.elementAt(i);
			
			Plan topPlan = new Plan(""+(i+1));//read, solve state in X actions.
			topPlan.setHandleGoal(this.topLevelGoal.getIDNumber());
			topPlan.setPreState(true);
			topPlan.setInitPreState(true);
			topPlan.setDepth(1);
			Vector<Goal> endGoalCollection = new Vector<Goal>();//the goals on the bottom layer...
			Vector<Plan> endPlanCollection = new Vector<Plan>();//the plans at the bottom layer....
			for(int j = 0; thisRow.size()>j;j++)
			{
				Vector<Action> thisColumn = thisRow.elementAt(j);
				Goal sequenceGoal = new Goal("_EndGoal_"+(i+1)+"_"+(j+1));
				sequenceGoal.setDepth(this.getDepth());
				endGoalCollection.add(sequenceGoal);
				
				for(int k = 0; thisColumn.size()>k;k++)
				{
					Action step = thisColumn.elementAt(k);
					Plan planStep = new Plan("_EndPlan_"+(i+1)+"_"+(j+1)+"_"+(k+1));
					planStep.setHandleGoal(sequenceGoal.toNameString());
					planStep.addObjectToBody(step);
					planStep.setInitPreState(true);
					planStep.setPreState(true);
					endPlanCollection.add(planStep);
					//this.planCollection.add(planStep);
				}
				
			}
			this.planCollection.add(topPlan);
			System.out.println("Building sorted set: "+i);
			buildTreeMiddle(topPlan,endGoalCollection,endPlanCollection);

		}
		System.out.println("Done Building Tree");
	}
	
	/**
	 * Assumes actions have been built and sequences constructed and sorted into numbered sets.
	 * 
	 */
	public void buildFlatOrTree()
	{
		System.out.println("Building Flat OR Tree");
		for(int i = 0; this.sortedSets.size()>i;i++)
		{
			Vector<Vector<Action>> thisRow = this.sortedSets.elementAt(i);
			Plan topPlan = new Plan((this.planCollection.size()+1));
			topPlan.setHandleGoal(this.topLevelGoal.getIDNumber());
			topPlan.setPreState(true);
			topPlan.setInitPreState(true);
			
			for(int j = 0; thisRow.size()>j;j++)
			{
				Vector<Action> thisColumn = thisRow.elementAt(j);
				Goal sequenceGoal = new Goal(topPlan.getIDNumber()+"_"+j);
				topPlan.addObjectToBody(sequenceGoal);
				sequenceGoal.setDepth(1);
				this.goalCollection.add(sequenceGoal);
				for(int k = 0; thisColumn.size()>k;k++)
				{
					Action step = thisColumn.elementAt(k);
					Plan planStep = new Plan(sequenceGoal.getIDNumber()+"_"+k);
					planStep.setHandleGoal(sequenceGoal.toNameString());
					planStep.addObjectToBody(step);
					planStep.setInitPreState(true);
					planStep.setPreState(true);
					this.planCollection.add(planStep);
				}
			}
			this.planCollection.add(topPlan);
		}
		System.out.println("Done Building Tree");
	}
	
	public void printSequenceSets()
	{
		System.out.println("Currerently we have: "+this.sortedSets.size()+" sets" );
		for(int i = 0; this.sortedSets.size() >i;i++)
		{
			System.out.println("-----------------------------");
			Vector<Vector<Action>> thisRow = this.sortedSets.elementAt(i);
			System.out.println("This Row has: "+thisRow.size()+ " columns");
			for(int j = 0; thisRow.size()>j;j++)
			{
				Vector<Action> thisColumn = thisRow.elementAt(j);
				System.out.println("This column has: "+thisColumn.size()+" unique actions");
				System.out.print("Actions: ");
				for(int k = 0; thisColumn.size()>k;k++)
				{
					System.out.print(thisColumn.elementAt(k).toNameString()+ " ");
				}
				System.out.print("\n");
				
			}
			System.out.println("-----------------------------");
		}
		
		
	}
	
	
	
	
	public void buildSequenceSets()
	{
		//build up table structure for sorting the variables....
		for (int i = 0; (this.getNumberOfAttributes()+1)>i;i++)
		{
			Vector<Vector<Action>> thisRow = new Vector<Vector<Action>>();
			for(int j = 0; i>j;j++)
			{
				Vector<Action> thisColumn = new Vector<Action>();
				thisRow.add(thisColumn);
			}
			this.sortedSets.add(thisRow);
		}
		//done building table.....
		
		for (int i =0; this.sequenceCollection.size()>i;i++)//go through all sequences..
		{
			ActionSequence theSequence = this.sequenceCollection.elementAt(i);
			int length = theSequence.getSequenceLength();
			Vector<Vector<Action>> thisRow = this.sortedSets.elementAt(length);
			
			for(int j = 0; length >j;j++)//go through the sequence...
			{
				Vector<Action> thisColumn = thisRow.elementAt(j);
				Action performedAction = theSequence.getAction(j);
				if(!thisColumn.contains(performedAction))
				{
					thisColumn.add(performedAction);
					thisRow.set(j, thisColumn);
				}
			}
			this.sortedSets.set(length, thisRow);
			
		}
	}
	
	
	/**
	 * Returns the hamming distance to the start state.
	 * Strange. does this have the implied use of the goal? Yes. Removed additional (read unrequired) bs parametre 
	 * @param bs1
	 * @param bs2
	 * @return
	 */
	public int getHammingDistance(BitSet bs1)
	{
		int distance =0;
		for( int i = 0; this.getNumberOfAttributes()>i;i++)
		{
			if(isDontCare(i))
			{}
			else
			{
				if(bs1.get(i)!= this.goalState.get(i))
				{
					distance++;
				}
			}
		}
		
		return distance;
	}
	
	
	
	public void generateActionsSmart()
	{
		for(int i = 0; bitSetCollection.size()>i; i++)
		{
			//for every state
			ActionSequence mySequence = new ActionSequence(this.getNumberOfAttributes());//DS we put our information into...
			BitSet startState = (BitSet)bitSetCollection.elementAt(i).clone();
			int thisHammingDistance = this.getHammingDistance(startState); 
			mySequence.setStartState((BitSet)startState.clone());
			while(thisHammingDistance> 0)
			{
				boolean actionUsed = false;
				for(int j = 0; this.getNumberOfAttributes()>j; j++)
				{
					if(!isDontCare(j))
					{
						if(startState.get(j)!=goalState.get(j))
						{
							//We need to change this variable.
							int actionNumber = findAction(startState, j);
							System.out.println("Use action: "+actionNumber);
							if(actionNumber>-1)
							{
								//we have an action we can use.
								System.out.print("------------------------------\nFinding Action for state: ");
								this.printBitSet(startState);
								System.out.print("Try action: "+actionCollection.elementAt(actionNumber).getNameString()+" profile:    ");
								actionCollection.elementAt(actionNumber).printStringRepresentation();
								System.out.print("\n---------------------------");
							
								Action useMe = actionCollection.elementAt(actionNumber);
								int modifiedBit = this.findChangeBit(useMe);//redundant use, but a bit more modular
								if(modifiedBit>-1)
								{
									//could be a problem. Well maybe not. Once we break out of the boolean only area we will need to read the precondition. Mind you this breaks a lot more than this.
									startState.flip(modifiedBit);
									mySequence.addAction(useMe);
									actionUsed = true;
									break;
								}
							}
							else
							{//we don't have an action we can use, but we still need to change this bit.
							}
						}
					}
					else
					{
						System.out.println("We don't care about bit: "+j);
					}
				}
				
				if(!actionUsed)
				{
					//we have absolutely no actions we can use to modify the world state...
					for(int j = 0; this.getNumberOfAttributes()>j; j++)
					{
						//find the first point of difference...
						if(startState.get(j)!=goalState.get(j))
						{
							//we are going to change this variable...
							int actionNumber = -1;
							this.generateAction((BitSet)startState.clone(), j);//make it
							actionNumber = findAction((BitSet)startState.clone(), j);//find it...
							
							if(actionNumber>-1)
							{
								//cool, now we have one
								Action useMe = actionCollection.elementAt(actionNumber);
								int modifiedBit = this.findChangeBit(useMe);
								if(modifiedBit>-1)
								{
									startState.flip(modifiedBit);
									mySequence.addAction(useMe);
								}
							}
							else
							{
								System.out.println("Could not find new action.");
								System.exit(0);
							}
						}
					}
				}
				actionUsed = false;
				//recalculate the ham distance to hopefully escape the loop!
				thisHammingDistance =  this.getHammingDistance(startState);
				System.out.println("Hamming Distance :"+thisHammingDistance);
				
			}	
				
			
			
			
			this.sequenceCollection.add(mySequence);
		}
		
		
	}

	public boolean stateEqualsGoal(BitSet bs1)
	{
		boolean areEqual = false;
		if(this.goalMask==null)
		{
			return bs1.equals(goalState); 
		}
		else
		{
			areEqual = true;
			//Goal State has a mask...
			for(int i =0; goalMask.length()>i; i++)
			{
				if(goalMask.charAt(i)=='x'||goalMask.charAt(i)=='X')
				{
					//is don't care...so we dont!
				}
				else if(goalMask.charAt(i)=='1')
				{
					//mask is one.
					boolean thisState = bs1.get(i);
					if(thisState)
					{
						//state is also one
						
					}
					else
					{
						areEqual = false;
						break;
					}
				}
				else if(goalMask.charAt(i)=='0')
				{
					boolean thisState = bs1.get(i);
					if(!thisState){}
					else
					{
						areEqual = false;
						break;
					}
					
				}
				
				
			}
		}

		return areEqual;
	}
	
	public void generateActions()
	{
		for(int i = 0; bitSetCollection.size()>i; i++)
		{
			ActionSequence mySequence = new ActionSequence(this.getNumberOfAttributes());
			//if(bitSetCollection.elementAt(i).equals(goalState))
			if(this.stateEqualsGoal(bitSetCollection.elementAt(i)))
			{
				//do nothing as we need no actions to get to this point.
				mySequence.setStartState(goalState);
				//this.sequenceCollection.add(mySequence);
			}
			else
			{
				
				//we need to perform some modification to get from this start state to the goal state.
				BitSet startState = (BitSet)bitSetCollection.elementAt(i).clone();
				mySequence.setStartState((BitSet)startState.clone());
				
				
				for(int j = 0; this.getNumberOfAttributes()>j; j++)
				{
					if(!this.isDontCare(j))
					{	
						if(startState.get(j)!=goalState.get(j))
						{
							//we need an action to change this bit 
							if(actionCollection.size()<=0)
							{
								//no actions exist.
								//System.out.print("Generating Action for state: ");
								//this.printBitSet(startState);
								//System.out.print("\n");
								this.generateAction(startState, j);
								int actionNumber = findAction(startState, j);
								if(actionNumber>-1)
								{
									Action useMe = actionCollection.elementAt(actionNumber);
									int modifiedBit = this.findChangeBit(useMe);
									if(modifiedBit>-1)
									{
										startState.flip(modifiedBit);
										mySequence.addAction(useMe);
									}
								}
							}
							else
							{
								//we have actions, but do we have one to change this bit?
								int actionNumber = findAction(startState, j);
								if(actionNumber>-1)
								{
									//we have an action we can use....
									//System.out.print("------------------------------\nFinding Action for state: ");
									//this.printBitSet(startState);
									//System.out.print("Try action: "+actionCollection.elementAt(actionNumber).getNameString()+" profile:    ");
									//actionCollection.elementAt(actionNumber).printStringRepresentation();
									//System.out.print("\n---------------------------");
										
									Action useMe = actionCollection.elementAt(actionNumber);
									int modifiedBit = this.findChangeBit(useMe);//redundant use, but a bit more modular
									if(modifiedBit>-1)
									{
										//could be a problem. Well maybe not. Once we break out of the boolean only area we will need to read the precondition. Mind you this breaks a lot more than this.
										startState.flip(modifiedBit);
										mySequence.addAction(useMe);
									}
								}
								else
								{
									//we have actions but none that help. We need a new one...
									this.generateAction(startState, j);//make it
									actionNumber = findAction(startState, j);//find it...
									if(actionNumber>-1)
									{
										//cool, now we have one
										Action useMe = actionCollection.elementAt(actionNumber);
										int modifiedBit = this.findChangeBit(useMe);
										if(modifiedBit>-1)
										{
											startState.flip(modifiedBit);
											mySequence.addAction(useMe);
										}
									}
									else
									{
										//even making one failed...panic.
										System.out.println("ERROR: No actions found and could not create a new action..");
										System.exit(1);
									}
								}
							}
						}
					}
					else
					{
						System.out.println("We don't care about variable: "+j);
					}
				}
			}
			this.sequenceCollection.add(mySequence);
		}
	}
	
	

	
	
	/**
	 * 
	 * @param sortMe
	 * @return
	 */
	
	public int findChangeBit(Action sortMe)
	{
		String [] actionProfile = sortMe.getStringRepresentation();
		int index = -1;
		for(int i = 0; actionProfile.length> i;i++)
		{
			if(actionProfile[i].equals("c")|| actionProfile[i].equals("C"))
			{
				index = i;
				break;
			}
			
		}
		return index;
	}
	
	public boolean canUseAction(BitSet bs, Action thisAction)
	{
		boolean canBeUsed = true;
		String[] actionProfile = thisAction.getStringRepresentation();
		
		BitSet matchup = new BitSet(this.getNumberOfAttributes());
		matchup.clear();
		
		//this action changes the required variable
		//check preconditions.
		for (int j = 0; actionProfile.length>j; j++)
		{
			if(actionProfile[j].equals("x"))
			{
				//ignore
			}
			else if(actionProfile[j].equals("c"))
			{
				//ignore.. boolean should already be set...
			}
			else if(actionProfile[j].equals("1"))
			{
				//the action requires this variable to be true.
				if(bs.get(j)==true)
				{
					//this works
					matchup.set(j);
				}
				else
				{
					//doesn't work
					return false;
				}
			}
			else if(actionProfile[j].equals("0"))
			{
				//action requires this variable to be false...
				if(bs.get(j)==false)
				{
					//this works
					matchup.set(j);
				}
				else
				{
					//doesn't work
					return false;
				} 
			}
		}
		//end action inspection.
		return canBeUsed;
	}
	
	
	
	public int findAction(BitSet modifyMe, int changeBit)
	{
		int selectedIndex = -1;
		//System.out.println("Finding Action for set: "+this.stringBitSet(modifyMe)+" to change bit :"+changeBit);
		//System.out.println("Modify Me bit: "+modifyMe.get(changeBit));
		
		for(int i = 0; actionCollection.size()>i;i++)
		{
			Action thisAction = (Action)actionCollection.elementAt(i).clone();
			String[] actionProfile = thisAction.getStringRepresentation();
			BitSet matchup = new BitSet(this.getNumberOfAttributes());
			matchup.clear();
			//System.out.println("Finding Action for set: "+this.stringBitSet(modifyMe));
			if(actionProfile[changeBit].equals("c") || actionProfile[changeBit].equals("C"))
			{//Change position matches
				//System.out.println("Position Matches, modMe bit: "+modifyMe.get(changeBit)+" toTrue:" +thisAction.isToTrue());
				boolean matchedTransistion = false;
				if(!modifyMe.get(changeBit) && thisAction.isToTrue()) //0->1
				{
					
					matchedTransistion = true;
				}
				else if(modifyMe.get(changeBit) && !thisAction.isToTrue()) //1->0
				{
					matchedTransistion = true;
				}
				if(matchedTransistion)
				{
					//System.out.println("Transmission Passed");
					//Transition matched.
					//this action changes the required variable
					//check preconditions.
					matchup.set(changeBit);
				
					for (int j = 0; actionProfile.length>j; j++)
					{
						if(actionProfile[j].equals("x"))
						{
							matchup.set(j);
							//ignore
						}
						else if(actionProfile[j].equals("c"))
						{
							//ignore.. bool should already be set...
							matchup.set(j);
						}
						else if(actionProfile[j].equals("1"))
						{
							//the action requires this variable to be true.
							if(modifyMe.get(j)==true)
							{
								//this works
								matchup.set(j);
							}
							else
							{
								//next action
								break;
							}
						}
						else if(actionProfile[j].equals("0"))
						{
							//action requires this variable to be false...
							if(modifyMe.get(j)==false)
							{
								//this works
								matchup.set(j);
							}
							else
							{
								//next action..
								break;
							} 
						}
					}
				
					//end action inspection.
					if(matchup.cardinality()==this.getNumberOfAttributes())
					{
						//matches up 100%
						selectedIndex = i;
						//selectedAction = actionCollection.elementAt(i);
						//solutionFound = true;
						
						//System.out.print("Try action: "+selectedAction.getNameString()+" profile:    ");
						//selectedAction.printStringRepresentation();
						//System.out.print("\n---------------------------");
						//return selectedIndex;
					}	
				}
				else
				{
					//System.out.println("Transition failed");
				}
			}
			else
			{
				
			}
			
		}
		//System.out.println("Returning action reference :"+selectedIndex);
		return selectedIndex;
	}
	
	
	/**
	 * Creates an action which modifies the specified bit. 
	 * Creates preconditions based on the remaining parts of the BitSet. 
	 * The number retained of preconditions retained are equal to the value specified in the class value.  
	 * @param modifyMe
	 * @param changeBit
	 */
	public void generateAction(BitSet modifyMe, int changeBit)
	{
		System.out.println("Making action for "+this.stringBitSet(modifyMe)+" to change bit :"+changeBit);
		String[] actionProfile = new String[this.getNumberOfAttributes()];
		for(int i = 0; this.getNumberOfAttributes()>i;i++)
		{
			actionProfile[i] = "x";
		}	
		actionProfile[changeBit] = "c";
		
		
		Action newb = new Action(""+(this.getNewActionNumber()), this.getNumberOfAttributes());
		
		if(modifyMe.get(changeBit)==true)
		{
			newb.setToTrue(false);
		}
		else if(modifyMe.get(changeBit)==false)
		{
			newb.setToTrue(true);
		}
		boolean postState = modifyMe.get(changeBit);
		newb.setPostStatement(alphabet[changeBit]+"="+!postState);
		int [] preConds = new int[this.getNumberOfPreconditions()];
		for(int i = 0; preConds.length >i;i++)
		{
			preConds[i] = -1;
		}
		
		//System.out.println("We generate: "+this.getNumberOfPreconditions()+" preconditions");
		//generate random preconditions...
		for(int i = 0; this.getNumberOfPreconditions()>i;i++)
		{
			int randInt = randGen.nextInt(this.getNumberOfAttributes());
			boolean alreadySelected = false;
			for(int j = 0; i>j;j++)
			{
				if(randInt==preConds[j])
				{
					alreadySelected =true;
					break;
				}
			}
			if(randInt==changeBit)
			{
				i--;
			}
			else if(alreadySelected)
			{
				i--;
			}
			else
			{
				if(modifyMe.get(randInt))
				{
					actionProfile[randInt] = "1";
				}
				else
				{
					actionProfile[randInt] = "0";	
				}
				preConds[i] = randInt;
			}
		}
		//build pre statement
		String preStatement = "";
		for(int i = 0; preConds.length >i; i++)
		{
			//we may want to remove some of the world states. See how things pan out.
			//not the bit which is modified...
			preStatement = preStatement + "("+alphabet[preConds[i]]+"=="+modifyMe.get(preConds[i])+")";
			//statement length is all-1;
			if((i+1)==preConds.length){}
			else 
			{
				preStatement = preStatement +" &&";
			}
		}
		newb.setPreStatement(preStatement);
		newb.setStringRepresentation(actionProfile);
		System.out.println("New action created: "+newb);
		actionCollection.add(newb);
		//System.out.print("Action: ");
		//newb.printStringRepresentation();
		//System.out.print("\n");
		//System.out.print(newb);
	}
	
	
	public void generateAttributes(int number)
	{
		//String[] alphabet = this.generateAlphabet();
		for(int i = 0; number>i;i++)
		{
			Attribute temp  = new Attribute(true, true, alphabet[i], 1);
			attributeCollection.add(temp);
		}
	}
	
	public void generateAllWorldStates()
	{
		BitSet startState = new BitSet(this.getNumberOfAttributes());
		startState.clear();
		BitSet addState = new BitSet(this.getNumberOfAttributes());
		addState.set(this.getNumberOfAttributes()-1);
		bitSetCollection.add((BitSet)startState.clone());
		while(this.getNumberOfAttributes()> startState.cardinality())
		{
			startState = addBitSets(startState, addState);
			bitSetCollection.add((BitSet)startState.clone());
		}
		//System.out.println("Generated: "+bitSetCollection.size()+" world states.");
		/*for (int i = 0; bitSetCollection.size()>i;i++)
		{
			this.printBitSet(bitSetCollection.elementAt(i));
		}*/
	}
	
	public void printBitSet(BitSet bs)
	{
		for(int i= 0 ; this.getNumberOfAttributes()>i;i++)
		{
			if(bs.get(i)==true)
			{
				System.out.print("1");
			}
			else if (bs.get(i)==false)
			{
				System.out.print("0");
			}
		}
		System.out.print("\n");
	}
	
	public String stringBitSet(BitSet bs)
	{
		String ret = "";
		for(int i= 0 ; this.getNumberOfAttributes()>i;i++)
		{
			if(bs.get(i)==true)
			{
				ret = ret+"1";
			}
			else if (bs.get(i)==false)
			{
				ret = ret+"0";
			}
		}
		return ret;
	}
	
	public void printSequences()
	{
		for( int i = 0; this.sequenceCollection.size()>i ;i++)
		{
			System.out.println(this.sequenceCollection.elementAt(i));
		}
		System.out.println("Total number of Sequences: "+this.sequenceCollection.size());
	}
	
	public BitSet addBitSets(BitSet bs1, BitSet adder)
	{
		
		//System.out.println("------------------------");
		//System.out.print("Adding: ");
		//this.printBitSet(bs1);
		//System.out.print("Adding: ");
		//this.printBitSet(adder);
		boolean carry = false;
		BitSet stolenAdder = (BitSet)adder.clone();
		for(int i = this.getNumberOfAttributes()-1; i>-1;i--)
		{
			
			if(bs1.get(i)==true && stolenAdder.get(i)==true)
			{
				carry = true;
			}
			if(carry)
			{
				if(0> (i-1))
				{
					System.out.println("overflow");
					break;
				}
				else
				{
					stolenAdder.set(i-1);
				}
				carry = false;
			}
		
		}
		bs1.xor(stolenAdder);
		//System.out.println("------------------------");
		//System.out.print("Result: ");
		//this.printBitSet(bs1);
		return bs1;
	}
	
	public boolean numberOfTrueStates(BitSet testMe)
	{
		if(testMe.cardinality()==this.getNumberOfAttributes())
		{
			return true;
		}
		else
		{
			return false;
		}
		
	}
	
	/**
	 * Places all the letters of the alphabet into a String array
	 * @return String array with all letters of the alphabet
	 */
	public void generateAlphabet()
	{
		alphabet = new String[]{"a","b","c", "d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};

	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getDepth() {
		return depth;
	}

	public void setNumberOfAttributes(int numberOfAttributes) {
		this.numberOfAttributes = numberOfAttributes;
	}

	public int getNumberOfAttributes() {
		return numberOfAttributes;
	}

	public void setBreadth(int breadth) {
		this.breadth = breadth;
	}

	public int getBreadth() {
		return breadth;
	}

	public void setGoalState(BitSet goalState) {
		this.goalState = goalState;
	}

	public BitSet getGoalState() {
		return goalState;
	}

	public void setNumberOfPreconditions(int numberOfPreconditions) {
		this.numberOfPreconditions = numberOfPreconditions;
	}

	public int getNumberOfPreconditions() {
		return numberOfPreconditions;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setFlatOr(boolean flatOr) {
		this.flatOr = flatOr;
	}

	public boolean isFlatOr() {
		return flatOr;
	}

	public void setDropOr(boolean dropOr) {
		this.dropOr = dropOr;
	}

	public boolean isDropOr() {
		return dropOr;
	}

	public void setGuidedTree(boolean guidedTree) {
		this.guidedTree = guidedTree;
	}

	public boolean isGuidedTree() {
		return guidedTree;
	}

	public void setSplitValue(int splitValue) {
		this.splitValue = splitValue;
	}

	public int getSplitValue() {
		return splitValue;
	}

	public void setGoalMask(String goalMask) {
		this.goalMask = goalMask;
	}

	public String getGoalMask() {
		return goalMask;
	}
	
	public int getGoalBreadth() {
		return goalBreadth;
	}

	public void setGoalBreadth(int goalBreadth) {
		this.goalBreadth = goalBreadth;
	}

	public void setPlanBreadth(int planBreadth) {
		this.planBreadth = planBreadth;
	}

	public int getPlanBreadth() {
		return planBreadth;
	}

	public void setIdealSequenceLength(int idealSequenceLength) 
	{
		System.out.println("Ideal Sequence length. entered value: "+idealSequenceLength);
		if(isOdd(idealSequenceLength))
		{
			System.out.println("This is an odd value");
			this.setIdealSequenceLengthLower(idealSequenceLength);
			this.setIdealSequenceLengthUpper(idealSequenceLength+1);
			this.setIdealLowIsOdd(true);
		}
		else
		{
			System.out.println("This is an even value");
			this.setIdealSequenceLengthLower(idealSequenceLength);
			this.setIdealSequenceLengthUpper(idealSequenceLength+1);
			this.setIdealLowIsOdd(false);
		}
		System.out.println("Set uppper limit to: "+(idealSequenceLength+1));
		this.idealSequenceLength = idealSequenceLength;
		//System.exit(0);
	}

	public int getIdealSequenceLength() {
		return idealSequenceLength;
	}

	public void setIdealSequenceLengthUpper(int idealSequenceLengthUpper) {
		this.idealSequenceLengthUpper = idealSequenceLengthUpper;
	}

	public int getIdealSequenceLengthUpper() {
		return idealSequenceLengthUpper;
	}

	public void setIdealSequenceLengthLower(int idealSequenceLengthLower) {
		this.idealSequenceLengthLower = idealSequenceLengthLower;
	}

	public int getIdealSequenceLengthLower() {
		return idealSequenceLengthLower;
	}

	public void setIdealLowIsOdd(boolean idealLowIsOdd) {
		this.idealLowIsOdd = idealLowIsOdd;
	}

	public boolean isIdealLowIsOdd() {
		return idealLowIsOdd;
	}

	public void setActionCounter(int actionCounter) {
		this.actionCounter = actionCounter;
	}

	public int getActionCounter() {
		return actionCounter;
	}

	public void setExtendedDrop(boolean extendedDrop) {
		this.extendedDrop = extendedDrop;
	}

	public boolean isExtendedDrop() {
		return extendedDrop;
	}
}
