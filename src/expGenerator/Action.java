package expGenerator;

/** class describing an action. 
 * I do not consider it as a plan as it does not have a body.
 * Should we also have a decision tree for the actions?
 * Unlike a plan, an action has a postcondition 
 * that describes the change of the world created by the action.
 * 
 * @author stephane
 *
 */
public class Action extends GoalPlan {
	
	/** postcondition of the action */
	String post;
	/** true precondition of the action, unknown by the agent */
	String pre;
	/**
	 * get the postcondition of the action, i.e. a java statement that describes
	 * a change in the environment
	 * @return the string expressing the condition*/
	public String getPost() {
		return post;
	}
	/** 
	 * String representing the postcondition of the action, i.e. its action 
	 * on the world.
	 * @param post a string containing a java expression that changes the
	 * state of the world
	 */
	public void setPost(String post) {
		this.post = post;
	}

	/**
	 * gets the true precondition of the action, i.e. a java predicate
	 * @return a string containing a java predicate
	 */
	public String getPre() {
		return pre;
	}
	/**
	 * sets the precondition of the action, i.e. a java predicate
	 * @param pre a string containing a java predicate
	 */
	public void setPre(String pre) {
		this.pre = pre;
	}

	/** Default constructor */
	public Action(String name) {
		super(name);
	}
	
	/** method to display info about the action, i.e.
	 * <li> id
	 * <li> true precondition
	 * <li> postcondition
	 */
	public String toString(){
		return  "Action \n\t id:\t\t"+id
			+"\t\n\t pre:\t\t"+pre 
			+"\t\n\t post:\t\t"+post;
	}
	
	/**
	 * method to test whether two actions are the same, here, it only 
	 * compares id
	 * @param a an action
	 * @return whether both actions have the same id
	 */
	public boolean equals(Action a){
		//System.out.println("this :"+id + "?=?"+g.id);
		return (this.id.equals(a.id) );
	}
	/**
	 * method to test equality between an object and an action, simply checks
	 * whether the object is an instance of action, and if so, calls the method
	 * to compare two actions.
	 * @param an object
	 * @return a boolean telling whether the object is equal to the action.
	 */
	public boolean equals(Object arg0) {
		if (arg0 instanceof Action)
			return (this.equals((Action) arg0));
		else
			return false;
	}

}
