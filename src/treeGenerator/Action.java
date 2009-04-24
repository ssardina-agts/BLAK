package treeGenerator;

/**
 * Wrapper class for actions in a tree definition file.
 * @author Scott Parkinson: scottjp@cs.rmit.edu.au
 *
 */
public class Action implements TreeGenElement
{
	private String preStatement;
	private String postStatement;
	private String nameString;
	//private ChangeDescription transformer;
	private String[] stringRepresentation;
	private int numberOfAttributes;
	private boolean newTemp;
	private boolean toTrue;
	
	
	

	public Action(String name)
	{
		nameString = name;
		this.preStatement = null;
		this.postStatement = null;
		//this.transformer = new ChangeDescription("", false);
		this.setNewTemp(false);
		toTrue = false;
		
	}
	
	public Action(String name, int numberOfAttributes)
	{
		nameString = name;
		this.preStatement = null;
		this.postStatement = null;
		//this.transformer = new ChangeDescription("", false);
		this.setNumberOfAttributes(numberOfAttributes);
		stringRepresentation = new String[this.getNumberOfAttributes()];
		for(int i = 0; this.getNumberOfAttributes()>i ;i++)
		{
			stringRepresentation[i] ="";
		}
		this.newTemp = false;
		toTrue = false;
	}
	
/*	public void setChangeVariable(String changes)
	{
		this.transformer.setVariableName(changes);
	}
	
	public void setChangeMode(boolean changes)
	{
		this.transformer.setSwitchMode(changes);
	}*/
	
	
	public int getChangeIndex()
	{
		int ind = -1;
		for(int i = 0; this.stringRepresentation.length>i;i++)
		{
			if(this.stringRepresentation[i].equals("C")||this.stringRepresentation[i].equals("c"))
			{
				ind = i;
				break;
			}
		}
		return ind;
	}
	
	public String toNameString()
	{
		return "a"+nameString;
	}
	
	public String toString()
	{
		String returnString = "@action a"+nameString;
		if(this.getPreStatement()!=null)
		{
			returnString = returnString + "\n\t@pre ("+this.getPreStatement()+")";
		}
		if(this.getPostStatement()!=null)
		{
			returnString = returnString + "\n\t@post {"+this.getPostStatement()+";}";
		}
		//returnString  = returnString + "trans: "+this.isToTrue();
		
		return returnString;
	}
	
	

	public void setPreStatement(String preStatement) {
		this.preStatement = preStatement;
	}

	public String getPreStatement() {
		return preStatement;
	}

	public void setPostStatement(String postStatement) {
		this.postStatement = postStatement;
	}

	public String getPostStatement() {
		return postStatement;
	}

	



	public void setNameString(String nameString) {
		this.nameString = nameString;
	}




	public String getNameString() {
		return nameString;
	}

	/*public void setTransformer(ChangeDescription transformer) {
		this.transformer = transformer;
	}

	public ChangeDescription getTransformer() {
		return transformer;
	}*/
	
	public boolean equals(Object obj)
	{
		if(obj instanceof Action)
		{
			Action compareTo = (Action)obj;
			
			if(this.getPreStatement().equals(compareTo.getPreStatement()) && this.getPostStatement().equals(compareTo.getPostStatement()) && this.getNameString().equals(compareTo.getNameString()) && this.getStringRepresentation().equals(compareTo.getStringRepresentation()) && this.getNumberOfAttributes()==compareTo.getNumberOfAttributes() && this.isNewTemp()==compareTo.isNewTemp() && this.isToTrue()==compareTo.isToTrue())
			{
				return true;
			}
		}
		return false;
	}
	
	public String[] getStringRepresentation()
	{
		return this.stringRepresentation;
	}

	public void setStringRepresentation(String[] stringRepresentation) {
		this.stringRepresentation = stringRepresentation;
	}
	
	
	public int getNumberOfAttributes() 
	{
		return numberOfAttributes;
	}

	public void setNumberOfAttributes(int numberOfAttributes) 
	{
		this.numberOfAttributes = numberOfAttributes;
	}
	
	public void printStringRepresentation()
	{
		for( int i = 0; this.stringRepresentation.length>i;i++)
		{
			System.out.print(this.stringRepresentation[i]);
		}
		
	}

	public void setNewTemp(boolean newTemp) {
		this.newTemp = newTemp;
	}

	public boolean isNewTemp() {
		return newTemp;
	}
	
	public Object clone()
	{
		Action myClone = new Action(this.nameString);
		myClone.setPreStatement(this.getPreStatement());
		myClone.setPostStatement(this.getPostStatement());
		myClone.setStringRepresentation(this.getStringRepresentation());
		myClone.setNumberOfAttributes(this.getNumberOfAttributes());
		myClone.setNewTemp(this.isNewTemp());
		myClone.setToTrue(this.isToTrue());
		return myClone;
	}

	
	public boolean greaterThan(Action compareTo)
	{
		
		
		return false;
	}

	public void setToTrue(boolean toTrue) {
		this.toTrue = toTrue;
	}

	public boolean isToTrue() {
		return toTrue;
	}
}
