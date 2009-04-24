package treeGenerator;

public class MainTreeGenerator 
{

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		if(args.length>1)
		{
			TreeGenerator treeGen;
			if(args[1].equals("b"))
			{
				System.out.println("Boolean Styled Tree Generation");
				treeGen = new BooleanTreeGenerator(args[0]);
			}
			else if(args[1].equals("s"))
			{
				System.out.println("Smart Styled Tree");
				treeGen = new SmartTreeGenerator(args[0]);
			}
			else
			{		
				System.out.println("Random Tree Generation");
				treeGen = new RandomTreeGenerator(args[0]);
			}
			treeGen.generateAlphabet();
			treeGen.buildTree();
			treeGen.placeActions();
			treeGen.writeTreeFile();
		
		}
		else
		{
			System.out.println("Usage: java treeGenerator.MainTreeGenerator filename typeswitch\nWhere b means boolean nodes/attributes, r is random breadth across nodes.");
		}
	}

}
