import aos.jack.jak.plan.PlanInstanceInfo;

public class PlanIdInfo extends PlanInstanceInfo{

	public int plan_id;
	public double pSuccess;
	public double confidence;
	public boolean isFailedThresholdHandler;

	public PlanIdInfo(int id, double prob, double conf, boolean isFTH){
		super(5/*default precedence*/);
		plan_id = id;
		pSuccess = prob;
		confidence = conf;
		isFailedThresholdHandler = isFTH;
	}
}
