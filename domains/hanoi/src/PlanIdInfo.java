import aos.jack.jak.plan.PlanInstanceInfo;

public class PlanIdInfo extends PlanInstanceInfo{

    public String[] state;
	public int plan_id;
	public double pSuccess;
	public double confidence;
	public boolean isFailedThresholdHandler;

	public PlanIdInfo(String[] s, int id, double prob, double conf, boolean isFTH){
		super(5/*default precedence*/);
		state = s;
		plan_id = id;
		pSuccess = prob;
		confidence = conf;
		isFailedThresholdHandler = isFTH;
	}
}
