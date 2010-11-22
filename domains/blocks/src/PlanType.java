import aos.jack.jak.plan.PlanInstanceInfo;

public class PlanType extends PlanInstanceInfo
{
	public int choice;	
	PlanType(int t)
	{
		super(5);
		choice = t;
	}
}
