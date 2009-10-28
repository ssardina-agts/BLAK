package trees;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

public interface Logger{
	public void indentReset();
	public void indentLeft();
	public void indentRight();
    public void writeLog(String msg);
}