package treeGenerator;

import java.util.*;

public class Path implements TreeGenElement
{

		private Vector<TreeGenElement> completePath;//holds Goals, Plans and Actions... 
		private Vector<Boolean> startState;//the upper preconditions that this plan takes
		

		private Vector<Vector<Boolean>> endStates;//the final state this plan resides in....maybe not required.
		private Vector<Boolean> changeState01;//describes the changes this path can make to the world state
		private Vector<Boolean> changeState10;//describes the changes this path can make to the world state
		//private int depth;
		
		public Path()
		{
			completePath = new Vector<TreeGenElement>();
			startState = new Vector<Boolean>();
			endStates = new Vector<Vector<Boolean>>();
			changeState01 = new Vector<Boolean>();
			changeState10 = new Vector<Boolean>();
		}

		public String toString()
		{
			return "";
		}
		
		
		public String toNameString()
		{
			return "";
		}
		
		public Vector<Boolean> getStartState()
		{
			return this.startState;
		}
		
		public void populateChangeState()
		{
			//populate change state.
			for(int j =0; startState.size()>j;j++)
			{
				changeState01.add(new Boolean(false));
				changeState10.add(new Boolean(false));
			}
			//done.
			
			
			for(int i= 0; endStates.size()>i;i++)
			{
				Vector<Boolean> currentEndState = this.endStates.elementAt(i);
			
			
				for(int j =0; startState.size()>j;j++)
				{
					if(startState.elementAt(j).booleanValue()!=currentEndState.elementAt(j).booleanValue())
					{
						//value changes
						if(startState.elementAt(j).booleanValue()==true)
						{
							//1->0
							this.changeState10.setElementAt(new Boolean(true), j);
						}
						else if(startState.elementAt(j).booleanValue()==false)
						{
							//0->1
							this.changeState01.setElementAt(new Boolean(true), j);
						}
						else
						{
							//should not reach
							System.out.println("Logic suggests that a state change has occured but it could not be locallly detected.");
							System.exit(1);
						}
					}
				}
				System.out.println("Showing State Details....");
				System.out.println(stateToString(startState)+" <--Start State");
				System.out.println(stateToString(currentEndState)+" <--End State");
				System.out.println(stateToString(changeState01)+"+CS: 0->1");
				System.out.println(stateToString(changeState10)+"+CS: 1->0");
				
			}
		}
		
		public void setStartState(Vector<Boolean> startState) 
		{
			this.startState = startState;
		}
		
		public Vector<Boolean> setStartState() 
		{
			return this.startState;
		}
		
		public void addEndState(Vector<Boolean> addMe)
		{
			this.endStates.add(addMe);
		}
		
		public String stateToString(Vector<Boolean> stringMe)
		{
			String returnString ="";
			for(int i = 0; stringMe.size()>i;i++)
			{
				if(stringMe.elementAt(i).booleanValue()==true)
				{
					returnString = returnString+"1";
				}
				else if(stringMe.elementAt(i).booleanValue()==false)
				{
					returnString = returnString+"0";
				}
			}
			
			return returnString;
		}
		
		public Vector<Vector<Boolean>> giveEndPoints()
		{
			return this.endStates;
		}
		
		public void addObjectToPath(TreeGenElement addMe)
		{
			this.completePath.add(addMe);
		}
		
		public Vector<TreeGenElement> getPathVector()
		{
			return this.completePath;
		}
		
		
		public void determineStartState()
		{
			//Vector 
			
			
			for(int i = 0; completePath.size()>i; i++)
			{
				TreeGenElement temp = this.completePath.elementAt(i);
				if (temp instanceof Goal)
				{
					System.out.println("ElementAt: "+i+" is a goal");
				}
				else if(temp instanceof Plan)
				{
					System.out.println("ElementAt: "+i+" is a plan");
				}
			}
		}
}
