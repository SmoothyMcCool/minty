package tom.render.service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.exceptions.PugException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;
import tom.output.ExecutionResult;
import tom.task.services.RenderService;

@Service
public class RenderServiceImpl implements RenderService {

	private final Logger logger = LogManager.getLogger(RenderServiceImpl.class);

	@Value("${pugTemplates}")
	private String pugFileLocation;

	@Value("${taskResultsLocation}")
	private String resultsDir;

	private final PugConfiguration pugConfiguration;

	public RenderServiceImpl(PugConfiguration pugConfiguration) {
		this.pugConfiguration = pugConfiguration;
	}

	@Override
	public Path renderPug(String template, String outfileName, ExecutionResult data) throws IOException {

		Path location = Path.of(resultsDir + "/" + outfileName);

		try (FileWriter writer = new FileWriter(location.toString())) {
			PugTemplate pugTemplate = pugConfiguration.getTemplate(pugFileLocation + "/" + template);
			PugModel model = new PugModel(data.getResults());
			pugTemplate.process(model, writer);
		} catch (PugException e) {
			logger.error("PugRenderService: Caught PugException: ", e);
			throw e;
		} catch (IOException e) {
			logger.error("PugRenderService: Caught IOException: ", e);
			throw e;
		}

		return location;
	}

	@Override
	public Path renderJson(String outfileName, ExecutionResult data) throws IOException {
		Path location = Path.of(resultsDir + "/" + outfileName);
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.writerWithDefaultPrettyPrinter().writeValue(location.toFile(), data);

		return location;
	}

}
