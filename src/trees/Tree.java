package trees;


public class Tree{

    private Node root;

    public Tree(){}

    public Tree(Node node){
	root = node;
    }

    public void setRoot(Node node){
	root = node;
    }

    public void setRoot(Object item, Logger logger){
	root = new Node(item, logger);
    }

    public Node getRoot(){return root;}
  
    public String toString(){
	return root.toString(0);
    }
}