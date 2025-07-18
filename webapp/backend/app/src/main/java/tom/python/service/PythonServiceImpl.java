package tom.python.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.services.PythonService;

@Service
public class PythonServiceImpl implements PythonService {

	private static final Logger logger = LogManager.getLogger(PythonServiceImpl.class);

	@Value("${pythonScripts}")
	private String pythonScripts;

	@Value("${tempFileStore}")
	private String tempFileDir;

	@Override
	public Map<String, Object> execute(String pythonFile, Map<String, String> inputDictionary) {
		Path inputFilePath = Paths.get(tempFileDir);
		if (!inputFilePath.toFile().isDirectory()) {
			logger.error("Specified tempFileStore is not a directory.");
		}

		Path outputFilePath = null;
		try {
			outputFilePath = Files.createTempFile(Paths.get(tempFileDir), "py-out-", ".tom");
			ObjectMapper mapper = new ObjectMapper();
			Files.writeString(outputFilePath, mapper.writeValueAsString(inputDictionary));
		} catch (IOException e) {
			logger.warn("Could not create temporary file!");
			return null;
		}

		ProcessBuilder processBuilder = new ProcessBuilder("python", pythonScripts + "/" + pythonFile,
				inputFilePath.toString(), outputFilePath.toString());
		processBuilder.redirectErrorStream(true); // Merge error stream with standard output stream. Not that it matters
													// because we are throwing the output on the floor.

		try {
			Process process = processBuilder.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				logger.warn("Script " + pythonFile + " failed with exit code " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			logger.warn("Failed to run script: " + e);
		}

		Map<String, Object> result = fileToMap(outputFilePath);
		outputFilePath.toFile().delete();
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> fileToMap(Path outputFilePath) {
		try {
			String content = Files.readString(outputFilePath);
			ObjectMapper mapper = new ObjectMapper();

			return mapper.readValue(content, Map.class);

		} catch (IOException e) {
			logger.warn("Could not read Python output file " + outputFilePath + ": " + e);
			return null;
		}
	}

}
