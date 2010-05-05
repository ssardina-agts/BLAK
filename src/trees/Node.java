package trees;
import java.util.*;



public abstract class Node{

    /*-----------------------------------------------------------------------*/
    /* MARK: Data Members */
    /*-----------------------------------------------------------------------*/
    private String name;
    public Vector children;
    public Logger logger;
    
    protected Node parent;
    protected Hashtable pathsKnown;
    protected Hashtable pathStringsKnown;
    protected Hashtable paths;
    protected Hashtable pathStrings;
    
    
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Constructors */
    /*-----------------------------------------------------------------------*/
    
    public Node(String val, Logger logger)
    {
    	name = val;
        this.logger = logger;

        parent = null;
    	children = new Vector();
        pathsKnown = new Hashtable();
        pathStringsKnown = new Hashtable();
        paths = new Hashtable();
        pathStrings = new Hashtable();
    }

    /*-----------------------------------------------------------------------*/
    /* MARK: Access Members */
    /*-----------------------------------------------------------------------*/

    public String name() { 
        return name; 
    }
    public void setName(String val) { 
        name = val; 
    }
    
    public void addChild(Node node){
        node.setParent(this);
        children.add(node);        
    }
	public int numberOfChildren() {
		return this.children.size();
	}
	
    
    /*-----------------------------------------------------------------------*/
    /* MARK: Access Functions - Coverage related */
    /*-----------------------------------------------------------------------*/
    
    public long paths(int depth) {
        return paths.containsKey(depth) ? ((Long)(paths.get(depth))).longValue() : 0;
    }
    public void setPaths(int depth, long val) {
        paths.put(depth,val);
    }
    
    public boolean pathStringsKnown(int depth) {
        return pathStringsKnown.containsKey(depth);
    }
    public void setPathStringsKnown(int depth) {
        pathStringsKnown.put(depth,true);
    }
    
    public boolean pathsKnown(int depth) {
        return pathsKnown.containsKey(depth);
    }
    public void setPathsKnown(int depth) {
        pathsKnown.put(depth,true);
    }
    
    public Node parent() {
        return parent;
    }
    public void setParent(Node node) {
        parent = node;
    }

    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Tree hierarchy related */
    /*-----------------------------------------------------------------------*/
    
    public Node search(Object target){
        System.out.println("check " + target +"?=" + name); 
        if (name.equals(target)) {
            return this;
        } else {
            for (Object obj: children){
                Node node = ((Node) obj);
                Node res = (node.search(target));
                if (res != null)
                    return res;
            }
            return null;
        }
    }
    
    public Vector getNodesBelow(){
        Vector res = new Vector();
        for (Object obj: children){
            Node node = ((Node) obj);
            res.add(node);
            Vector subtree = node.getNodesBelow();
            for (Object n : subtree)
                res.add(n);	    
        }
        return res;
    }
    
    public Vector getPathTo(Node destination) {
    	if(this.children.contains(destination)) {
    		Vector thePath = new Vector();
    		thePath.add(children.elementAt(children.indexOf(destination)));
    		thePath.insertElementAt(this, 0);
    		return thePath;
    	} else {
    		for(int i = 0; this.children.size()>i;i++) {
    			Node thisNode = (Node)this.children.elementAt(i);
    			Vector thePath = thisNode.getPathTo(destination);
    			if(thePath!=null) {
    				thePath.insertElementAt(this, 0);
    				return thePath;
    			}
    		}
    	}
    	return null;
    }
    
    public Node getNode(Object item) {
    	for(int j = 0; this.children.size()>j;j++) {
    		Node thisNode = (Node)this.children.elementAt(j);
    		if(thisNode.name().equals(item)) {
    			return thisNode;
    		} else {
    			Node deeperNode = null;
    			deeperNode = thisNode.getNode(item);
    			if(deeperNode!=null) {
    				return deeperNode;
    			}
    		}
    	}
    	return null;
    }

    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Misc */
    /*-----------------------------------------------------------------------*/

    public boolean isLeaf() {
    	return children.isEmpty();
    }

    public String stringOfState(String[] state) {
        String s = "";
        for(int i = 0; state.length>i;i++) {
            if(state[i]==null) {
                return null;
            } else if(state[i].equals("true")) {
                s += "1";
            } else if(state[i].equals("false")) {
                s += "0";
            } else {
                s += state[i];
            }
        }
        return s;    
    }
    
    public String toString(){
        return this.toString(0);
    }
    
    public String toString(int pad){
        //System.out.println("printing "+item);
        String out="";
        for (int i=0;i<pad;i++)
            out+=" ";
        out += name+"\n";
        for (Object obj : children){
            out+=((Node) obj).toString(pad+5); 
        }
        return out;
    }

    /*-----------------------------------------------------------------------*/
    /* MARK: Member Functions - Abstract */
    /*-----------------------------------------------------------------------*/
    public abstract boolean isStable(String[] state);

    public int[] stability(String[] state) {
        /* Returns an array of two values.
         * The first is the number of stable plans.
         * The second is the number of total plans
         */
        int[] val = new int[2];
        return val;
    };
    
}