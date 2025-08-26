package tom.tasks.extractor.gitlab;

import java.util.ArrayList;
import java.util.List;

public class CodeChunk {
	String filePath;
	int startLine;
	int endLine;
	List<String> lines = new ArrayList<>();
	String mergeRequestId;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(
				"// MR: " + mergeRequestId + ", File: " + filePath + ", Lines: " + startLine + "-" + endLine + "\n");
		for (String line : lines)
			sb.append(line).append("\n");
		return sb.toString();
	}
}
