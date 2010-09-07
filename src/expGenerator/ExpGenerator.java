package expGenerator;
import java.util.BitSet;
import java.util.Scanner;
import java.util.Vector;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import agents.Experience;
import trees.Node;
import weka.core.Attribute;
import weka.core.FastVector;
//import trees.*;

/**
 * This class is the main class for generating the JACK source code, that later
 * can be compiled using the JACK compiler. 
 */
public class ExpGenerator {
	
	/** set of attributes */
	private FastVector atts;
	/** value used for the classification, here + and - */
	private FastVector classVal;
	/** value used to represent boolean values, 
	 * here the string true and false */
	private FastVector boolVal;
	/** vector containing the type of each attribute */
	private Vector<String> attsType;
	/** vector containing boolean telling whether the attributes are observable
	 * by the learning agent */
	private Vector<Boolean> observableAtts;
	/** set of goals of the agents */
	private Vector<Goal> goals;
	/** the top goal which seats at the top of the goal-plan tree */
	private Goal topGoal;
	/** the set of plans */
	private Vector<Plan> plans;
	/** the set of actions */
	private Vector<Action> actions;
	/** name of the text file containing the metaPlan, I chose to use a text 
	 * file as I assume I will need to change/try thing, and it will be easier 
	 * to change the text file rather that the "embedded" code.
	 */
	public static String metaPlanFileName = "metaPlanBody.txt";
	/** name of the text file containing the main method */
	public static String mainFileName = "mainBody.txt";
	/** name of the text file that contains additional method used by the 
	 * learning agent, which should allow faster prototyping.
	 */
	public static String refinerMethods = "refinerMeth.txt";
	/** name of the target directory */
	public String targetDir;
	/** name of the input tree file */
	public String inputTreeFile;
	
	
	/**
	 * the main methods reads options in the command line and then creates
	 * an instance of the class ExpGenerator initialized with corresponding
	 * values
	 * @param args command line arguments
	 * <li> -o target directory
	 * <li> -i input filename
	 */
	public static void main(String args[]){
		String output = ".";
		String input = "input";
		if (args.length >0){
			int index =0;
			while (index < args.length){
				String arg = args[index];
				index++;
				char option = arg.charAt(1);
				switch(option){
					case 'o': // target directory
						output = args[index];
						index++;
						break;
					case 'i':// input filename
						input = args[index];
						index++;
						break;
				}
			}
		}
		System.out.println("Generate a simulator in the directory " +output
						   +" from the goal-plan tree in file "+input);
		ExpGenerator exp = new ExpGenerator(input, output);
	}
	
	/**
	 * Constructor. The constructor will generate all the code in the following
	 * order
	 * <li> read the input file containing the goal plan tree
	 * <li> generate the plans
	 * <li> generate the goals
	 * <li> generate the Environment, which is a JACK agent
	 * <li> generate the learning agent, which is JACK agent
	 * @param inputFileName name of the input file containing the goal plan tree
	 * @param outputDir name of the directory to write the output
	 */
	public ExpGenerator(String inputFileName, String outputDir) { 
		targetDir = outputDir;
        inputTreeFile = inputFileName;
		readInputFile(inputFileName);
		generatePlans();
		generateGoals();
		generateEnvironment();
		generateRefinerAgent();
        generateGoalPlanVisualisation();
	}
	
	/**
	 * method that reads the file containing the goal plan tree and collects 
	 * all the information needed to generate the JACK source code.
	 * @param filename name of the file containing a goal plan tree. The file 
	 * should adhere to the following format:
	 * <ol>
	 * <li> the set of attribute. One line per attribute. The 
	 * description is as follows:<br>
	 * <code>@attribute</code> attName boolean | { val1 val2 ... valk}
	 * <ul><li> attName is the name of the attribute
	 * <li> either it is boolean, then the keyword <code>boolean</code> is used
	 * <li> either it is an enumerated type, and in that case, the values 
	 * are written between a pair of braces <code>{</code> and <code>}</code>, 
	 * separated by space.
	 * </ul>
	 * <li> the top level goal<br>
	 * <code>@topgoal</code> goalName
	 * <ul><li> where goalName is the name of the top level goal of the goal-plan
	 * tree</ul>
	 * <li> the set of other goals, one goal per line<br>
	 * <code>@goal</code> goalName
	 * <ul><li> where goalName is the name of the goal</ul> 
	 * <li> the set of actions. Each actions is described using three lines in 
	 * the input file:
	 * <code>@action</code> actionName<br>
	 * <code>@pre</code> condition <br>
	 * <code>@post</code> statement <br>
	 * <ul>
	 * <li> actionName is the name of the action
	 * <li> condition is a boolean condition expressed in java that describes
	 * the precondition of the action, i.e. the condition for the action to 
	 * succeed (note that here, we consider that the action succeeds or fails, 
	 * we do not consider that the action can have multiple outcome)
	 * <li> statement is a java piece of code that describes how the world 
	 * changes when the action succeeds (note that we do not represent what
	 * the action does when it fails)
	 * </ul>
	 * <li> the set of plans, each plan is described by at least three lines, 
	 * optionaly, a fourth line when the true precondition of the plan is 
	 * specified
	 * <code>@plan</code> planName <br>
	 * <code>@handles</code> goalName <br>
	 * <code>@pre</code> preconditionTrue (optional) <br>
	 * <code>@initPre</code> initialPrecondition <br>
	 *  where
	 *  <ul><li> planName is the name of the plan, which will be used as its id
	 *  <li> goalName is the name of the goal that triggers this plan
	 *  <li> preconditionTrue is the true precondition of the plan, which need not be
	 *  specified (not used in the code as of now)
	 *  <li> initialPrecondition is the initial belief of the agent about the
	 *  precondition of the plan. For now, we consider it will always been 
	 *  used in conjunction with the knowledge learnt by experience.
	 *  </ul></ol>
	 */
	
	public void readInputFile(String filename){	
		try{
			Scanner readerObj = new Scanner(new File(filename));
			// first read the variables that may appear in the context conditions
			classVal = new FastVector();
	        classVal.addElement("+");
	        classVal.addElement("-");
	        boolVal = new FastVector();
	        boolVal.addElement("T");
	        boolVal.addElement("F");
	        attsType = new Vector<String>();
	        observableAtts = new Vector<Boolean>();
	        //Then, read in the file the attributes, type and attribute values;
	        String val;
	        atts = new FastVector();
	        while ((val = readerObj.next()).equals("@attribute")){
	        	String attName = readerObj.next();
	        	if ((val = readerObj.next()).equals("observable")){
	        		observableAtts.add(true);
	        		val = readerObj.next();
	        	}
	        	else
	        		observableAtts.add(false);
	        	if (val.equals("{")){
	        		attsType.add("String");
	        		FastVector attVal = new FastVector();
	        		while(!(val= readerObj.next()).equals("}")){
	        			attVal.addElement(val);
	        		}
	        		atts.addElement(new Attribute(attName, attVal));
	        	}
	        	else{
	        		if (val.equals("boolean")){
	        			atts.addElement(new Attribute(attName, boolVal));
	        			attsType.add("boolean");
	        		}
	        		else{
	        			System.err.println("Unknown type");
	        			System.exit(9);
	        		}
	        	}
	        	System.out.println("read the attribute "+atts.lastElement());
	        }
	        //Read the Goals
	        if (!val.equals("@topgoal")){
    			System.err.println("Goal should come now!");
    			System.exit(9);
    		}
	        int indexGoal=0;
	        goals = new Vector<Goal>();
	        topGoal = new Goal(readerObj.next());
	        val = readerObj.next();
            if (val.equals("repost")) {
                topGoal.setFailureRecovery(val);
                val = readerObj.next();
            }
	        topGoal.index=indexGoal;
	        indexGoal++;
	        goals.add(topGoal);
        	System.out.println("read goal "+ topGoal);
	        
	        while (val.equals("@goal")){
                Goal g = new Goal(readerObj.next());
	        	val = readerObj.next();
                if (val.equals("repost")) {
                    g.setFailureRecovery(val);
                    val = readerObj.next();
                }
	        	goals.add(g);
	        	goals.lastElement().index=indexGoal;
	        	indexGoal++;
	        	System.out.println("read goal "+ goals.lastElement());
	        }
	        //Read the actions
	        int indexActions=0;
	        actions = new Vector<Action>();
	        if (!val.equals("@action")){
    			System.err.println("Actions should come now!");
    			System.exit(9);
    		}
	        while(val.equals("@action")){
	        	Action a = new Action(readerObj.next());
	        	a.index = indexActions;
	        	indexActions++;
	        	val = readerObj.next();
	        	while(readerObj.hasNext() && !val.equals("@action") && !val.equals("@plan")){
	        		if (val.equals("@pre"))
	        			a.setPre(readerObj.nextLine());
	        		else if (val.equals("@post"))
	        			a.setPost(readerObj.nextLine());
	        		else{
	        			System.out.println("an action should only have post and pre");
	        			System.exit(9);
	        		}
	        		val = readerObj.next();
	        	}
	        	System.out.println("added "+a);
	        	actions.add(a);
	        }
	        //Read the plans
	        int indexPlan=0;
	        plans = new Vector<Plan>();
	        if (!val.equals("@plan")){
    			System.err.println("Plans should come now!");
    			System.exit(9);
    		}
	        while(val.equals("@plan")){
	        	Plan p = new Plan(readerObj.next());
	        	p.index = indexPlan;
	        	indexPlan++;
	        	val = readerObj.next();
	        	while(readerObj.hasNext() && !val.equals("@plan")){
	        		if (val.equals("@pre"))
	        			p.setPre(readerObj.nextLine());
	        		if (val.equals("@initPre"))
	        			p.setInitPre(readerObj.nextLine());
	        		if (val.equals("@handles")){
	        			// search the event that this plan handles
	        			int index;
	        			Goal h = new Goal(readerObj.next());
	        			if((index=goals.indexOf(h))>-1){	        			
	        				p.setHandles(goals.get(index));
	        			}
	        			else{
	        				System.err.println("Goal "+ h +" handled by plan "+p.getId()+" not found!");
	        				System.exit(9);
	        			}
	        		}
	        		if (val.equals("@body")){
	        			// gets all the subgoals and actions and checks whether they are there
	        			Scanner body = new Scanner(readerObj.nextLine());
	        			while (body.hasNext()){
	        				int index;
	        				String name = body.next();
	        				Goal sg = new Goal(name);
	        				Action sa = new Action(name);
		        			if((index=goals.indexOf(sg))>-1){
		        				p.addElementBody(goals.get(index));
		        			}
		        			else if((index=actions.indexOf(sa))>-1){
		        				p.addElementBody(actions.get(index));
		        			}
		        			else{
		        				System.err.println("Goal "+ sg + " posted by plan "+p.getId()+" not found!");
		        				System.exit(9);
		        			}
	        			}
	        		}
	        		if (readerObj.hasNext())
	        			val = readerObj.next();
	        	}
	        	if (p.getHandles().equals(topGoal))
	        		p.setTopLevelPlan(true);
	        	else
	        		p.setTopLevelPlan(false);
	        	System.out.println("added "+p);
	        	plans.add(p);
	        }
            // Append the threshold fail plans (one per goal)
            for (Goal g : goals){
                Plan p = new Plan("Ptf_"+g.getId());
                p.index = indexPlan++;
                p.setHandles(g);
                if (g.equals(topGoal)) {
	        		p.setTopLevelPlan(true);
                } else {
	        		p.setTopLevelPlan(false);
                }
                p.setIsFailedThresholdHandler(true);
                plans.add(p);
	        	System.out.println("added "+p);
            }
		}
		catch(IOException e){
			System.err.println("Error opening the input file! \n"+e);
			System.exit(9);
		}
		// the plans have been generated, I go back and revisit the goals to 
		// determine the plans that can satisfy each goal.
		for (Goal g: goals){
			for (Plan p: plans){
				if (p.handles.equals(g))
					g.addPlan(p);
			}
		}
	}
	
	/**
	 * method that generates all JACK plan source.
	 * <ol> generates the plans contained in the goal-plan tree
	 * <ul><li> getInstanceInfo for the metaPlan to handle the decision of the plan
	 * <li> relevant always set to true
	 * <li> context always set to true
	 * <li> body the body encoded in the goal plan tree are used here
	 * <li> pass call the method in the learner to record the success.  
	 * If it is a top level plan, sends a message to the environment that a 
	 * new iteration can be started
	 * <li> fail same as pass method
	 * </ul>
	 * <li> Plan_UpdateBelief: generate the code for the plan handling the case
	 * where agents updates their belief
	 * <li> generate the MetaPlan
	 * <li> generate the plan to start a new iteration
	 * </ol>
	 */
	public void generatePlans(){
		int planIndex=0;
		// generate all the plans of the goal-plan tree
		for (Plan p : plans){
            if (p.isFailedThresholdHandler()) {
                generateThresholdFailPlan((p.getHandles()).getId(), p.getId(), planIndex, p.isTopLevelPlan()); 
            } else {
                //write the import
                String code = "package plans;\n"
                +"import agents.RefinerAgent;\n"
                +"import agents.Config.RunMode;\n"
                +"import events."+p.getHandles().getId()+";\n";
                for (GoalPlan g: p.body){
                    if (g instanceof Goal)
                        code += "import events."+g.getId()+";\n";
                }
                if (p.isTopLevelPlan())
                    code += "import events.ReadyForNextIteration;\n";
                //write the class
                code += "\n\npublic plan "+ p.getId() + " extends Plan {\n"
                + " \nprivate int plan_id ="+ planIndex +";\n\n"
                + " \npublic String name = \""+ p.getId()+"\";\n\n";
                code +=writeGetInstanceInfo();
                code +=writeJackDeclarations(p);
                code +=writeRelevant(p);
                code +=writeContext(p);
                code +=writeBody(p);
                code +=writePassFail(p);
                code+="}";
                try{	
                    PrintWriter writer = new PrintWriter(targetDir+"/plans/"+p.getId()+".plan");
                    writer.println(code);
                    writer.close();
                }
                catch(IOException e){
                    System.err.println("Error creating the JACK plan for "+p.getId()+"\n"+e);
                    System.exit(9);
                }
            }
			planIndex++;
		}
        
        
		// generate the plan for updating the state of the world
		/*
		 String code ="package plans;\n"
		 + "import agents.RefinerAgent;\n"
		 +"import events.Update_Event;\n\n"
		 +"public plan Plan_UpdateBelief extends Plan {\n"
		 +"\t#handles event Update_Event env;\n"
		 +"\t#uses interface RefinerAgent self;\n\n"
		 +"\tstatic boolean relevant(Update_Event env){\n"
		 +"\t\treturn true;\n"
		 +"\t}\n\n"
		 +"\tcontext(){\n"
		 +"\t\ttrue;\n"
		 +"\t}\n\n"
		 +"\t#reasoning method\n"
		 +"\tbody(){\n";
		 for (int i=0;i<atts.size();i++)
		 if (observableAtts.get(i))
		 code += "\t\tself."+((Attribute) atts.elementAt(i)).name()
		 + "= env."+((Attribute) atts.elementAt(i)).name()+";\n";
		 code+="\t}\n}\n";
		 try{	
		 PrintWriter writer = new PrintWriter(targetDir+"/plans/Plan_UpdateBelief.plan");
		 writer.println(code);
		 writer.close();
		 }
		 catch(IOException e){
		 System.err.println("Error creating the JACK plan for Plan_UpdateBelief\n"+e);
		 System.exit(9);
		 }
		 */
		// generate the metaplan plan
		generateMetaPlan();
		// class info plan
		String code = "package plans;\n"
		+ "import aos.jack.jak.plan.PlanInstanceInfo;\n"
		+ "\npublic class PlanIdInfo extends PlanInstanceInfo{\n\n"
		+"\tpublic int plan_id;\n"
		+"\tpublic double pSuccess;\n"
		+"\tpublic double coverage;\n"
		+"\tpublic double coverageWeight;\n"
		+"\tpublic double planConfidence;\n"
		+"\tpublic double stateConfidence;\n"
		+"\tpublic boolean isFailedThresholdHandler;\n\n"
		+"\tpublic PlanIdInfo(int id ,int pre, double prob){\n"
		+"\t\tthis(id ,pre, prob, 0.0, 1.0, 1.0, 1.0, false);\n"
        +"\t}\n\n"
		+"\tpublic PlanIdInfo(int id ,int pre, double prob, double cov, double weight, double pconf, double sconf, boolean isFTH){\n"
		+"\t\tsuper(pre);\n"
		+"\t\tplan_id = id;\n"
		+"\t\tpSuccess = prob;\n"
		+"\t\tcoverage = cov;\n"
		+"\t\tcoverageWeight = weight;\n"
		+"\t\tplanConfidence = pconf;\n"
		+"\t\tstateConfidence = sconf;\n"
		+"\t\tisFailedThresholdHandler = isFTH;\n"
		+"\t}\n}";
		try{	
			PrintWriter writer = new PrintWriter(targetDir+"/plans/PlanIdInfo.java");
			writer.println(code);
			writer.close();
		}
		catch(IOException e){
			System.err.println("Error creating the class PlanIdInfo\n"+e);
			System.exit(9);
		}
		// plan to start new iteration
		code = "package plans;\n"
		+"import agents.Environment;\n"
		+"import events.ReadyForNextIteration;\n"
		+"\n"
		+"public plan StartNewIteration extends Plan {\n"
		+"\t#handles event ReadyForNextIteration ready;\n"
		+"\t#uses interface Environment env;\n\n"
		+"static boolean relevant(ReadyForNextIteration ready){\n"
		+"\t return true;\n"
		+"}\n\n"
		+"context(){\n\ttrue;\n}\n\n"
		+"#reasoning method\nbody(){\n"
        +"\t/* Start new episode */\n"
        +"\tenv.runOneIteration(ready.outcome);\n"
        +"}\n\n}";
		try{	
			PrintWriter writer = new PrintWriter(targetDir+"/plans/StartNewIteration.plan");
			writer.println(code);
			writer.close();
		}
		catch(IOException e){
			System.err.println("Error creating the plan StartNewIteration\n"+e);
			System.exit(9);
		}   
		
	}
	
	/**
	 * Writes the PlanInstanceInfo method for the plan. This method is used by
	 * JACK when multiple plans are applicable and is used to determine which
	 * plan is applicable, in our case, it can be used to choose the plan
	 * probabilistically.
	 * @return a string that contains the java code for the method 
	 * getInstanceInfo
	 */
	public String writeGetInstanceInfo(){
		return	"public PlanInstanceInfo getInstanceInfo(){\n"
		+"\tag.setLastInstance(plan_id);\n"
		+"\tdouble coverage = ag.getCoverage(plan_id);\n"
		+"\tdouble[] ps = ag.getProbability(plan_id);\n"
		+"\tdouble planConfidence = ag.averageExperiencedStability(plan_id);\n"
		+"\tdouble stateConfidence = ag.stateConfidence(plan_id);\n"
		+"\treturn new PlanIdInfo(plan_id, 9, ps[0], coverage, ag.coverageWeight, planConfidence, stateConfidence, false);\n"
		+"}\n\n";
	}
	
	/**
	 * Writes the relevant method, and in our case, this method always returns
	 * true
	 * @param p the current plan for which the method will be added
	 * @return a string that contains the java code for the relevant JACK 
	 * method
	 */
	public String writeRelevant(Plan p){
		return "static boolean relevant("+ p.getHandles().getId() +" ev) { \n"
		+"\t return true;\n"
		+"}\n\n";
	}
	
	/**
	 * Writes the context condition of the plan. In our case, it is the 
	 * initial belief of the agent about the context condition. Note that the
	 * call to the decision tree, which in our BDI learning agent, also forms 
	 * the context condition, is performed in the method that writes the
	 * getInstanceInfo
	 * @param p current plan considered
	 * @return a string containing the java code to the context JACK method
	 */
	public String writeContext(Plan p){
		return "context(){ \n"
		+ "\t"+p.getInitPre()+"\n"
		+"}\n\n";
		
	}
	/**
	 * Writes the body of the plan as specified in the input file. The body 
	 * contains either subgoal, and in that case, the goal is posted, or it 
	 * contains an action, in that case, it is executed by calling a method
	 * from the Environment
	 * @param p the current plan considered
	 * @return a string containing the java code for the body JACK method 
	 */
	public String writeBody(Plan p){
		String meth = "#reasoning method \n body(){\n";
		int index=0;
		for (GoalPlan g: p.body){
			if (g instanceof Goal){
				meth +="\t@subtask(subgoal"+ index +".subgoal());\n";
				index++;
			}
			else if (g instanceof Action)
				meth += "\tag.env.simulate("+ g.index +");\n";
			else{
				System.err.println("error writing the body of the plan"+p);
				System.exit(9);
			}
			
		}
		meth +="}\n\n";
		return meth;
	}
	
	/**
	 * Write the code for the pass and fail method. In each case, the learning
	 * agent records the outcome of the plan execution. If the plan handles the
	 * top level goal, then the methods also send a message to the environment
	 * to ask to start a new iteration.
	 * @param p the current plan considered
	 * @return a string containing the java code for the JACK methods pass and
	 * fail
	 */
	public String writePassFail(Plan p){
		String code = "#reasoning method\n"
		+"pass(){\n"
		+"\tag.record(plan_id,true);\n";
		if (p.isTopLevelPlan())
            code+="\t@send(\"Environment\", ready.nextIt(true,ag.env.it));\n";
		code += "}\n\n"
		+ "#reasoning method\n"
		+ "fail(){\n"
		+ "\tag.record(plan_id,false);\n";
		if (p.isTopLevelPlan())
			code+="\tif (ag.run_mode != RunMode.FAILURE_RECOVERY || ag.triggerNewIteration()) {\n"
            +"\t\tag.setTriggerNewIteration(false);\n"
            + "\t\t@send(\"Environment\", ready.nextIt(false,ag.env.it));\n"
            + "}";
		code+= "}\n\n";
		return code;
	}
	
	/**
	 * Writes all the JACK declaration at the beginning of the plan of the 
	 * goal-plan tree, i.e. the event handled, the subgoal(s) that can be 
	 * posted by this plan, if the plan handles the top level goal, the plan
	 * can also posts a message to ask for a new iteration.
	 * @param p the current plan considered
	 * @return a string containing the JACK declarations within a plan from the
	 * goal plan tree.
	 */
	public String writeJackDeclarations(Plan p){
		int i=0;
		String code ="#handles event "+ p.getHandles() + " trigger;\n";
		for (GoalPlan g: p.body){
			if (g instanceof Goal){
				code += "#posts event " + g.getId() + " subgoal"+i+";\n";
				i++;
			}
		}
		if (p.isTopLevelPlan())
			code+="#sends event ReadyForNextIteration ready;\n";
		code += "#uses interface RefinerAgent ag;\n\n";
		return code;
	}
	

	public void generateGoals(){
		//generate the goals of the goal plan tree
		for (Goal g: goals){
			String code = "package events;\n";
			//write the class
			if (g == topGoal) {
				code += "public event "+ g.getId() + " extends BDIMessageEvent {\n"
				+ "\t#posted as\n"
				+ "\tstart(){}\n";
            } else {
				code += "public event "+ g.getId() + " extends BDIGoalEvent {\n"
				+ "\t#posted as\n"
				+"\tsubgoal(){}\n";
            }
            code += ""
            + "\t#set behavior PostPlanChoice always;\n"
            + "\t#set behavior ApplicableChoice random;\n"
            + "\t#set behavior Recover "+g.failureRecovery()+";\n"
            +"\n}";
			try{	
				PrintWriter writer = new PrintWriter(targetDir+"/events/"+g.getId()+".event");
				writer.println(code);
				writer.close();
			}
			catch(IOException e){
				System.err.println("Error creating the JACK plan for "+g.getId()+"\n"+e);
				System.exit(9);
			}
		}
		// generate the event to update the state of the environment
		/*
		 String code="package events;\n"
		 + "public event Update_Event extends MessageEvent {\n"
		 + "\tpublic String state;\n";
		 for (int i=0;i< atts.size(); i++){
		 if (observableAtts.get(i))
		 code += "\tpublic " + attsType.get(i) + " " 
		 + ((Attribute) atts.elementAt(i)).name() + ";\n";
		 }
		 code += "\n#posted as\n\t worldChanged(";
		 // count the number of observable attributes
		 int countObs=0, printed=0;
		 for (int i=0;i< atts.size()-1; i++)
		 if (observableAtts.get(i))
		 countObs++;
		 for (int i=0;i< atts.size()-1; i++){
		 if (observableAtts.get(i)){
		 if (printed < countObs)
		 code += attsType.get(i)+ " "
		 +((Attribute) atts.elementAt(i)).name()+i+", ";
		 else
		 code += attsType.get(i)+ " "
		 +((Attribute) atts.elementAt(i)).name()+i;
		 printed++;
		 }
		 }
		 if (observableAtts.get(atts.size()-1)){
		 code += attsType.get(atts.size()-1)+ " "
		 +((Attribute) atts.lastElement()).name()+(atts.size()-1);
		 }
		 code += "){\n\t\tstate = ";
		 printed=0;
		 for (int i=0;i< atts.size()-1; i++){
		 if (observableAtts.get(i)){
		 if (printed < countObs)
		 code+="\""+((Attribute) atts.elementAt(i)).name()+":\"+"
		 +((Attribute) atts.elementAt(i)).name()+i+"+ ";
		 else
		 code+="\""+((Attribute) atts.elementAt(i)).name()+":\"+"
		 +((Attribute) atts.elementAt(i)).name()+i;
		 printed++;
		 }
		 }
		 if (observableAtts.get(atts.size()-1))
		 code+="\""+((Attribute) atts.elementAt(atts.size()-1)).name()+":\"+"
		 +((Attribute) atts.elementAt(atts.size()-1)).name()+(atts.size()-1);
		 code+=";\n";
		 for (int i=0;i< atts.size(); i++){
		 if (observableAtts.get(i))
		 code+= "\t\t"+((Attribute) atts.elementAt(i)).name()+"="
		 +((Attribute) atts.elementAt(i)).name()+i+";\n";
		 }
		 code +="\t}\n}";
		 try{	
		 PrintWriter writer = new PrintWriter(targetDir+"/events/Update_Event.event");
		 writer.println(code);
		 writer.close();
		 }
		 catch(IOException e){
		 System.err.println("Error creating the JACK plan for Update_Event\n"+e);
		 System.exit(9);
		 }
		 */
		// write ready for next iteration event
		String code= "package events;\n"
		+ "public event ReadyForNextIteration extends MessageEvent {\n"
		+"\tpublic boolean outcome;\n\n"
		+"\t#posted as\n"
		+"\tnextIt(boolean result, int iteration){\n"
		+"\t\toutcome=result;\n"
		+"\t}\n}";
		try{	
			PrintWriter writer = new PrintWriter(targetDir+"/events/ReadyForNextIteration.event");
			writer.println(code);
			writer.close();
		}
		catch(IOException e){
			System.err.println("Error creating the JACK plan for ReadyForNextIteration\n"+e);
			System.exit(9);
		}
	}
	
	
	
	/**
	 * Generates the MetaPlan method. The body of the metaplan is read from
	 * a file to allow easier prototyping
	 */
	public void generateMetaPlan(){
		String code = "package plans;\n"
		+ "import java.lang.Math;\n"
        + "import java.util.Enumeration;\n"
		+ "import agents.Config.PlanSelectMode;\n"
		+ "import agents.Config.RunMode;\n"
		+ "import agents.RefinerAgent;\n";
		for (Goal g: goals)
			code +="import events."+g.getId()+";\n";
		code += "\npublic plan MetaPlan extends Plan {\n";
		for (Goal g: goals)
			code +="\t #chooses for event "+g.getId()+";\n";
		code += "\t#handles event PlanChoice pc;\n"
		+"\t#uses interface RefinerAgent ag;\n\n";
		code +="\tstatic boolean relevant(PlanChoice pc){\n"
		+"\t\treturn true;\n"
		+"\t}\n\n";
		code+= "\tcontext(){\n"
		+"\t\ttrue;\n"
		+"\t}\n\n";
		code+= "\t#reasoning method\n"
		+"\tbody(){\n";
		try{
			Scanner reader = new Scanner(new InputStreamReader(getClass().getResourceAsStream(metaPlanFileName)));			
			while (reader.hasNextLine()){
				code += "\t"+reader.nextLine()+"\n";
			}
			reader.close();
			code +="\t}\n}";
			PrintWriter writer = new PrintWriter(targetDir+"/plans/MetaPlan.plan");
			writer.println(code);
			writer.close();
		}
		catch(IOException e){
			System.err.println("Error creating the JACK plan MetaPlan\n"+e);
			System.exit(9);
		}
		
	}
	
    
	/**
	 * Generates the ThresholdFail plan that is used when all other applicable
     * plans for the given goal lie below the threshold proabbility of success.
     */
	public void generateThresholdFailPlan(String gid, String pid, int planIndex, boolean isTopLevelPlan){
		String code = "package plans;\n"
		+ "import java.lang.Math;\n"
		+ "import agents.RefinerAgent;\n"
		+ "import agents.Config.RunMode;\n"
        + "import events."+gid+";\n";
		if (isTopLevelPlan) {
			code+="import events.ReadyForNextIteration;\n";
        }

		code += "\npublic plan "+pid+" extends Plan {\n"
        + " \nprivate int plan_id = "+ planIndex +";\n\n"
        + " \npublic String name = \""+pid+"\";\n\n";
        
        code +=	"\tpublic PlanInstanceInfo getInstanceInfo(){\n"
		+"\t\tag.setLastInstance(plan_id);\n"
		+"\t\treturn new PlanIdInfo(plan_id, 9, 0.0, 1.0, 1.0, 1.0, 1.0, true);\n"
		+"\t}\n\n";
        
		if (isTopLevelPlan) {
			code+="\t#sends event ReadyForNextIteration ready;\n";
        }
		code +="\t#handles event "+gid+" trigger;\n"
		+ "\t#uses interface RefinerAgent ag;\n\n";
		code +="\tstatic boolean relevant("+gid+" ev){\n"
		+ "\t\treturn true;\n"
		+ "\t}\n\n";
		code += "\tcontext(){\n"
		+ "\t\ttrue;\n"
		+ "\t}\n\n";
		code += "\t#reasoning method\n"
		+ "\tbody(){\n"
        + "\t\tfalse;\n"
        + "\t}\n\n";
		code += "\t#reasoning method\n"
		+ "\tpass(){\n"
		+ "\t\tag.record(plan_id,true);\n";
        if (isTopLevelPlan) {
            code+="\t\t\t@send(\"Environment\", ready.nextIt(true,ag.env.it));\n";
        }
		code += "\t}\n\n"
		+ "\t#reasoning method\n"
		+ "\tfail(){\n"
		+ "\t\tag.record(plan_id,false);\n";
		if (isTopLevelPlan) {
			code+="\t\tif (ag.run_mode != RunMode.FAILURE_RECOVERY || ag.triggerNewIteration()) {\n"
            +"\t\t\tag.setTriggerNewIteration(false);\n"
			+ "\t\t@send(\"Environment\", ready.nextIt(false,ag.env.it));\n"
            +"\t\t}";
        }
		code += "\t}\n\n"
		+ "}\n";
        try{
			PrintWriter writer = new PrintWriter(targetDir+"/plans/"+pid+".plan");
			writer.println(code);
			writer.close();
		}
		catch(IOException e){
			System.err.println("Error creating the JACK plan "+pid+"\n"+e);
			System.exit(9);
		}
	}

    
	/**
	 * Code that generates the environment agent.
	 */
	
	
	public void generateEnvironment(){
		String code = "package agents;\n";
		// write the imports
		code +="import events.ReadyForNextIteration;\n"
		+"import plans.StartNewIteration;\n"
		+"import agents.Config.UpdateMode;\n"
		+"import agents.Config.PlanSelectMode;\n"
		+"import agents.Config.RunMode;\n"
		//+"import events.Update_Event;\n"
		+"import events."+topGoal.getId()+";\n"
		+"import trees.Logger;\n"
		+"import java.io.File;\n"
		+"import java.io.IOException;\n"
		+"import java.io.PrintWriter;\n"
		+"import java.util.Random;\n"
		+"import java.io.FileOutputStream;\n"
		+"import java.io.BufferedReader;\n"
		+"import java.io.FileReader;\n"
		+"import java.util.*;\n"
		+"import java.util.StringTokenizer;\n";
		
		code+="/**\n"
		+"The environment is considered as an agent. The environment will post events and changes the value of variables\n"
		+"*/ \n";
		// code for the class
		code+="public agent Environment extends Agent implements Logger {\n"
		//+"#sends event Update_Event updateEvent;\n"
		+"#sends event "+ topGoal.getId() + " topGoal;\n"
		+"#handles event ReadyForNextIteration;\n"
		+"#uses plan StartNewIteration;\n\n";
		//constructor    
		code+="public Environment(String name,String outputDir, String filename){\n"
		+"\tsuper(name);\n"
		+"\ttargetDir = outputDir;\n"
		+"\tgenerator = new Random();\n"
		+"\tit =0;\n"
		+"\tSystem.out.println(\"The environment has started\");\n"
		+"\ttry{\n"
		+"\t\twriterOutcome = new PrintWriter(targetDir + \"/\" + filename + \"-episodes.csv\");\n"
		+"\t\twriterOutcomeTG = new PrintWriter(targetDir + \"/\" + filename + \"-topgoal.csv\");\n"
		+"\t\tlog = new PrintWriter(targetDir + \"/run.log\");\n"
		+"\t}catch(IOException e){\n"
		+"\t\tSystem.err.println(\"error opening the file for writing the outcome \\n\"+e);\n"
		+"\t\tSystem.exit(9);\n"
		+"\t}\n"
		+"}\n\n";
		//class attributes
		code+= "public static int numIterations = 1000;\n"
		+ "public int it,ticks=1,num_successes=0,num_failures=0;\n"
		+ "Random generator;\n"
		+ "public PrintWriter writerOutcome;\n"
		+ "public PrintWriter writerOutcomeTG;\n"
		+ "public PrintWriter log;\n"
		+ "private String logindent = \"\";\n"		
		+ "public String filenameOutcome = \"outcome.dat\";\n"
		+ "public double noise = 0.1;\n"
		+ "public static PlanSelectMode plan_selection = PlanSelectMode.PROBABILISTIC;\n"
		+ "public static UpdateMode update_mode = UpdateMode.CONCURRENT;\n"
		+ "public static RunMode run_mode = RunMode.DEFAULT;\n"
		+ "public RefinerAgent learningAgent;\n"
		+ "public String targetDir;\n"		
		+"// attributes describing the world\n";
		for (int i=0;i< atts.size(); i++){
			code += "public " + attsType.get(i) + " " 
			+ ((Attribute) atts.elementAt(i)).name() + ";\n";
		}
		//simulate method~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
		// first set the argument for notify if the action succeeds 
		
		String args_notify = "";
		// count the number of observable attributes
		int countObs=0, j=0;
		for (int i=0;i< atts.size(); i++)
			if (observableAtts.get(i))
				countObs++;
		for (int i=0;i< countObs-1; i++){
			while (!observableAtts.get(j))
				j++;
			args_notify += ((Attribute) atts.elementAt(j)).name()+", ";
			j++;
		}
		while (!observableAtts.get(j))
			j++;
		args_notify += ((Attribute) atts.elementAt(j)).name();
		
		
		// implementation of the method
		code+= "\npublic boolean simulate(int action_id){\n"
		+ "\tswitch(action_id){\n";
		int index=0;
		for (Action a: actions){
			code+="\t\tcase "+index +":\n";
			if (a.getPre()!=null)
				code+="\t\t\tif(!("+a.getPre()+"))\n"
				+"\t\t\t\treturn false;\n"
				+"\t\t\telse{\n"
				+ "\t\t\tif (generator.nextDouble() < noise)\n"
				+ "\t\t\t\treturn false;\n"
				+"\t\t\t\telse{\n"
				+"\t\t\t\t\t"+a.getPost()+"\n" 
				+"\t\t\t\t\tlearningAgent.notify("+args_notify + ");\n\t\t\t\t}\n"
				+"\t\t\t}\n\t\t\tbreak;\n";
			else
				code+="\t\t\tif (generator.nextDouble() < noise)\n"
				+ "\t\t\t\treturn false;\n"
				+"\t\t\telse{\n"
				+"\t\t\t\t"+a.getPost()+"\n"
				+"\t\t\t\tlearningAgent.notify("+args_notify + ");\n\t\t\t\t}\n"
				+"\t\tbreak;\n";
			index++;
		}
		code +="\t}\n"
		+"\treturn true;\n}\n\n";

		
		//run one iteration
		code+="public void runOneIteration(){\n"
        +"\tlearningAgent.resetNewEpisode();\n"
		+"\twriteLog(\"Iteration: \"+it+\"--------------------------\");\n"
		+"\tif (it < numIterations) {\n"
		+"\t\tit++;\n"
		+"\t\t// randomly select the value of the attributes \n";
		code+= "\t\tString[] attval;\n";
		for (int i=0;i< atts.size(); i++){
			if (attsType.get(i).equals("boolean"))
				code+= "\t\t" + ((Attribute) atts.elementAt(i)).name() 
				+"= generator.nextBoolean();\n";
			else{
				code+= "\t\tattval = new String["
				+((Attribute) atts.elementAt(i)).numValues()+"];\n";
				
				for (int k=0;k<((Attribute) atts.elementAt(i)).numValues()-1;k++)
					code+= "\t\tattval["+k+ "]=\""+((Attribute) atts.elementAt(i)).value(k)+"\";\n";
				code+= "\t\tattval["+(((Attribute) atts.elementAt(i)).numValues()-1)+ "]=\""
				+((Attribute) atts.elementAt(i)).value(((Attribute) atts.elementAt(i)).numValues()-1)
				+"\";\n";
				code += "\t\t"+((Attribute) atts.elementAt(i)).name() + "= "
				+"attval[generator.nextInt(attval.length)];\n";
			}
		}
		code+= "\t\tlearningAgent.notify("+args_notify + ");\n";
		code+="\t\tMessageEvent mess = topGoal.start();\n"
		+"\t\tsend(\"BDI-Learning Agent\", mess);\n"
		+"\t}\n"
		+"\telse{\n"
		+"\t\twriterOutcome.close();\n"
		+"\t\twriterOutcomeTG.close();\n"
		+"\t\tlog.close();\n"
		+"\t\tlearningAgent.printDT();\n"
		+"\t\tlearningAgent.finish();\n"
		+"\t\tSystem.exit(0);\n"
		+"\t}\n"
		+"}\n\n";
		
		
		// run one iteration initial
		code+="public void runOneIteration(boolean outcomePrev){\n"
		+ "\tif (outcomePrev)\n"
		+ "\t\tnum_successes++;\n"
		+ "\telse\n"
		+ "\t\tnum_failures++;\n"
		+ "\tif (it>0 && it%ticks==0){\n"
		+ "\t\twriterOutcome.println(it + \" \"+(num_successes*1.0/ticks)+ \" \"+(num_failures*1.0/ticks));\n"
		+ "\t\tnum_successes=0;\n"
		+ "\t\tnum_failures=0;\n"
		+ "\t\twriterOutcome.flush();\n"
		+ "\t}\n"
		+ "\trunOneIteration();\n"
		+ "}\n\n";	

		code+="public void writeOutcomeForTopGoal(int steps){\n"
		+ "\twriterOutcomeTG.println(steps);\n"
		+ "\twriterOutcomeTG.flush();\n"
		+ "}\n\n";	
		
		//code for the main method
		Scanner reader = new Scanner(new InputStreamReader(getClass().getResourceAsStream(mainFileName)));			
		while (reader.hasNextLine())
			code += reader.nextLine()+"\n";
		reader.close();
		code+="}";
		try{
			PrintWriter writer = new PrintWriter(targetDir+"/agents/Environment.agent");
			writer.println(code);
			writer.close();
		}
		catch(IOException e){
			System.err.println("Error creating the agent Environment\n"+e);
			System.exit(9);
		}
	}
	
	/**
	 * codes that generate the BDI-learning agent
	 */
	public void generateRefinerAgent(){
		System.out.println("Building Refiner Agent");
		String code = "package agents;\n";

        code += "import agents.Config.UpdateMode;\n"
             +  "import agents.Config.PlanSelectMode;\n"
             +  "import agents.Config.RunMode;\n";

		//java imports
		code += "import java.util.Hashtable;\n"
		+"import java.util.Random;\n"
		+"import trees.Node;\n"
		+"import java.util.*;\n"
		+"import java.io.FileOutputStream;\n"
		+"import java.io.PrintWriter;\n"
		+"import trees.Tree;\n";
		//import the events
		code += "import events.ReadyForNextIteration;\n";
		//			+"import events.Update_Event;\n";
		for (Goal g: goals)
			code += "import events."+g.getId()+";\n";
		//imports the plans
		for (Plan p: plans)
			code += "import plans."+p.getId()+";\n";
		code+= "import plans.MetaPlan;\n"
		+"import aos.jack.jak.event.PlanChoice;\n";
		
		//imports for weka
		code+="import weka.classifiers.trees.J48;\n"
		+ "import weka.core.Attribute;\n"
		+ "import weka.core.FastVector;\n"
		+ "import weka.core.Instance;\n"
		+ "import weka.core.Instances;\n"
		+ "import weka.core.converters.ArffSaver;\n";
		
		// start the class
		code += "\npublic agent RefinerAgent extends Agent {\n\n";
		// declaration of the events handled
		//code+= "\t#handles event Update_Event;\n";
		for (Goal g: goals)
			code +="\t#handles event "+ g.getId()+";\n";
		code += "\t#handles event PlanChoice;\n";
		// declaration of the events posted
		code+=	"\t#posts event ReadyForNextIteration ready;\n";
		// declaration of the plan used
		//code += "\t#uses plan Plan_UpdateBelief;\n";
		for (Plan p : plans)
			code += "\t#uses plan "+p.getId()+";\n";
		code+="\t#uses plan MetaPlan;\n";
		//Constructor
		code +="\n\npublic RefinerAgent(String name){\n"
		+"\tsuper(name);\n"
		+"\tupdate_mode = UpdateMode.CONCURRENT;\n"
		+"\tselect_mode = PlanSelectMode.PROBABILISTIC;\n"
		+"\trun_mode = RunMode.DEFAULT;\n"
        +"\tstepsToAchieveTopGoal = 0;\n"
		+"\ttriggerNewIteration = false;\n"
		+"\tstableK = 3;\n"
		+"\tstableE = 0.05;\n"
		+"\tstableW = 1;\n"
		+"\tstateHistoryWindow = 0;\n"
		+"\tconfidenceAlpha = 0.95;\n"
        +"\tplanSelectThreshold = 0.0;\n"
        +"\tcoverageWeight = 1.0;\n"
        
        +"\tactiveExecutionTrace = new ExecutionTrace();\n"

		+"\tSystem.out.println(\"The BDI-learning agent has started!\");\n";
		int index=0; 
		
		code +="\ttry{\n";
		// 1. set up attributes
		code +="\t\tatts = new FastVector();\n"
		+ "\t\tclassVal = new FastVector();\n"
		+ "\t\tclassVal.addElement(\"+\");\n"
		+ "\t\tclassVal.addElement(\"-\");\n"
		+ "\t\tboolVal = new FastVector();\n"
		+ "\t\tboolVal.addElement(\"T\");\n"
		+ "\t\tboolVal.addElement(\"F\");\n";
		// create the attributes
		for (int i=0;i<atts.size();i++){
			if (observableAtts.get(i)){
				Attribute a = (Attribute) atts.elementAt(i);
				if (attsType.get(i).equals("boolean"))
					code += "\t\tatts.addElement(new Attribute(\""+a.name()+"\",boolVal));\n";
				else{
					code+="\t\tFastVector val"+ i + " = new FastVector();\n";
					for (int j=0;j<a.numValues();j++)
						code+= "\t\tval"+ i + ".addElement(\""+ a.value(j) +"\");\n";
					code+="\t\tatts.addElement(new Attribute(\""+a.name()+"\",val"+ i + "));\n";
				}
			}
		}
		code +="\t\tAttribute cl = new Attribute(\"outcome\",classVal);\n"
		+"\t\tatts.addElement(cl);\n\t}\n";
		code+="\tcatch(Exception e){\n"
		+"\t\tSystem.err.println(\"error with weka \\n\" + e);\n"
		+"\t\tSystem.exit(9);\n"
		+"\t}\n"
		+"\tgenerator = new Random();\n"
		+"\tplanNodes = new PlanNode[numPlans];\n"
		+"\t//generateTree();\n";
		
		code+="}\n\n";
		//class attributes
		code+="\tint numPlans="+plans.size()+";\n"
		+"\tpublic Environment env;\n";
		for (int i=0;i< atts.size(); i++){
			if (observableAtts.get(i))
				code += "\tpublic " + attsType.get(i) + " " 
				+ ((Attribute) atts.elementAt(i)).name() + ";\n";
		}
		int numObs =0;
		for (boolean obs : observableAtts)
			if (obs)
				numObs++;
		code+="\tint numAttributes = "+numObs+";\n"
		+"\tpublic PlanNode[] planNodes;\n"
		+"\tprivate FastVector atts;\n"
		+"\tprivate FastVector classVal;\n"
		+"\tprivate FastVector boolVal;\n"
		+"\tpublic Random generator;\n"
		+"\tpublic UpdateMode update_mode;\n"
		+"\tpublic PlanSelectMode select_mode;\n"
		+"\tpublic RunMode run_mode;\n"
        +"\tpublic int stepsToAchieveTopGoal;\n"
		+"\tprivate boolean triggerNewIteration;\n"
		+"\tpublic int stableK;\n"
		+"\tpublic double stableE;\n"
		+"\tpublic int stableW;\n"
		+"\tpublic int stateHistoryWindow;\n"
		+"\tpublic double confidenceAlpha;\n"
		+"\tint[] startToUseDT;\n"	
		+"\tpublic boolean probSelect = false;\n"
		+"\tTree gpTree;\n"
		+"\tpublic double planSelectThreshold;\n"
		+"\tpublic double coverageWeight;\n"
        
        +"\tpublic ExecutionTrace activeExecutionTrace; \n"

		/*
		+"\tpublic boolean isStableUpdates()\n"
		+"\t{\n"
		+"\t\treturn stableUpdates;\n"
		+"\t}\n\n"
		*/

		+"\tpublic void setUpdateMode(UpdateMode state)\n"
		+"\t{\n"
		+"\t\tthis.update_mode = state;\n"
		+"\t}\n\n"
		+"\tpublic void setSelectMode(PlanSelectMode state)\n"
		+"\t{\n"
		+"\t\tthis.select_mode = state;\n"
		+"\t}\n\n"
		+"\tpublic void setRunMode(RunMode mode)\n"
		+"\t{\n"
		+"\t\tthis.run_mode = mode;\n"
		+"\t}\n\n"
		+"\tpublic void setTriggerNewIteration(boolean val)\n"
		+"\t{\n"
		+"\t\tthis.triggerNewIteration = val;\n"
		+"\t}\n\n"
		+"\tpublic boolean triggerNewIteration()\n"
		+"\t{\n"
		+"\t\treturn this.triggerNewIteration;\n"
		+"\t}\n\n"
		+"\tpublic int getStableK()\n"
		+"\t{\n"
		+"\t\treturn this.stableK;\n"
		+"\t}\n\n"
		+"\tpublic void setStableK(int value)\n"
		+"\t{\n"
		+"\t\tthis.stableK = value;\n"
		+"\t}\n\n"
		+"\tpublic int getStableW()\n"
		+"\t{\n"
		+"\t\treturn this.stableW;\n"
		+"\t}\n\n"
		+"\tpublic void setStableW(int value)\n"
		+"\t{\n"
		+"\t\tthis.stableW = value;\n"
		+"\t}\n\n"
		+"\tpublic int getStateHistoryWindow()\n"
		+"\t{\n"
		+"\t\treturn this.stateHistoryWindow;\n"
		+"\t}\n\n"
		+"\tpublic void setStateHistoryWindow(int value)\n"
		+"\t{\n"
		+"\t\tthis.stateHistoryWindow = value;\n"
		+"\t}\n\n"
		+"\tpublic double getConfidenceAlpha()\n"
		+"\t{\n"
		+"\t\treturn this.confidenceAlpha;\n"
		+"\t}\n\n"
		+"\tpublic void setConfidenceAlpha(double value)\n"
		+"\t{\n"
		+"\t\tthis.confidenceAlpha = value;\n"
		+"\t}\n\n"
		+"\tpublic double getStableE()\n"
		+"\t{\n"
		+"\t\treturn this.stableW;\n"
		+"\t}\n\n"
		+"\tpublic void setStableE(double value)\n"
		+"\t{\n"
		+"\t\tthis.stableE = value;\n"
		+"\t}\n\n";
		
		code+="\tpublic int findPlanNodeByID(String planID)\n"
		+"\t{\n"
		+"\t\tfor(int j = 0; this.planNodes.length >j;j++)\n"
		+"\t\t{\n"
		+"\t\t\tif(this.planNodes[j].name().equals(planID))\n"
		+"\t\t\t{\n"
		+"\t\t\t\treturn j;\n"
		+"\t\t\t}\n"
		+"\t\t}\n"
		+"\t\treturn -1;\n"
		+"\t}\n\n\n";
		
		code +="\tpublic void resetLastStates()\n"
		+"\t{\n"
		+"\t\tfor(int i = 0; planNodes.length >i; i++)\n"
		+"\t\t{\n"
		+"\t\t\tplanNodes[i].resetLastState();\n"
		+"\t\t}\n"
		+"\t}\n\n\n";
		
		code +="\n\n\tpublic void clearAllLastStates()\n"
		+"\t{\n"
		+"\t\tfor(int i = 0; planNodes.length>i;i++)\n"
		+"\t\t{\n"
		+"\t\t\tplanNodes[i].resetLastState();\n"
		+"\t\t}\n"
		+"\t}\n\n";
		
		// set Environment
		code +="\npublic void setEnvironment(Environment e){env = e;}\n\n";
		
		
		// generate the goal-plan tree structure~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code += "public void generateTree(){\n";
		code += "System.out.println(\"Generating GP tree. k is: \"+stableK+\" and epsilion is: \"+stableE);\n";
		
		
		//~~~~  generate all the plan nodes
		code += "\tplanNodes = new PlanNode["+plans.size()+"];\n";
		for (Plan p : plans){
			code+="\tplanNodes["+p.index+"] = new PlanNode("
			+ "\"" + p.getId()+ "\", atts, "
			+ "update_mode, select_mode, run_mode, "
            + "stableE, stableK, stableW, stateHistoryWindow, "
            + (p.isFailedThresholdHandler()?"true":"false")
            + ", 2/*buildThreshold*/, 0/*experience window not used*/, (trees.Logger)env);\n";
		}	
		//~~~ generate all the goal nodes
		code += "\tNode[] goalNodes = new GoalNode["+goals.size()+"];\n";
		index = 0;
		for (Goal g: goals){
			code+="\tgoalNodes["+index+"] = new GoalNode(\""+ g.getId() +"\", (trees.Logger)env);\n";
			if (g.equals(topGoal))
				code+="\tgpTree = new Tree(goalNodes["+index+"]);\n";
			index++;
		}
		// make all the links
		index=0;
		for (Goal g: goals){
			for (Plan p : g.handlers) {
                if (index==0) {
				code += "\tplanNodes["+p.index +"].setTopGoal((GoalNode)goalNodes["+index+"]);\n";
                };
				code += "\tgoalNodes["+index+"].addChild(planNodes["+p.index +"]);\n";
            }
			index++;
		}
		for (Plan p: plans){
			for (GoalPlan g : p.body)
				if (g instanceof Goal)
					code += "\tplanNodes["+p.index+"].addChild(goalNodes["+g.index+"]);\n";
			
		}
        
        // set complexity
		for (Plan p : plans){
			code+="\tplanNodes["+p.index+"].setComplexity(planNodes["+p.index+"].getPaths());\n";
        }        
        
		code+="\n}\n\n";
        /*
		code+="\n\nprintPlanNodes();\n}\n\n";
		
		code +="\t\n\npublic void printPlanNodes()\n"
		+"{\n"
		+"\tint bottomPlans = 0;\n"
		+"\t\tfor(int i =0; planNodes.length>i;i++)\n"
		+"\t\t{\n"
		+"\t\t\tif(planNodes[i].numberOfChildren()==0)\n"
		+"\t\t\t\t{\n"
		+"\t\t\t\t\tbottomPlans++;\n"
		+"\t\t\t\t}\n"
		+"\t\t\tSystem.out.println(\"This tree has the following bottom level plans: \"+bottomPlans);\n"
		+"\t\t\twriteLog(\"This tree has the following bottom level plans: \"+bottomPlans, \"planBottom\");\n"
		+"\t\t}\n" 
		+"\t}\n\n\n";
		*/
        
		code +="\tpublic BitSet addBitSets(BitSet bs1, BitSet adder)\n"
		+"\t{\n"
		+"\t\tboolean carry = false;\n"
		+"\t\tBitSet stolenAdder = (BitSet)adder.clone();\n"
		+"\t\tfor(int i = atts.size()-2; i>-1;i--)\n"
		+"\t\t{\n"
		+"\t\t\tif(bs1.get(i)==true && stolenAdder.get(i)==true)\n"
		+"\t\t\t{\n"
		+"\t\t\t\tcarry = true;\n"
		+"\t\t\t}\n"
		+"\t\t\tif(carry)\n"
		+"\t\t\t{\n"
		+"\t\t\t\tif(0> (i-1))\n"
		+"\t\t\t\t{\n"
		+"\t\t\t\t\tbreak;\n"
		+"\t\t\t\t}\n"
		+"\t\t\t\telse\n"
		+"\t\t\t\t{\n"
		+"\t\t\t\t\tstolenAdder.set(i-1);\n"
		+"\t\t\t\t}\n"
		+"\t\t\t\tcarry = false;\n"
		+"\t\t\t}\n"
		+"\t\t}\n"
		+"\tbs1.xor(stolenAdder);\n"
		+"\treturn bs1;\n"
		+"\t}\n\n\n";
		
		code+="\n\tpublic void printBitSet(BitSet bs)\n"
		+"\t{\n"
		+"\t\tfor(int i= 0 ; atts.size()-1>i;i++)\n"
		+"\t\t{\n"
		+"\t\t\tif(bs.get(i)==true)\n"
		+"\t\t\t{\n"
		+"\t\t\t\tSystem.out.print(\"1\");\n"
		+"\t\t\t}\n"
		+"\t\t\telse if (bs.get(i)==false)\n"
		+"\t\t\t{\n"
		+"\t\t\t\tSystem.out.print(\"0\");\n"
		+"\t\t\t}\n"
		+"\t\t}\n"
		+"\t\tSystem.out.print(\"\\n\");\n"
		+"\t}\n\n\n";
		
		
		code +="\npublic int findPlanIndex(PlanNode thePlan)\n"
		+"{\n"
		+"\tfor (int i = 0; planNodes.length>i;i++)\n"
		+"\t{\n"
		+"\t\t if(planNodes[i].equals(thePlan))\n"
		+"\t\t{\n"
		+"\t\t\treturn i;\n"
		+"\t\t}\n"
		+"\t}\n"
		+"\treturn -1;\n"
		+"}\n\n\n";
		
		
		// record method ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		code+="public void record(int plan_id, boolean res)\n"
		+"{\n"
        +"\tif (planNodes[plan_id].isFailedThresholdHandler()) {\n"
        +"\t\tenv.writeLog(\"Refiner Agent: Skip recording for threshold handler plan \"+planNodes[plan_id].name()+\"\");\n"
        +"\t\treturn;\n"
        +"\t}\n"

		+"\tString strres=(res) ? \"(+)\" : \"(-)\";\n"
		+"\tenv.writeLog(\"Refiner Agent: Recording \"+strres+\" result in state \"+planNodes[plan_id].stringOfLastState()+\" for plan \"+planNodes[plan_id].name()+\" on iteration \"+env.it);\n"
        
        +"\tif (planNodes[plan_id].isLeaf()) {\n"
        +"\t\tstepsToAchieveTopGoal++;\n"
        +"\t\tenv.writeLog(\"Refiner Agent: Tried \"+stepsToAchieveTopGoal+\" actions so far to achieve top goal.\");\n"
        +"\t}\n"

        +"\tif (res && (planNodes[plan_id].topGoal() != null)) {\n"
        +"\t\tenv.writeLog(\"Refiner Agent: Total actions to achieve top goal was \"+stepsToAchieveTopGoal);\n"
        +"\t\tenv.writeOutcomeForTopGoal(stepsToAchieveTopGoal);\n"
        +"\t\tstepsToAchieveTopGoal = 0;\n"
        +"\t}\n"
		
        +"\t/* Now record the experience */\n"
        +"\tenv.indentRight();\n"
        +"\tint[] val = new int[2];\n"
        +"\tval[0] = 1;\n"
        +"\tval[1] = 1;\n"
        +"\tif (activeExecutionTrace.shouldRecordFailure()) {\n"
        +"\t\tres = false;\n"
        +"\t\tenv.writeLog(\"Refiner Agent: Forced result to (-) for Plan \"+planNodes[plan_id].name()+\" since a subgoal initially failed and was reposted.\" );\n"
        +"\t}\n"
		+"\tplanNodes[plan_id].record(res, res ? val : activeExecutionTrace.poppedTraceStability());\n"
        +"\tenv.indentLeft();\n"
        
        +"\t/* Finished recording so pop this plan off the active execution trace */\n"
        +"\tactiveExecutionTrace.popTrace(res==false/*indicate if stability info should be updated*/);\n"
        +"\t\tenv.writeLog(\"Refiner Agent: Removed Plan \"+planNodes[plan_id].name()+\" from active execution trace: \"+activeExecutionTrace);\n"
		+"}\n";
		
		
		// resetNewEpisode ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code+= "public void resetNewEpisode() {\n"
		+"\tif(true || update_mode == UpdateMode.STABLE) {\n"
		+"\tresetLastStates();\n"
		+"\tactiveExecutionTrace.reset();\n"
		+"\t}\n"
		+"}\n";

		code+= "\tpublic String findPlanNode(Object item)\n"
		+"\t{\n"
		+"\t\tString returnString  = \"-------------------------------------\\nLocal: \";\n"
		+"\t\tfor(int i = 0; planNodes.length>i;i++)\n"
		+"\t\t{\n"
		+"\t\t\tif(planNodes[i].name().equals(item))\n"
		+"\t\t\t{\n"
		+"\t\t\t\treturnString = returnString + planNodes[i];\n"
		+"\t\t\t}\n"
		+"\t\t}\n"
		+"\t\tNode secondNode = this.gpTree.getRoot().getNode(item);\n"
		+"\t\tif(secondNode!=null)\n"
		+"\t\t{\n"
		+"\t\t\treturnString = returnString + \", second node: \"+secondNode;\n"
		+"\t\t}\n"
		+"\t\telse\n"
		+"\t\t{\n"
		+"\t\t\tSystem.out.println(\"Node search failed\");\n"
		+"\t\t\tSystem.exit(0);\n"
		+"\t\t}\n"
		+"\t\t returnString = returnString +\"-------------------------------------\"\n;"
		+"\t\treturn returnString;\n"
		+"\t}\n\n\n";
		
		
		
		// set last instance method ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code += "public void setLastInstance(int plan_id){\n"
		+"\tString[] state = new String[numAttributes];\n";
		index =0;
		for (int i=0;i<atts.size();i++){
			if (observableAtts.get(i)){
				if (attsType.get(i).equals("boolean"))
					code += "\tstate[" + index +"]=Boolean.toString("
					+ ((Attribute) atts.elementAt(i)).name() + ");\n";
				else
					code += "\tstate[" + index +"]="
					+((Attribute) atts.elementAt(i)).name() + ";\n";
				index++;
			}
		}
		code += "\tplanNodes[plan_id].setLastInstance(state);\n"
		+"}\n\n";

        // get last state method ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code += "public String[] state(){\n"
		+"\tString[] state = new String[numAttributes];\n";
		index =0;
		for (int i=0;i<atts.size();i++){
			if (observableAtts.get(i)){
				if (attsType.get(i).equals("boolean"))
					code += "\tstate[" + index +"]=Boolean.toString("
					+ ((Attribute) atts.elementAt(i)).name() + ");\n";
				else
					code += "\tstate[" + index +"]="
					+((Attribute) atts.elementAt(i)).name() + ";\n";
				index++;
			}
		}
		code += "\treturn state;\n"
		+"}\n\n";
        
		
		// printDT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code += "public void printDT(){\n"
		+"\tfor (int p=0;p<numPlans;p++){\n"
		+"\t\tplanNodes[p].printDT();\n"
		+"\t}\n"
		+"}\n\n";
		
		// getProbability~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code+="public double[] getProbability(int plan_id){\n"
		+"\treturn planNodes[plan_id].getProbability();\n"
		+"}\n\n";
		
		// getCoverage~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code+="public double getCoverage(int plan_id){\n"
		+"\treturn (double)(planNodes[plan_id].getCoverage(planNodes[plan_id].lastState()))/planNodes[plan_id].getPaths();\n"
		+"}\n\n";

        // averageExperiencedStability~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code+="public double averageExperiencedStability(int plan_id){\n"
        +"\tPlanNode thisNode = planNodes[plan_id];\n"
		+"\treturn (thisNode.isLeaf() ? (thisNode.isStable()?1.0:0.0) : thisNode.averageExperiencedStability());\n"
		+"}\n\n";

        // stateConfidence~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code+="public double stateConfidence(int plan_id){\n"
        +"\tPlanNode thisNode = planNodes[plan_id];\n"
		+"\treturn thisNode.stateExperienceConfidence();\n"
		+"}\n\n";
        
		// notify method  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		code += "public void notify(";
		// count the number of observable attributes
		int j=0;
		for (int i=0;i< numObs-1; i++){
			while (!observableAtts.get(j))
				j++;
			code += attsType.get(j)+ " "
			+((Attribute) atts.elementAt(j)).name()+"_val, ";
			j++;
		}
		while (!observableAtts.get(j))
			j++;
		code += attsType.get(j)+ " "
		+((Attribute) atts.elementAt(j)).name()+"_val ";
		code += "){\n";
		for (int i=0;i< atts.size(); i++){
			if (observableAtts.get(i))
				code+= "\t\t"+((Attribute) atts.elementAt(i)).name()+"="
				+((Attribute) atts.elementAt(i)).name()+"_val;\n";
		}
		code +="\t}\n\n";
		
		code +="\n}";
		try{
			PrintWriter writer = new PrintWriter(targetDir+"/agents/RefinerAgent.agent");
			writer.println(code);
			writer.close();
		}
		catch(IOException e){
			System.err.println("Error creating the agent RefinerAgent\n"+e);
			System.exit(9);
		}	
	}

    public void generateGoalPlanVisualisation() {
        int index = 0;
        String str = "";

        str += "// Auto-generated Graphviz Goal/Plan Visualisation\n";
        str += "// Input file:"+inputTreeFile+"\n\n";
        str += "digraph goalplan {\n\n";
        str += "\t// Graph attributes\n";
        str += "\torientation = landscape\n";
        str += "\tsize=\"8.5,11\"\n";
        str += "\tcenter = \"true\"\n";
        
        // Generate all plan nodes
        str += "\n\t// Plan nodes\n";
		for (Plan p : plans) {
            str += "\t" + p.getId() + " [label=\"" + p.getId() + "\",shape=box]\n";
        }
        // Generate all goal nodes
        str += "\n\t// Goal nodes\n";
        for (Goal g: goals) {
            str += "\t" + g.getId() + " [label=\"" + g.getId() + "\",shape=ellipse]\n";
        }
        // Generate all action nodes
        str += "\n\t// Action nodes\n";
        index = 0;
		for (Plan p: plans){
			for (GoalPlan g : p.body) {
				if (g instanceof Action) {
                    str += "\ta" + index + " [label=\"" + g.getId() + "\",style=\"rounded\",shape=box]\n";
                    index++;
                }
            }
		}
        // Create all the links
        str += "\n\t// Goal -> Plan edges\n";
		for (Goal g: goals){
			for (Plan p : g.handlers) {
                str += "\t" + g.getId() + " -> " + p.getId() + "\n";
            }
		}
        str += "\n\t// Plan -> Goal edges\n";
		for (Plan p: plans){
			for (GoalPlan g : p.body) {
				if (g instanceof Goal) {
					str += "\t" + p.getId() + " -> " + g.getId() + "\n";
                }
            }
		}
        str += "\n\t// Plan -> Action edges\n";
        index = 0;
		for (Plan p: plans){
			for (GoalPlan g : p.body) {
				if (g instanceof Action) {
					str += "\t" + p.getId() + " -> a" + index + "\n";
                    index++;
                }
            }
		}
        str += "\n}\n";
        
        File outFileName = new File(targetDir+"/graphviz/gptree.dot");
		try{
            (new File(outFileName.getParent())).mkdirs();
			PrintWriter writer = new PrintWriter(outFileName.toString());
			writer.println(str);
			writer.close();
		}
		catch(IOException e){
			System.err.println("Error writing file: " + outFileName.toString());
			System.exit(9);
		}	
        
    }
	
} 
