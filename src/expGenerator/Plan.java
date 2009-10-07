package expGenerator;
import java.util.Vector;

/**
 * Implement a plan, which can be composed of a sequence of actions and subgoals.
 *   If the plan contains only a single action, an Action should be used instead.
 * @author stephane
 *
 */
public class Plan extends GoalPlan{

	/** tells which goal is handled by this plan */
	Goal handles;
	/** vector that describes the sequence of actions or subgoal 	 */
	Vector<GoalPlan> body;
	/** true precondition, it is not used for the agent behavior, but can be
	 * used later for the reasoning about the correctness of the behavior  */
	String pre;
	/** initial precondition believed by the agent, used in addition to the 
	 * context condition learnt by the agent */
	String initPre;
	/** a boolean that tells whether this plan handles a top level goal. 
	 *  It is important in our implementation since, when the plan finishes 
	 *  (failure or success), the outcome must be recorded, we must reason 
	 *  about updating the decision, and a <b>new</b> instance must be
	 *  started*/
	boolean isTopLevelPlan;
    boolean isFailedThresholdHandler;
	
	/**
	 * tells whether the plan handles a top level goal. When it is the case, 
	 * @return a boolean
	 */
	public boolean isTopLevelPlan() {
		return isTopLevelPlan;
	}
	/** set the attribute telling whether the plan handles the top-level goal
	 * 
	 * @param isTopLevelPlan boolean that tells whether the plan handles a top
	 * level goal.
	 */
	public void setTopLevelPlan(boolean isTopLevelPlan) {
		this.isTopLevelPlan = isTopLevelPlan;
	}
	/**
	 * get the initial precondition used by the agent (this precondition will
	 * be used in addition of the context learnt by the agent)
	 * @return the initial precondition used by the agent.
	 */
	public String getInitPre() {
		return initPre;
	}
	/**
	 * set the initial precondition used by the agent (this precondition will
	 * be used in addition of the context learnt by the agent)
	 * @param initPre the initial precondition used by the agent.
	 */
	public void setInitPre(String initPre) {
		this.initPre = initPre;
	}
	/**
	 * get the true precondition of the plan (not used by the agent, but
	 * is present here to allow possible reasoning about correctness of the 
	 * agent). It is an optional attribute.
	 * @return the true precondition of the plan
	 */
	public String getPre() {
		return pre;
	}
	/**
	 * set the true precondition of the plan (not used by the agent, but
	 * is present here to allow possible reasoning about correctness of the 
	 * agent).  It is not required to fill it.
	 * @param pre the true precondition of the plan
	 */
	public void setPre(String pre) {
		this.pre = pre;
	}

	/**
	 * Constructor
	 * @param name name of the plan.
	 */
	public Plan(String name) {
		super(name);
		body = new Vector<GoalPlan>();
        isFailedThresholdHandler = false;
	}
	/**
	 * set the goal that is handled by this plan
	 * @param trig goal that triggers this plan
	 */
	public void setHandles(Goal trig){ handles = trig;}
	/**
	 * get the goal that is handled by this plan/trigger this plan
	 * @return the goal that triggers the plan
	 */
	public Goal getHandles(){return handles;}
	/**
	 * set if this plan is a handler for failed thresholds
	 * @param val boolean
	 */
	public void setIsFailedThresholdHandler(boolean val){isFailedThresholdHandler = val;}
	/**
	 * check if this plan is a handler for failed thresholds
	 * @return boolean
	 */
	public boolean isFailedThresholdHandler(){return isFailedThresholdHandler;}
	/**
	 * add an element to the body of the plan, the action or subgoal is added
	 * at the end
	 * @param e subgoal or action that is part of the body of the plan
	 */
	public void addElementBody(GoalPlan e){body.add(e);}
	/** 
	 * get the sequence of actions/subgoals that form the body of the plan
	 * @return a vector containing a sequence of actions/subgoals
	 */
	public Vector<GoalPlan> getBody(){return body;}
	/**
	 * method to print information about the plan:
	 * <li> id of the plan
	 * <li> goal that trigger/is handled by the plan
	 * <li> true precondition when specified
	 * <li> initial precondition believed by the agent
	 * <li> body of the plan
	 */
	public String toString(){
		String str =  "Plan \n\t id:\t\t"+id+"\n\t handles:\t"+handles;
		if (pre != null)
			str+="\t\n\t pre:\t\t"+pre;
		str+="\t\n\t initPre:\t" + initPre+"\n";
		if (body.isEmpty())
			return str;
		else{
			str+="\tbody of the plan:\n";
			for (GoalPlan step: body){
				if ( step instanceof Goal)
					str +="\t\t posts:\t"+ step +"\n";
				else if (step instanceof Action)
					str +="\t\t action:\t" + step +"\n";
				else{
					System.err.println("step in the body which is not a goal" +
							" or an action!");
					System.exit(9);
				}
			}				
			return str;
		}
	}
	
}
