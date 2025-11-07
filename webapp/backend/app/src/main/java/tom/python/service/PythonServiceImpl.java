package tom.python.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.services.PythonService;
import tom.config.ExternalProperties;

@Service
public class PythonServiceImpl implements PythonService {

	private static final Logger logger = LogManager.getLogger(PythonServiceImpl.class);

	private final String pythonScripts;
	private final String tempFileDir;

	public PythonServiceImpl(ExternalProperties properties) {
		pythonScripts = properties.get("pythonScripts");
		tempFileDir = properties.get("tempFileStore");
	}

	@Override
	public Map<String, Object> execute(String pythonFile, Map<String, Object> inputDictionary) {
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
			Files.writeString(inputFilePath, mapper.writeValueAsString(inputDictionary));

			outputFilePath = Files.createTempFile(Paths.get(tempFileDir), "py-out-", ".tom");

			ProcessBuilder processBuilder = new ProcessBuilder("python", pythonFile, inputFilePath.toString(),
					outputFilePath.toString());
			processBuilder.redirectErrorStream(true); // Merge error stream with standard output stream. Not that it
														// matters because we are throwing the output on the floor.

			Process process = processBuilder.start();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

				String line;
				while ((line = reader.readLine()) != null) {
					// Log each line from the Python process
					logger.info("[Python:{}] {}", pythonFile, line);
				}
			}
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				logger.warn("Script " + pythonFile + " failed with exit code " + exitCode);
			}

			Map<String, Object> result = fileToMap(outputFilePath);
			return result;

		} catch (IOException e) {
			logger.warn("IOException while trying to run python file. ", e);
			throw new RuntimeException("IOException while trying to run python file: ", e);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn("InterruptedException while trying to run python file. ", e);
			throw new RuntimeException("InterruptedException while trying to run python file: ", e);

		} catch (Exception e) {
			Thread.currentThread().interrupt();
			logger.warn("Exception while trying to run python file. ", e);
			throw new RuntimeException("Exception while trying to run python file: ", e);

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
	public Map<String, Object> executeCodeString(String code, Map<String, Object> inputDictionary) {
		Path tempPyFile = null;
		try {
			tempPyFile = Files.createTempFile(Path.of(pythonScripts), "tempPy-", ".py");
			Files.writeString(tempPyFile, code);

			return execute(tempPyFile.toString(), inputDictionary);

		} catch (IOException e) {
			logger.warn("Exception while trying to run python codestring: ", e);
			throw new RuntimeException("Exception while trying to run python codestring: ", e);

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
