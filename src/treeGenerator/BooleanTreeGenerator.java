package treeGenerator;
import java.util.*;
import java.io.*;


/**
 * Creates a tree for use with the BDI Learning Agents
 * @author Scott Parkinson: scottjp@cs.rmit.edu.au
 *
 */

public class BooleanTreeGenerator implements TreeGenerator
{
	private int treeDepth;
	private String fileName; 
	private Vector<Attribute> attributeCollection;
	private Vector<Goal> goalCollection;
	private Vector<Plan> planCollection;
	private Vector<String> boolOpCollection;
	private Vector<Action> actionCollection;
	
	private String[] alphabet;
	private String[] upperAlphabet;
	//private int maxSingleSetSize;
	
	private double typeLikely;
	//private double observableLikely;
	private int minBreadthPerNode;
	private int maxBreadthPerNode;
	
	
	/**
	 *  Constructor
	 */
	public BooleanTreeGenerator()
	{
		setUp();
	}
	
	/**
	 * Constructor
	 * @param newFileName The filename which we will write the tree to. ".txt" is appended to the end to the file name provided
	 */
	public BooleanTreeGenerator(String newFileName)
	{
		this.setFileName(newFileName);
		setUp();
	}
	
	
	/**
	 * Sets up variables for use in the tree generator.
	 */
	public void setUp()
	{
	
		attributeCollection = new Vector<Attribute>();
		planCollection = new Vector<Plan>();
		goalCollection = new Vector<Goal>();
		actionCollection =new Vector<Action>();
		
		alphabet = generateAlphabet();
		upperAlphabet = this.toUpperStringArray(alphabet);
		this.treeDepth = 4;
		boolOpCollection = new Vector<String>();
		boolOpCollection.add("==");
		boolOpCollection.add("!=");
		this.setMinBreadthPerNode(2);
		this.setMaxBreadthPerNode(2);
		
	}
	
	/**
	 * Converts the string array characters to upper case.
	 * @param lower String array
	 * @return passed string array in upper case
	 */
	public String[] toUpperStringArray(String[] lower)
	{
		String[] passback = new String[lower.length];
		for(int i=0; lower.length>i;i++)
		{
			passback[i] = lower[i].toUpperCase();
		}		
		return passback;
	}
	
	
	/**
	 * Places all the letters of the alphabet into a String array
	 * @return String array with all letters of the alphabet
	 */
	public String[] generateAlphabet()
	{
		String[] alphabet = {"a","b","c", "d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
		return alphabet;
	}
	
	/**
	 * Writes all the generated contents of the tree to the file name provided in the constructor.
	 */
	public void writeTreeFile()
	{
		String fileInfo = this.getBuiltTree();
		try
	    {
	    	PrintWriter prnOut = new PrintWriter(new FileOutputStream(fileName+".txt"), true);
			prnOut.println(fileInfo);
			prnOut.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: File could not be created."); 
		}
	}
	
	/**
	 * Generates a random attribute set for this tree.
	 */
	public void attributeGenerator()
	{
		int attributeSetSize = this.getTreeDepth();
		for(int i = 0; attributeSetSize > i; i++)
		{
			boolean observe;
			observe = true;
			boolean type;
			type = true;
			int attSize = 0;
			Attribute tempAttr = new Attribute(observe, type, alphabet[i], attSize);
			tempAttr.setSetValues(upperAlphabet);
			attributeCollection.add(tempAttr);
		}
	}
	/**
	 * Generates a tree with alternating layers of goals and plans
	 */
	public void buildTree()
	{
		attributeGenerator();
		Goal root = new Goal("InitialGoal");
		root.setDepth(0);
		goalCollection.add(root);
		int numberOfPlans = 2;
		for(int i = 0; numberOfPlans>i;i++)
		{
			Plan newPlan = new Plan(""+(i+1)+"");
			newPlan.setHandleGoal(root.getIDNumber());
			newPlan.setInitPreState(true);
			newPlan.setPreState(true);
			planCollection.add(newPlan);
			
		}
		
		for(int j =1;this.treeDepth>j;j++)
		{
			Vector<Plan> abovePlans = getPlansAtDepth(j-1);
			
			for(int p=0; abovePlans.size()>p; p++)
			{
				//for each plan in the level above generate a random selection of goals.
				int numberOfGoals = 1;//make clean bool tree
				for(int g=0; numberOfGoals>g;g++)
				{
					Goal myGoal = new Goal(abovePlans.elementAt(p).getIDNumber()+(g+1));
					myGoal.setDepth(j);
					goalCollection.add(myGoal);
					abovePlans.elementAt(p).addObjectToBody(myGoal);
				}
			}
			
			Vector<Goal> goalsAtDepth = this.getGoalsAtDepth(j);
			for(int g= 0; goalsAtDepth.size()>g; g++)
			{
				//for each goal generated, generate a random selection of plans
				int numberOfPlansAtLevel = 2;
				for(int p =0; numberOfPlansAtLevel>p;p++)
				{
					Plan myPlan = new Plan(goalsAtDepth.elementAt(g).getIDNumber()+(p+1));
					myPlan.setDepth(j);
					myPlan.setHandleGoal("G"+goalsAtDepth.elementAt(g).getIDNumber());
					myPlan.setInitPreState(true);
					myPlan.setPreState(true);
					
					planCollection.add(myPlan);
				}
			}
			//repeat.
		}
		generateAction();
	}
	
	/**
	 * Returns a Vector containing all the goals at a certain distance(depth) from the root node.
	 * @param depth Distance from the root node
	 * @return Vector with all the goals at the specified distance
	 */
	public Vector<Goal> getGoalsAtDepth(int depth)
	{
		Vector<Goal> returnVector = new Vector<Goal>();
		for( int i =0; goalCollection.size()>i;i++)
		{
			if(goalCollection.elementAt(i).getDepth()==depth)
			{
				returnVector.add(goalCollection.elementAt(i));
			}
			
		}
		return returnVector;
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
			returnString = returnString + attributeCollection.elementAt(i)+"\n";
		}
		for(int g=0; goalCollection.size()>g;g++)
		{
			returnString = returnString + goalCollection.elementAt(g)+"\n";
		}
		/**
		 * Actions would go in here.
		 * 
		 */
		for(int a= 0; actionCollection.size()>a; a++)
		{
			returnString = returnString + actionCollection.elementAt(a)+"\n";
			
		}
		
		//returnString = returnString + "#action definitions go here\n";
		for(int p=0; planCollection.size()>p;p++)
		{
			returnString = returnString + planCollection.elementAt(p)+"\n";
		}
		
		return returnString;
	}
	
	/**
	 * Returns a vector containing all the plans a certain depth from the root node. 
	 * @param depth The distance from the root node
	 * @return A Vector containing all the Plans at the specified distance from the root node. 
	 */
	public Vector<Plan> getPlansAtDepth(int depth)
	{
		Vector<Plan> returnVector = new Vector<Plan>();
		for(int i = 0; planCollection.size()>i;i++)
		{
			if(planCollection.elementAt(i).getDepth()==depth)
			{
				returnVector.add(planCollection.elementAt(i));
			}
		}
		return returnVector;
	}
	

	public void generateAction()
	{
		for(int i =0; this.attributeCollection.size()>i; i++)
		{
			Action trueSwitch = new Action((i+1)+"");
			trueSwitch.setPreStatement(this.attributeCollection.elementAt(i).getAttributeName()+"==false");
			trueSwitch.setPostStatement(this.attributeCollection.elementAt(i).getAttributeName()+"=true");
			actionCollection.add(trueSwitch);
			
			Action falseSwitch = new Action((attributeCollection.size()+1+i)+"");
			falseSwitch.setPreStatement(this.attributeCollection.elementAt(i).getAttributeName()+"==true");
			falseSwitch.setPostStatement(this.attributeCollection.elementAt(i).getAttributeName()+"=false");
			actionCollection.add(falseSwitch);
		}
		
	}
	
	public void placeActions()
	{
		boolean [] flippers = new boolean[this.getTreeDepth()];
		for(int i = 0; flippers.length>i;i++)
		{
			flippers[i] = false;
		}
		
		for (int i = 0; planCollection.size()> i;i++)
		{
			Plan tempPlan = planCollection.elementAt(i);
			int tempDepth = tempPlan.getDepth();
			if(flippers[tempDepth]==true)
			{
				tempPlan.addObjectToBody(actionCollection.elementAt((tempDepth*2)));
				flippers[tempDepth] = false;
			}
			else
			{
				tempPlan.addObjectToBody(actionCollection.elementAt((tempDepth*2)+1));
				flippers[tempDepth] = true;
			}
			planCollection.setElementAt(tempPlan,i);
			
		}
		
	}

	public void setTreeDepth(int treeDepth) {
		this.treeDepth = treeDepth;
	}

	public int getTreeDepth() {
		return treeDepth;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setTypeLikely(double typeLikely) {
		this.typeLikely = typeLikely;
	}

	public double getTypeLikely() {
		return typeLikely;
	}

	public void setMinBreadthPerNode(int minBreadthPerNode) {
		this.minBreadthPerNode = minBreadthPerNode;
	}

	public int getMinBreadthPerNode() {
		return minBreadthPerNode;
	}

	public void setMaxBreadthPerNode(int maxBreadthPerNode) {
		this.maxBreadthPerNode = maxBreadthPerNode;
	}

	public int getMaxBreadthPerNode() {
		return maxBreadthPerNode;
	}
	
}
