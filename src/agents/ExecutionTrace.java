package agents;

import java.lang.String;
import java.util.Vector;
import java.util.Iterator;
import trees.Node;

class TraceNode {
    public Node goal;
    public Node plan;
    public String[] state;
    public boolean shouldRecordFailure;
    
    public TraceNode(Node g, Node p, String[] s) {
        this(g,p,s,false);
    }
    
    public TraceNode(Node g, Node p, String[] s, boolean b) {
        goal = g;
        plan = p;
        state = s;
        shouldRecordFailure = b;
    }
}

public class ExecutionTrace {
    /*-----------------------------------------------------------------------*/
    /* MARK: Data Members */
    /*-----------------------------------------------------------------------*/
	private Vector<TraceNode> trace;
    private boolean werePoppedTracesStable;
    private int nTotal;
    private int nStable;
	
    /*-----------------------------------------------------------------------*/
    /* MARK: Constructors */
    /*-----------------------------------------------------------------------*/
	public ExecutionTrace() {
        trace = new Vector<TraceNode>();
        werePoppedTracesStable = true;
        nTotal = 0;
        nStable = 0;
	}

    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Stack */
    /*-----------------------------------------------------------------------*/
    public void pushTrace(Node g, Node p, String[] s) {
        /* FIFO */
        TraceNode n = new TraceNode(g,p,s);
        trace.add(n);
        /* Reset the popped trace counters each time you push */
        nTotal = 0;
        nStable = 0;
    }
    public void popTrace(boolean updateStable) {
        /* FIFO - Pop the most recent instance from the trace */
        if (!trace.isEmpty()) {
            TraceNode n = trace.remove(trace.indexOf(trace.lastElement()));
            if (updateStable) {
                werePoppedTracesStable &= n.goal.isStable(n.state);
                int[] stability = n.goal.stability(n.state);
                nStable += stability[0];
                nTotal += stability[1];
            }
        }
    }
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - BUL related */
    /*-----------------------------------------------------------------------*/
    
    public int[] poppedTraceStability() {
        int[] val = new int[2];
        val[0] = 1;
        val[1] = 1;
        if (nTotal > 0) {
            val[0] = nStable;
            val[1] = nTotal;
        }
        return val;
    }
    
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Misc */
    /*-----------------------------------------------------------------------*/

    /* Sets shouldRecordFailure flags for all nodes in the trace */
    public void setShouldRecordFailure(boolean val) {
        Iterator iterator = trace.iterator(); 
        while (iterator.hasNext()) { 
            TraceNode p = (TraceNode) iterator.next(); 
            p.shouldRecordFailure = val;
        }
    }

    /* Returns shouldRecordFailure flag for the last node in the trace */
    public boolean shouldRecordFailure() {
        boolean val = false;
        if (!trace.isEmpty()) {
            TraceNode p = (TraceNode)(trace.lastElement());
            val = p.shouldRecordFailure;
        }
        return val;
    }
    
    public boolean isEmpty() {
        return trace.isEmpty();
    }
    
    public void reset() {
        trace.removeAllElements();
        werePoppedTracesStable = true;
        nTotal = 0;
        nStable = 0;
    }
    
    public String toString() {
        String s = "";
        for (TraceNode n : trace) {
            s += n.goal.name() + ":" + n.plan.name() + " ";
        }
        return s;
    }
}
