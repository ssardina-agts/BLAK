package treeGenerator;
import java.util.*;
import java.io.*;

/**
 * @author root
 *
 */
public class SmartTreeGenerator implements TreeGenerator
{



	/**
	 * Creates a tree for use with the BDI Learning Agents
	 * @author Scott Parkinson: scottjp@cs.rmit.edu.au
	 *
	 */


		private int treeDepth;
		private String fileName; 
		private Random randGen;
		private Vector<Attribute> attributeCollection;
		private Vector<Goal> goalCollection;
		private Vector<Plan> planCollection;
		private Vector<String> boolOpCollection;
		private Vector<Action> actionCollection;
		//private Vector<Path> pathCollection;
		
		
		private String[] alphabet;
		private String[] upperAlphabet;
		//private int maxSingleSetSize;
		
		//private double typeLikely;
		private double observableLikely;
		

			
		/**
		 *  Constructor
		 */
		public SmartTreeGenerator()
		{
			setUp();
		}
		
		/**
		 * Constructor
		 * @param newFileName The filename which we will write the tree to. ".txt" is appended to the end to the file name provided
		 */
		public SmartTreeGenerator(String newFileName)
		{
			this.setFileName(newFileName);
			setUp();
		}
		
		
		/**
		 * Sets up variables for use in the tree generator.
		 */
		public void setUp()
		{
			randGen = new Random();
			//this.typeLikely = 0.5;
			this.observableLikely = 0.5;
			//this.maxSingleSetSize = 10;
			
			attributeCollection = new Vector<Attribute>();
			planCollection = new Vector<Plan>();
			goalCollection = new Vector<Goal>();
			actionCollection = new Vector<Action>();
			//pathCollection = new Vector<Path>();
			
			alphabet = generateAlphabet();
			upperAlphabet = this.toUpperStringArray(alphabet);
			this.treeDepth = 2;
			boolOpCollection = new Vector<String>();
			boolOpCollection.add("==");
			boolOpCollection.add("!=");
		
			
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

		
		
		public void generateAction()
		{
			for(int i =0; this.attributeCollection.size()>i; i++)
			{
				Attribute thisAttr = attributeCollection.elementAt(i);
				if(thisAttr.isBool())
				{
					Action trueAction = new Action(""+(actionCollection.size()+1));
					trueAction.setPostStatement(thisAttr.getAttributeName()+" = true");
					actionCollection.add(trueAction);
					
					Action falseAction  = new Action(""+(actionCollection.size()+1));
					falseAction.setPostStatement(thisAttr.getAttributeName()+" = false");
					actionCollection.add(falseAction);
				}
				else
				{
					String [] variables = thisAttr.getSetValues();
					for (int j = 0; thisAttr.getLength()>j;j++)
					{
						Action newAction  = new Action (""+(actionCollection.size()+1));
						newAction.setPostStatement(thisAttr.getAttributeName()+" = \""+variables[j]+"\"");
						actionCollection.add(newAction);
					}
				}
			}
		}
		
		
		/**
		 * Generates an attribute set for this tree.
		 */
		public void attributeGenerator()
		{
			int attributeSetSize =5;
			//System.out.println("Generating "+attributeSetSize+" attributes.");
			for(int i =0; attributeSetSize > i; i++)
			{
				boolean observe;
				boolean type;
				type =true;//boolean
				int attSize = 0;//ignore
			
				if(randGen.nextDouble()>this.observableLikely)
				{
					observe = true;
				}
				else
				{
					observe = false;
				}
				Attribute tempAttr = new Attribute(observe, type, alphabet[i], attSize);
				tempAttr.setSetValues(upperAlphabet);
				attributeCollection.add(tempAttr);
			}
			System.out.println("Done building Attributes");
			for(int j=0; attributeCollection.size()>j;j++)
			{
				System.out.println("Attri: "+attributeCollection.elementAt(j));
			}
		}
		
		
		/**
		 * Places the actions into the tree. Redundant.
		 */
		public void placeActions()
		{}	
		
		/**
		 * Generates a random Action, 
		 * @return
		 */
		public Action randomAction()
		{
			Action newAction = new Action("Name me");
			
			return newAction;
		}
		
		public Action randomAction(Vector<Boolean> worldState)
		{
			Action newAction = new Action(""+(actionCollection.size()+1));
			//may go out of bounds...
			int flipBit = randGen.nextInt(worldState.size());
			
			Boolean flipMe = worldState.elementAt(flipBit);
			boolean initialValue = flipMe.booleanValue();
			String preStatement = "";
			for(int i = 0; worldState.size() >i; i++)
			{
				//we may want to remove some of the world states. See how things pan out.
				if(i!=flipBit)
				{
					//not the bit which is modified...
					preStatement = preStatement + "("+alphabet[i]+"=="+worldState.elementAt(i)+")";
					//statement length is all-1;
					if((i+1)==worldState.size()){}
					else if (flipBit ==(i+1) && (i+2)>=worldState.size()){}
					else 
					{
						preStatement = preStatement +" &&";
					}
				}
			}
					
			newAction.setPostStatement(alphabet[flipBit]+"="+!initialValue);
			//newAction.setChangeMode(!initialValue);
			//newAction.setChangeVariable(alphabet[flipBit]);
			newAction.setPreStatement(preStatement);
			
			
			return newAction;
		}
		
		public Action buildAction(Vector<Boolean> worldState, String attribute, boolean desiredState)
		{
			Action newAction = new Action(""+(actionCollection.size()+1));
			
			
			return  newAction;
		}
		
		
		
		
		public void generateRandomAction()
		{
		}
		
		/**
		 * Returns a Vector of Vectors containing random Boolean objects
		 * @return A Vector of Vectors containing random Boolean objects
		 */
		public Vector<Vector<Boolean>> generateRandomWorldStates(int numberOfStates)
		{
			Vector<Vector<Boolean>> stateCollection = new Vector<Vector<Boolean>>();
			int collectionLength;
			if(numberOfStates>0)
			{
				collectionLength = numberOfStates;
			}
			else
			{
				collectionLength = randGen.nextInt((attributeCollection.size()-2));
				collectionLength++;
				collectionLength++;
			}
				
			
			
			
			for(int i = 0; collectionLength>i;i++)
			{
				Vector<Boolean> thisState = new Vector<Boolean>();
				for (int j = 0; attributeCollection.size()>j; j++)
				{
					Boolean current = new Boolean(randGen.nextBoolean());
					thisState.add(current);
				}
				stateCollection.add(thisState);
			}
			return stateCollection;
		}

		public void cleanActions()
		{
			for(int i = 0; actionCollection.size()>i;i++)
			{
				Action current = actionCollection.elementAt(i);
				
				for(int j = 0; actionCollection.size()>j;j++)
				{
					if(i!=j)
					{
						Action compare = actionCollection.elementAt(j);
						if(current.equals(compare))
						{
							System.out.println("Duplicate action found. Action "+current.getNameString()+" same as "+compare.getNameString());
							actionCollection.removeElementAt(j);
							j--;
						}
					}
				}
			}
			
		}
		
		
		/**
		 * Generates a tree with alternating layers of goals and plans
		 * Breadth of nodes below each node is min 2, max 5.
		 */
		public void buildTree()
		{
			attributeGenerator();
			Vector<Vector<Boolean>> randomStates = this.generateRandomWorldStates(40);
			
			for(int rs = 0; randomStates.size()>rs ;rs++)
			{
				Action newAction = randomAction(randomStates.elementAt(rs));
				actionCollection.add(newAction);
			}
			//we now have some actions from the random states.
			cleanActions();//remove duplicate actions.
		
			this.dumpActionCollection();//prints actions
			
			//Build the actions into some goals and plans.
			for (int a= 0; actionCollection.size()>a;a++)
			{
				Goal newGoal = new Goal(""+goalCollection.size());
				newGoal.setDepth(this.getTreeDepth());
				goalCollection.add(newGoal);
				
				Plan newPlan = new Plan(planCollection.size()+"");
				newPlan.setHandleGoal(newGoal.toNameString());
				newPlan.setDepth(this.treeDepth);//set at Bottom of tree
				newPlan.addObjectToBody(actionCollection.elementAt(a));
				planCollection.add(newPlan);
				
				Path newPath = new Path();
				newPath.addObjectToPath(newGoal);
				newPath.addObjectToPath(newPlan);
				newPath.determineStartState();
			}
			
			
			
			int breaker = 0;
			while (1>breaker)
			{
				//int pool = this.getTreeDepth()*this.getTreeDepth();
				int pool = 2;
				Vector<Vector<Boolean>> randomStart = this.generateRandomWorldStates(pool);
				Vector<Vector<Boolean>> randomEnd = this.generateRandomWorldStates(pool);
				for(int i = 0; pool>i;i++)
				{
					Vector<Boolean> start = randomStart.elementAt(i);
					Vector<Boolean> end = randomEnd.elementAt(i);
					int difference = this.getStateDifference(start, end);
					System.out.println("Difference: "+difference);
					Vector<Vector<Boolean>> randyStates = this.generateRandomWorldStates(2);
					
					Path newPath = new Path();
					newPath.setStartState(start);
					newPath.addEndState(end);
					for(int rand = 0; randyStates.size()>rand;rand++)
					{
						newPath.addEndState(randyStates.elementAt(rand));
					}
					newPath.populateChangeState();
					
					//System.out.println("path Chnage state +"i"+: "+newPath)
					
				}
				
				breaker++;
			}

			
			
			//System.exit(0);
			/*Goal root = new Goal("InitialGoal");
			root.setDepth(0);
			goalCollection.add(root);
			int numberOfPlans = randGen.nextInt(3)+2;//min plans 2, max 5!
			for(int i = 0; numberOfPlans>i;i++)
			{
				Plan newPlan = new Plan(""+(i+1)+"");
				newPlan.setHandleGoal(root.getIDNumber());
				planCollection.add(newPlan);
				
			}
			
			for(int j =1;this.treeDepth>j;j++)
			{
				Vector<Plan> abovePlans = getPlansAtDepth(j-1);
				
				for(int p=0; abovePlans.size()>p;p++)
				{
					//for each plan in the level above generate a random selection of goals.
					int numberOfGoals = randGen.nextInt(3)+2;
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
					int numberOfPlansAtLevel = randGen.nextInt(3)+2;
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
			}*/
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
			for(int a = 0; actionCollection.size()>a;a++)
			{
				returnString = returnString +actionCollection.elementAt(a)+"\n";
				
			}
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
		
		
		/**
		 * Gives the number of difference world variables between two world states
		 * Assumes that t
		 * @param state1 The first state
		 * @param state2 The second state
		 * @return the number of differences
		 */
		public int getStateDifference(Vector<Boolean> state1, Vector<Boolean> state2)
		{
			int difference = 0;
			for(int i = 0; state1.size()>i; i++)
			{
				if(state1.elementAt(i).booleanValue()!=state2.elementAt(i).booleanValue())
				{
					difference++;
				}
			}
			
			return difference;
		}


		public void setTreeDepth(int treeDepth) 
		{
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

		public void dumpActionCollection()
		{
			System.out.println("Action Collection Dump");
			for (int i=0; actionCollection.size()>i;i++)
			{
				System.out.println(actionCollection.elementAt(i));
			}
		}
		
	}
