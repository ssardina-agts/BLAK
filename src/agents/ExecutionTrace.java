package agents;

import java.lang.String;
import java.util.Vector;
import java.util.Iterator;
import trees.Node;

class TraceNode {
    public Node goal;
    public Node plan;
    public Vector<Node> applicable;
    public String[] state;
    public boolean shouldRecordFailure;
    
    public TraceNode(Node g, Node p, Vector<Node> a, String[] s) {
        this(g,p,a,s,false);
    }
    
    public TraceNode(Node g, Node p, Vector<Node> a, String[] s, boolean b) {
        goal = g;
        plan = p;
        applicable = a;
        state = s;
        shouldRecordFailure = b;
    }
}

public class ExecutionTrace {
    /*-----------------------------------------------------------------------*/
    /* MARK: Data Members */
    /*-----------------------------------------------------------------------*/
	private Vector<TraceNode> trace;
    private int nTotal;
    private int nStable;
	
    /*-----------------------------------------------------------------------*/
    /* MARK: Constructors */
    /*-----------------------------------------------------------------------*/
	public ExecutionTrace() {
        trace = new Vector<TraceNode>();
        nTotal = 0;
        nStable = 0;
	}

    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Stack */
    /*-----------------------------------------------------------------------*/
    public void pushTrace(Node g, Node p, Vector<Node> a, String[] s) {
        /* FIFO */
        TraceNode n = new TraceNode(g,p,a,s);
        trace.add(n);
        /* Reset the popped trace counters each time you push */
        nTotal = 0;
        nStable = 0;
    }
    public void popTrace() {
        /* FIFO - Pop the most recent instance from the trace */
        if (!trace.isEmpty()) {
            TraceNode n = trace.remove(trace.indexOf(trace.lastElement()));
            /* Now update stability information for popped traces before discarding */
            if (n.applicable != null) {
                for (int i = 0; i < n.applicable.size(); i++) {
                    Node a = n.applicable.elementAt(i);
                    nStable += (a.isStable(n.state)) ? 1 : 0;
                    nTotal += 1;
                }
            }
        }
    }
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - BUL related */
    /*-----------------------------------------------------------------------*/
    public int[] poppedTraceStability() {
        int[] val = new int[2];
        val[0] = nStable;
        val[1] = nTotal;
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
