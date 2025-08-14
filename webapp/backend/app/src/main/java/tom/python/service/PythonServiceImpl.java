package tom.python.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.services.PythonService;

@Service
public class PythonServiceImpl implements PythonService {

	private static final Logger logger = LogManager.getLogger(PythonServiceImpl.class);

	@Value("${pythonScripts}")
	private String pythonScripts;

	@Value("${tempFileStore}")
	private String tempFileDir;

	@Override
	public Map<String, Object> execute(String pythonFile, Map<String, String> inputDictionary) {
		Path inputFilePath = null;
		Path outputFilePath = null;

		inputFilePath = Paths.get(tempFileDir);
		if (!inputFilePath.toFile().isDirectory()) {
			logger.error("Specified tempFileStore is not a directory.");
			return Map.of();
		}

		try {

			ObjectMapper mapper = new ObjectMapper();

			inputFilePath = Files.createTempFile(inputFilePath, "py-out-", ".tom");
			String code = inputDictionary.remove("Code");
			Files.writeString(inputFilePath, mapper.writeValueAsString(inputDictionary));
			inputDictionary.put("Code", code);

			outputFilePath = Files.createTempFile(Paths.get(tempFileDir), "py-out-", ".tom");

			ProcessBuilder processBuilder = new ProcessBuilder("python", pythonScripts + "/" + pythonFile,
					inputFilePath.toString(), outputFilePath.toString());
			processBuilder.redirectErrorStream(true); // Merge error stream with standard output stream. Not that it
														// matters because we are throwing the output on the floor.

			Process process = processBuilder.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				logger.warn("Script " + pythonFile + " failed with exit code " + exitCode);
			}

			Map<String, Object> result = fileToMap(outputFilePath);
			return result;

		} catch (IOException | InterruptedException e) {

			logger.warn("Exception while trying to run python file. ", e);
			return Map.of();

		} finally {

			if (inputFilePath != null && inputFilePath.toFile().isFile()) {
				inputFilePath.toFile().delete();
			}
			if (outputFilePath != null && outputFilePath.toFile().isFile()) {
				outputFilePath.toFile().delete();
			}

		}

	}

	@Override
	public Map<String, Object> executeCodeString(String code, Map<String, String> inputDictionary) {
		Path tempPyFile = null;
		try {
			tempPyFile = Files.createTempFile(Path.of(pythonScripts), "tempPy-", ".py");
			Files.writeString(tempPyFile, code);

			return execute(tempPyFile.toString(), inputDictionary);

		} catch (IOException e) {
			logger.warn("Exception while trying to run python codestring: ", e);
			return new HashMap<>();

		} finally {
			if (tempPyFile != null) {
				tempPyFile.toFile().delete();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> fileToMap(Path outputFilePath) {
		try {
			String content = Files.readString(outputFilePath);
			ObjectMapper mapper = new ObjectMapper();

			return mapper.readValue(content, Map.class);

		} catch (IOException e) {
			logger.warn("Could not read Python output file " + outputFilePath + ": ", e);
			return new HashMap<>();
		}
	}

}
