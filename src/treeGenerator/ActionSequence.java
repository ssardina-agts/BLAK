package treeGenerator;
import java.util.Vector;
import java.util.BitSet;

public class ActionSequence 
{
	private Vector<Action> mySequence;
	private BitSet startState;
	private int numberOfAttributes;
	private Vector<BitSet> visitedStates;
	
	//TODO Consider adding action and visit state clearing automatically....
	
	
	public ActionSequence(int numberAttributes)
	{
		mySequence = new Vector<Action>();
		visitedStates = new Vector<BitSet>();
		this.setNumberOfAttributes(numberAttributes);
	}

	public void addAction(Action addMe)
	{
		mySequence.add(addMe);
		//add in states visited update?
	}
	
	public void addVisitedState(BitSet addMe)
	{
		this.visitedStates.add(addMe);
	}

	public void setStartState(BitSet startState) 
	{
		this.startState = startState;
	}

	public BitSet getStartState() 
	{
		return startState;
	}
	
	public String toString()
	{
		String returnString = new String("From state: "+this.printBitSet(this.startState)+" use the actions: ");
		for(int i = 0; this.mySequence.size()>i;i++)
		{
			returnString = returnString+this.mySequence.elementAt(i).toNameString()+" "; 
		}
		returnString  = returnString + "\t"+this.mySequence.size();
		
		return returnString;
	}
	
	public String printBitSet(BitSet bs)
	{
		String output = new String();
		if(bs!=null)
		{
			for(int i= 0 ; this.getNumberOfAttributes()>i;i++)
			{
				if(bs.get(i)==true)
				{
					output = output+"1";
				}
				else if (bs.get(i)==false)
				{
					output = output+"0";
				}
			}
			return output;
		}
		else
		{
			return null;
		}
	}
	
	public Object clone()
	{
		ActionSequence myClone = new ActionSequence(this.getNumberOfAttributes());
		for(int i = 0; this.mySequence.size()>i;i++)
		{
			myClone.addAction(this.mySequence.elementAt(i));
		}
		myClone.setStartState((BitSet)this.getStartState().clone());
		myClone.setNumberOfAttributes(this.getNumberOfAttributes());
		
		for(int i =0; this.visitedStates.size()>i;i++)
		{
			myClone.addVisitedState((BitSet)this.visitedStates.elementAt(i).clone());
			
		}
		return myClone;
	}
	
	public int getVisitLength()
	{
		return this.visitedStates.size();
	}
	
	public int getSequenceLength()
	{
		return this.mySequence.size();
	}
	
	public BitSet getVisitedState(int visitNumber)
	{
		if(this.visitedStates.size()>visitNumber)
		{
			return this.visitedStates.elementAt(visitNumber); 
		}
		else
		{
			return null;
		}
		
	}
	
	public Action getAction(int sequenceNumber)
	{
		if(this.mySequence.size()>sequenceNumber)
		{
			return this.mySequence.elementAt(sequenceNumber);
		}
		else
		{
			return null;
		}
	}

	public void setNumberOfAttributes(int numberOfAttributes) 
	{
		this.numberOfAttributes = numberOfAttributes;
	}

	public int getNumberOfAttributes() {
		return numberOfAttributes;
	}
	
	public void clearVisits()
	{
		this.visitedStates.clear();
	}
	
	public void clearActions()
	{
		this.mySequence.clear();
	}

	public void setVisitedStates(Vector<BitSet> visitedStates) 
	{
		this.visitedStates = visitedStates;
	}

	public Vector<BitSet> getVisitedStates() 
	{
		return visitedStates;
	}
	
	public void populateVisitedStates()
	{
		System.out.println("Begin populate Visited States. Start State: "+this.stringBitSet(startState));
		this.visitedStates.clear();
		this.visitedStates.add((BitSet)this.getStartState().clone());
		BitSet workingSet = (BitSet)this.getStartState().clone();
		for(int i = 0; this.mySequence.size()>i;i++)
		{
			Action thisAction = mySequence.elementAt(i);
			String[] rep = thisAction.getStringRepresentation();
			for(int j = 0; rep.length>j;j++)
			{
				if(rep[j].equals("c") || rep[j].equals("C"))
				{
					if(!workingSet.get(j) && thisAction.isToTrue())//0->1
					{
						workingSet.flip(j);
						System.out.println("Applying action: "+thisAction.getNameString()+" ws->: "+this.stringBitSet(workingSet));
						break;
					}
					else if(workingSet.get(j) && !thisAction.isToTrue())//1->0
					{
						workingSet.flip(j);
						System.out.println("Applying action: "+thisAction.getNameString()+" ws->: "+this.stringBitSet(workingSet));
						break;
					}
					else
					{
						System.out.println("j: "+j+", currentState: "+workingSet.get(j)+" action transistion: "+thisAction.isToTrue());
						System.out.println("Action: "+thisAction);
						
						System.out.println("ActionSequence has failed in applying action, ActionSequence.populateVisted States");
						System.out.println("This sequence details: "+this.toString());
						
						
						for(int k = 0; this.mySequence.size() >k;k++)
						{
							System.out.println(this.mySequence.elementAt(k));
						}
						
						System.exit(0);
					}
				}
			}
			this.visitedStates.add((BitSet)workingSet.clone());
		}
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
	
	public boolean usesAction(Action thisAction)
	{
		if(this.mySequence.contains(thisAction))
		{
			return true;
		}
		return false;
		
	}
	
	public boolean compressionEqual(ActionSequence compareTo)
	{
		boolean eqiv = false;
		
		if(compareTo.getSequenceLength()!=this.getSequenceLength())
		{
			return false;
		}
		else
		{
			eqiv = true;
			for(int i = 0; this.mySequence.size()>i;i++)
			{
				if(this.mySequence.get(i).equals(compareTo.getAction(i)))
				{}
				else
				{
					return false;
				}
				
			}
			
		}
		
		return eqiv;
	}
	
	
}
