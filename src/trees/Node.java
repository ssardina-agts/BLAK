package trees;
import java.util.*;



public class Node{

    public Object item;
    private String name;
    public Vector children;
    public boolean successful;
    public Logger logger;
    public boolean pathsKnown;
    public int paths;
    
    public Node(Object val, Logger logger)
    {
    	item = val;
    	children = new Vector();
    	successful = true;
        this.logger = logger;
        pathsKnown = false;
        paths = 0;
    }

    public boolean isSuccessful()
    {
    	return this.successful;
    	
    }
    
    public void setSuccessful(boolean setting)
    {
    	this.successful = setting;
    }
    
    public void resetSuccessfulChidren()
	{
		this.successful = false;
	}
    
    /**
     * Overwrite in subclass. Would make abstract but I believe this class is used sometimes.
     * @return
     */
    public boolean determineSuccessful()
    {
    	return false;
    	
    }
    
    public boolean isStable(String state)
    {
    	return false;
    }
    
    public boolean isStable(String[] state)
    {
    	return false;
    }
    
    public boolean isSuccessful(String[] state)
    {
    	return false;
    }
    
    public boolean isLeaf()
    {
    	return children.isEmpty();
    }

    public void addChild(Node node){
	children.add(node);
    }

    public Object getItem(){return item;}
    
    public Node search(Object target){
	System.out.println("check " + target +"?=" + this.item); 
	if (item.equals(target))
	    return this;
	else{
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

    public String toString(){
	return this.toString(0);
    }

    public String toString(int pad){
	//System.out.println("printing "+item);
	String out="";
	for (int i=0;i<pad;i++)
	    out+=" ";
	out += item.toString()+"\n";
	for (Object obj : children){
	    out+=((Node) obj).toString(pad+5); 
	}
	return out;
    }

    public Enumeration getStableEnumeration()
    {
    	return null;
    }
    
    public boolean hasChild(Node searchFor)
    {
    	if(this.children.contains(searchFor))
    	{	
    		return true;
    	}
    	else
    	{
    		for( int i = 0; this.children.size()>i;i++)
    		{
    			Node thisChild = (Node)this.children.elementAt(i);
    			boolean isThere = thisChild.hasChild(searchFor);
    			if(isThere)
    			{
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    public boolean hasChildOnly(Node searchFor)
    {
    	if(this.children.contains(searchFor))
    	{	
    		return true;
    	}
    	return false;
    }
    
    public Vector getPathTo(Node destination)
    {
    	if(this.children.contains(destination))
    	{
    		Vector thePath = new Vector();
    		thePath.add(children.elementAt(children.indexOf(destination)));
    		thePath.insertElementAt(this, 0);
    		return thePath;
    	}
    	else
    	{
    		for(int i = 0; this.children.size()>i;i++)
    		{
    			Node thisNode = (Node)this.children.elementAt(i);
    			Vector thePath = thisNode.getPathTo(destination);
    			if(thePath!=null)
    			{
    				thePath.insertElementAt(this, 0);
    				return thePath;
    			}
    		}
    	}
    	
    	return null;
    }

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int numberOfChildren()
	{
		return this.children.size();
	}
	
	public boolean equals(Object obj)
	{
		if(obj instanceof Node)
		{
			Node compareTo = (Node)obj;
			if(compareTo.item.equals(this.getItem()))
			{
				return true;
			}
		}
		return false;
	}
	
	public void record(boolean res){}
	
	public String stringOfLastState(){return null;}
	
    public String[] getLastState() 
    {
		return null;
	}
	
    public Node getNode(Object item)
    {
    	for(int j = 0; this.children.size()>j;j++)
    	{
    		Node thisNode = (Node)this.children.elementAt(j);
    		if(thisNode.getItem().equals(item))
    		{
    			return thisNode;
    		}
    		else
    		{
    			Node deeperNode = null;
    			deeperNode = thisNode.getNode(item);
    			if(deeperNode!=null)
    			{
    				return deeperNode;
    			}
    		}
    	}
    	return null;
    }
	
    public String stringOfState(String[] state)
    {
    	String returnMe = "";
    	
    	for(int i = 0; state.length>i;i++)
    	{
    		if(state[i]==null)
    		{
    			return null;
    		}
    		if(state[i].equals("true"))
    		{
    			returnMe = returnMe+"1";
    		}
    		else if(state[i].equals("false"))
    		{
    			returnMe = returnMe+"0";
    		}
    	}
    	return returnMe;	
    }
    
    public int getPaths() {
        return paths;
    }
}