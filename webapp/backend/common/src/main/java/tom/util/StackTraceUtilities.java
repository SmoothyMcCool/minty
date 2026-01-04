package tom.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTraceUtilities {

	public static String StackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}

}
