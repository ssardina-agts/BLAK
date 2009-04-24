package treeGenerator;

public class ChangeDescription 
{
	
	private String variableName;
	private boolean switchMode;//switch mode describes what the variable will be to the output
	
	public ChangeDescription(String name, boolean mode)
	{
		this.variableName = name;
		this.switchMode = mode;
	}

	public boolean isSwitchMode() {
		return switchMode;
	}

	public void setSwitchMode(boolean switchMode) {
		this.switchMode = switchMode;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableName() {
		return variableName;
	}
	
	
}
