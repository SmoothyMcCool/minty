package tom.tasks.extractor.gitlab;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.AiTask;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Get a GitLab Merge Request", configClass = "tom.tasks.extractor.gitlab.GitLabMergRequestExtractorConfig")
public class GitLabMergRequestExtractor implements AiTask {

	private final Logger logger = LogManager.getLogger(GitLabMergRequestExtractor.class);

	private GitLabMergRequestExtractorConfig configuration;
	private UUID uuid = UUID.randomUUID();
	private String error = null;
	private Map<String, Object> result = new HashMap<>();

	public GitLabMergRequestExtractor() {
	}

	public GitLabMergRequestExtractor(GitLabMergRequestExtractorConfig configuration) {
		this.configuration = configuration;
	}

	@Override
	public String taskName() {
		return "GitLabMergRequestExtractor-" + uuid.toString();
	}

	@Override
	public Map<String, Object> getResult() {
		return result;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public List<Map<String, Object>> runTask() {
		String changesJson;
		try {
			String baseUrl = configuration.getBaseUrl();
			if (baseUrl.endsWith("/")) {
				baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
			}
			if (!baseUrl.endsWith("/api/v4")) {
				baseUrl = baseUrl + "/api/v4";
			}

			changesJson = fetchGitLabJson(configuration.getBaseUrl() + "/projects/" + configuration.getProjectId()
					+ "/merge_requests/" + configuration.getMergeRequestId() + "/diffs",
					configuration.getPrivateToken());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(changesJson);
			String targetBranch = root.get("target_branch").asText();

			List<CodeChunk> finalChunks = new ArrayList<>();

			for (JsonNode change : root.get("changes")) {
				String filePath = change.get("new_path").asText();
				String diff = change.get("diff").asText();
				String fileContent = fetchFileContent(baseUrl, configuration.getProjectId(), filePath, targetBranch,
						configuration.getPrivateToken());
				String[] fileLines = fileContent.split("\n");

				List<AnnotatedLine> annotated = annotateLines(diff, fileLines);
				List<int[]> ranges = mergeAnnotatedRanges(annotated, configuration.getContextLines());

				for (int[] range : ranges) {
					int start = range[0];
					while (start <= range[1]) {
						int end = Math.min(start + configuration.getMaxLinesPerChunk() - 1, range[1]);
						CodeChunk chunk = new CodeChunk();
						chunk.filePath = filePath;
						chunk.startLine = start + 1; // convert 0-indexed to 1-indexed
						chunk.endLine = end + 1;
						chunk.mergeRequestId = configuration.getMergeRequestId();

						for (int i = start; i <= end; i++) {
							AnnotatedLine al = annotated.get(i);
							chunk.lines.add("[" + al.type + "] " + al.content);
						}
						finalChunks.add(chunk);
						start = end + 1;
					}
				}
			}

			StringBuilder builder = new StringBuilder();
			for (CodeChunk chunk : finalChunks) {
				builder.append(chunk);
				builder.append("-----");
			}

			result = Map.of("Data", builder.toString());
			return List.of(result);

		} catch (Exception e) {
			logger.warn("GitLabMergRequestExtractor failed with exception. ", e);
			error = "GitLabMergRequestExtractor failed with exception. " + e;
			throw new RuntimeException("GitLabMergRequestExtractor failed with exception. ", e);
		}
	}

	@Override
	public void setInput(Map<String, Object> input) {
		configuration.updateFrom(input);
	}

	@Override
	public String expects() {
		return "This task will use any fields in the input that match the configuration field names as overrides.";
	}

	@Override
	public String produces() {
		return "This task produces a single record with a \"Data\" field that contains a string representation of the merge request, suitable for AI consumption.";
	}

	private static List<AnnotatedLine> annotateLines(String diff, String[] fileLines) {
		List<AnnotatedLine> annotated = new ArrayList<>();
		String[] diffLines = diff.split("\n");

		int newFileLineNum = 0; // line number in the new file, 1-based
		for (String line : diffLines) {
			if (line.startsWith("@@")) {
				// Parse hunk header: @@ -oldStart,oldLen +newStart,newLen @@
				String[] parts = line.split(" ");
				String newPart = parts[2]; // +start,len
				newFileLineNum = Integer.parseInt(newPart.split(",")[0].substring(1));
			} else if (line.startsWith("+") && !line.startsWith("+++")) {
				annotated.add(new AnnotatedLine(line.substring(1), "added"));
				newFileLineNum++;
			} else if (line.startsWith("-") && !line.startsWith("---")) {
				annotated.add(new AnnotatedLine(line.substring(1), "removed"));
			} else {
				// context line
				if (newFileLineNum > 0 && newFileLineNum <= fileLines.length) {
					annotated.add(new AnnotatedLine(fileLines[newFileLineNum - 1], "unchanged"));
					newFileLineNum++;
				}
			}
		}

		return annotated;
	}

	private static List<int[]> mergeAnnotatedRanges(List<AnnotatedLine> annotated, int contextLines) {
		List<int[]> ranges = new ArrayList<>();
		for (int i = 0; i < annotated.size(); i++) {
			if (annotated.get(i).type.equals("added") || annotated.get(i).type.equals("removed")) {
				int start = Math.max(0, i - contextLines);
				int end = Math.min(annotated.size() - 1, i + contextLines);
				if (!ranges.isEmpty() && start <= ranges.get(ranges.size() - 1)[1]) {
					ranges.get(ranges.size() - 1)[1] = end;
				} else {
					ranges.add(new int[] { start, end });
				}
			}
		}
		return ranges;
	}

	private static String fetchGitLabJson(String url, String token) throws Exception {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(url);
			request.addHeader("PRIVATE-TOKEN", token);

			ClassicHttpResponse response = client.executeOpen(null, request, null);
			if (response.getCode() != 200) {
				throw new RuntimeException("GitLab API error: " + response.getCode());
			}

			Header contentType = response.getHeader("Content-Type");
			if (!contentType.getValue().equals(ContentType.APPLICATION_JSON.toString())) {
				throw new RuntimeException("GitLab API error: Did not return application/json. Instead returned "
						+ contentType.getValue());
			}

			return EntityUtils.toString(response.getEntity());
		}
	}

	private static String fetchFileContent(String baseUrl, String projectId, String filePath, String ref, String token)
			throws Exception {
		String encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8);
		String url = baseUrl + "/projects/" + projectId + "/repository/files/" + encodedPath + "/raw?ref="
				+ URLEncoder.encode(ref, StandardCharsets.UTF_8);
		return fetchGitLabJson(url, token);
	}

}
