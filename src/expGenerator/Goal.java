package expGenerator;
import java.util.Vector;


/**
 * Implements a goal. The important attribute is the vector containing the 
 * plans that can handle this goal.
 * @author stephane
 *
 */
public class Goal extends GoalPlan{
	
	/** is the set of plans that can handle the goal */
	public Vector<Plan> handlers;
    private String failureRecovery;
	
	/** Default constructor */
    public Goal(String name){
        super(name);
        handlers = new Vector<Plan>();
        failureRecovery = "never";
    }
    
    /** adds a plan in the set of plans that handle the goal
     * @param p a plan that handles the goal
     */
    public void addPlan(Plan p){
        if (!handlers.contains(p))
            handlers.add(p);
    }
	
    /**
     * equals method: the method checks whether the compared object is an 
     * instance of the Goal class and then calls the specific method 
     * comparing two goals
     * @param arg0 an Object
     */
    public boolean equals(Object arg0) {
        if (arg0 instanceof Goal)
            return (this.equals((Goal) arg0));
        else
            return false;
    }
    /**
     * Specific method to compare whether two goals are identical: the 
     * method compares the id of the two goals.
     * @param g compared goal
     * @return a boolean telling whether the goal are the same.
     */
    public boolean equals(Goal g){
        //System.out.println("this :"+id + "?=?"+g.id);
        return (this.id.equals(g.id) );
    }
    

    public void setFailureRecovery(String val) {
        failureRecovery = val;
    }
    
	public String failureRecovery() {
        return failureRecovery;
    }
}
