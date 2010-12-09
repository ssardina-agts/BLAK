import aos.jack.jak.plan.PlanInstanceInfo;

public class PlanIdInfo extends PlanInstanceInfo{

    public String[] state;
	public int plan_id;
	public double pSuccess;
	public double planConfidence;
	public double stateConfidence;
	public boolean isFailedThresholdHandler;

	public PlanIdInfo(String[] s, int id, double prob, double pconf, double sconf, boolean isFTH){
		super(5/*default precedence*/);
		state = s;
		plan_id = id;
		pSuccess = prob;
		planConfidence = pconf;
        stateConfidence = sconf;
		isFailedThresholdHandler = isFTH;
	}
}
