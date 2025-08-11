package tom.result.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResultServiceImpl implements ResultService {

	@Value("${workflowResultsLocation}")
	private String resultsDir;

	public ResultServiceImpl() {
	}

	@Override
	public List<String> getAvailableResults() throws IOException {
		Path dir = Paths.get(resultsDir);
		List<String> results = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path entry : stream) {
				if (Files.isRegularFile(entry)) {
					results.add(entry.getFileName().toString());
				}
			}
		}
		return results;
	}

	@Override
	public String getResult(String taskName) throws IOException {
		Path dir = Paths.get(resultsDir);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path entry : stream) {
				if (entry.getFileName().endsWith(taskName)) {
					return Files.readString(entry);
				}
			}
		}
		return "Result not found.";
	}

	@Override
	public boolean deleteResult(String resultName) throws IOException {
		Path dir = Paths.get(resultsDir);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path entry : stream) {
				if (entry.getFileName().endsWith(resultName)) {
					Files.delete(entry);
					return true;
				}
			}
		}
		return false;
	}
}
