package agents;


public class Config {

    public enum UpdateMode {
        CONCURRENT,
        STABLE,
    }
 
    public enum PlanSelectMode {
        PROBABILISTIC,
        COVERAGE,
    }
}