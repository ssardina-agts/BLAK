import aos.jack.jak.plan.PlanInstanceInfo;

public class PlanIdInfo extends PlanInstanceInfo{

	public int plan_id;
	public double pSuccess;
	public double coverage;
	public double coverageWeight;
	public boolean isFailedThresholdHandler;

	public PlanIdInfo(int id, double prob){
		this(id, prob, 0.0, 1.0, false);
	}

	public PlanIdInfo(int id, double prob, double cov, double weight, boolean isFTH){
		super(5/*default precedence*/);
		plan_id = id;
		pSuccess = prob;
		coverage = cov;
		coverageWeight = weight;
		isFailedThresholdHandler = isFTH;
	}
}
