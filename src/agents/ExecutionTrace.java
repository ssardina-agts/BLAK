package agents;

import java.lang.String;
import java.util.Vector;
import trees.Node;

class TraceNode {
    public Node goal;
    public Node plan;
    public String[] state;
    
    public TraceNode(Node g, Node p, String[] s) {
        goal = g;
        plan = p;
        state = s;
    }
}

public class ExecutionTrace {
    /*-----------------------------------------------------------------------*/
    /* MARK: Data Members */
    /*-----------------------------------------------------------------------*/
	private Vector<TraceNode> trace;
    private boolean werePoppedTracesStable;
	
    /*-----------------------------------------------------------------------*/
    /* MARK: Constructors */
    /*-----------------------------------------------------------------------*/
	public ExecutionTrace() {
        trace = new Vector<TraceNode>();
        werePoppedTracesStable = true;
	}

    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Stack */
    /*-----------------------------------------------------------------------*/
    public void pushTrace(Node g, Node p, String[] s) {
        /* FIFO */
        TraceNode n = new TraceNode(g,p,s);
        trace.add(n);
    }
    public void popTrace(boolean updateStable) {
        /* FIFO - Pop the most recent instance from the trace */
        if (!trace.isEmpty()) {
            TraceNode n = trace.remove(trace.indexOf(trace.lastElement()));
            if (updateStable) {
                werePoppedTracesStable &= n.goal.isStable(n.state);
            }
        }
    }
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - BUL related */
    /*-----------------------------------------------------------------------*/
    
    public boolean werePoppedTracesStable() {
        return werePoppedTracesStable;
    }
    
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Misc */
    /*-----------------------------------------------------------------------*/

    public void reset() {
        trace.removeAllElements();
        werePoppedTracesStable = true;
    }
    
    public String toString() {
        String s = "";
        for (TraceNode n : trace) {
            s += n.goal.name() + ":" + n.plan.name() + " ";
        }
        return s;
    }
}