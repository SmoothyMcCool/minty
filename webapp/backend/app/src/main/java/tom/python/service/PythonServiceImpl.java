package tom.python.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.services.python.PythonException;
import tom.api.services.python.PythonResult;
import tom.api.services.python.PythonService;
import tom.api.MintyProperties;

@Service
public class PythonServiceImpl implements PythonService {

	private static final Logger logger = LogManager.getLogger(PythonServiceImpl.class);

	private final String pythonScripts;
	private final String tempFileDir;

	public PythonServiceImpl(MintyProperties properties) {
		pythonScripts = properties.get("pythonScripts");
		tempFileDir = properties.get("tempFileStore");
	}

	@Override
	public PythonResult execute(String pythonFile, List<Map<String, Object>> inputDictionary) throws PythonException {
		Path inputFilePath = null;
		Path outputFilePath = null;
		List<String> logs = new ArrayList<>();

		inputFilePath = Paths.get(tempFileDir);
		if (!inputFilePath.toFile().isDirectory()) {
			logs.add("Error: Specified tempFileStore is not a directory.");
			return new PythonResult(null, logs);
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
					logs.add(String.format("Python:" + line));
				}
			}
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				logs.add("Warning: Script " + pythonFile + " failed with exit code " + exitCode);
			}

			Map<String, Object> result = fileToMap(outputFilePath);
			return new PythonResult(result, logs);

		} catch (IOException e) {
			throw new PythonException("IOException while trying to run python file: " + pythonFile, logs, e);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PythonException("InterruptedException while trying to run python file: " + pythonFile, logs, e);

		} catch (Exception e) {
			throw new PythonException("Exception while trying to run python file: " + pythonFile, logs, e);

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
	public PythonResult executeCodeString(String code, List<Map<String, Object>> inputDictionary)
			throws PythonException {
		Path tempPyFile = null;
		try {
			tempPyFile = Files.createTempFile(Path.of(pythonScripts), "tempPy-", ".py");
			Files.writeString(tempPyFile, code);

			return execute(tempPyFile.toString(), inputDictionary);

		} catch (IOException e) {
			logger.warn("Exception while trying to run python codestring: ", e);
			throw new PythonException("Exception while trying to run python codestring: ", List.of(), e);

		} finally {
			if (tempPyFile != null) {
				tempPyFile.toFile().delete();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> fileToMap(Path outputFilePath) throws IOException {
		String content = Files.readString(outputFilePath);
		ObjectMapper mapper = new ObjectMapper();

		return mapper.readValue(content, Map.class);
	}

}
