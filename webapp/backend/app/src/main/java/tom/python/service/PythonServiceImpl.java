package tom.python.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
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
import tom.api.task.Packet;
import tom.config.MintyConfiguration;

@Service
public class PythonServiceImpl implements PythonService {

	private static final Logger logger = LogManager.getLogger(PythonServiceImpl.class);

	private final Path tempFileDir;

	public PythonServiceImpl(MintyConfiguration properties) {
		tempFileDir = properties.getConfig().fileStores().temp();
	}

	@Override
	public PythonResult execute(String pythonFile, Packet input) throws PythonException {
		Path inputFilePath = tempFileDir;
		Path outputFilePath = null;
		List<String> logs = new ArrayList<>();

		if (!inputFilePath.toFile().isDirectory()) {
			logs.add("Error: Specified tempFileStore is not a directory.");
			return new PythonResult(null, logs);
		}

		try {

			ObjectMapper mapper = new ObjectMapper();

			inputFilePath = Files.createTempFile(inputFilePath, "py-in-", ".tom");
			Files.writeString(inputFilePath, mapper.writeValueAsString(input));

			outputFilePath = Files.createTempFile(tempFileDir, "py-out-", ".tom");

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

			Map<String, Object> result = fileToMap(outputFilePath, logs);
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
	public PythonResult executeCodeString(String code, Packet input) throws PythonException {
		Path tempPyFile = null;
		try {
			tempPyFile = Files.createTempFile(tempFileDir, "tempPy-", ".py");
			Files.writeString(tempPyFile, code);

			return execute(tempPyFile.toString(), input);

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
	private Map<String, Object> fileToMap(Path outputFilePath, List<String> logs) throws IOException {
		String content = Files.readString(outputFilePath);
		logs.add("DEBUG - Raw python output: " + content);
		ObjectMapper mapper = new ObjectMapper();

		return mapper.readValue(content, Map.class);
	}

}
