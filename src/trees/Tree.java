package trees;


public class Tree{

    private Node root;

    public Tree() {}
    
    public Tree(Node node){
        root = node;
    }

    public Node getRoot(){
        return root;
    }
    public void setRoot(Node node){
        root = node;
    }
    public void setRoot(String item, Logger logger){
        root = new Node(item, logger);
    }

    public String toString(){
        return root.toString(0);
    }
}