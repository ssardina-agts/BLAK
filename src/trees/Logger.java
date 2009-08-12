package trees;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

public interface Logger{
    public void writeLog(String msg, int indent);
}