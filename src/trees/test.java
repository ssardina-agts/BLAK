package trees;
import java.util.Vector;
public class test{

    public static void main(String[] args){
	new test();
    }

    public test(){
	generateTree();
    }

public void generateTree(){
    Node[] planNodes = new Node[21];
planNodes[0] = new Node(new Integer(0));
planNodes[1] = new Node(new Integer(1));
planNodes[2] = new Node(new Integer(2));
planNodes[3] = new Node(new Integer(3));
planNodes[4] = new Node(new Integer(4));
planNodes[5] = new Node(new Integer(5));
planNodes[6] = new Node(new Integer(6));
planNodes[7] = new Node(new Integer(7));
planNodes[8] = new Node(new Integer(8));
planNodes[9] = new Node(new Integer(9));
planNodes[10] = new Node(new Integer(10));
planNodes[11] = new Node(new Integer(11));
planNodes[12] = new Node(new Integer(12));
planNodes[13] = new Node(new Integer(13));
planNodes[14] = new Node(new Integer(14));
planNodes[15] = new Node(new Integer(15));
planNodes[16] = new Node(new Integer(16));
planNodes[17] = new Node(new Integer(17));
planNodes[18] = new Node(new Integer(18));
planNodes[19] = new Node(new Integer(19));
planNodes[20] = new Node(new Integer(20));
Node[] goalNodes = new Node[10];
goalNodes[0] = new Node("InitialGoal");
Tree gpTree = new Tree(goalNodes[0]);
goalNodes[1] = new Node("G11");
goalNodes[2] = new Node("G12");
goalNodes[3] = new Node("G13");
goalNodes[4] = new Node("G21");
goalNodes[5] = new Node("G22");
goalNodes[6] = new Node("G23");
goalNodes[7] = new Node("G31");
goalNodes[8] = new Node("G32");
goalNodes[9] = new Node("G33");
goalNodes[0].addChild(planNodes[0]);
goalNodes[0].addChild(planNodes[1]);
goalNodes[0].addChild(planNodes[2]);
goalNodes[1].addChild(planNodes[3]);
goalNodes[1].addChild(planNodes[4]);
goalNodes[2].addChild(planNodes[5]);
goalNodes[2].addChild(planNodes[6]);
goalNodes[3].addChild(planNodes[7]);
goalNodes[3].addChild(planNodes[8]);
goalNodes[4].addChild(planNodes[9]);
goalNodes[4].addChild(planNodes[10]);
goalNodes[5].addChild(planNodes[11]);
goalNodes[5].addChild(planNodes[12]);
goalNodes[6].addChild(planNodes[13]);
goalNodes[6].addChild(planNodes[14]);
goalNodes[7].addChild(planNodes[15]);
goalNodes[7].addChild(planNodes[16]);
goalNodes[8].addChild(planNodes[17]);
goalNodes[8].addChild(planNodes[18]);
goalNodes[9].addChild(planNodes[19]);
goalNodes[9].addChild(planNodes[20]);
planNodes[0].addChild(goalNodes[1]);
planNodes[0].addChild(goalNodes[2]);
planNodes[0].addChild(goalNodes[3]);
planNodes[1].addChild(goalNodes[4]);
planNodes[1].addChild(goalNodes[5]);
planNodes[1].addChild(goalNodes[6]);
planNodes[2].addChild(goalNodes[7]);
planNodes[2].addChild(goalNodes[8]);
planNodes[2].addChild(goalNodes[9]);


    System.out.println("tree built");
    System.out.println(gpTree);

    Node g13 = gpTree.getRoot().search("G13");
    System.out.println(g13);
    Node p17 = gpTree.getRoot().search(new Integer(17));
    System.out.println(p17);

    Vector bg13 = g13.getNodesBelow();
    for (Object n : bg13)
	System.out.println(n);
}
}