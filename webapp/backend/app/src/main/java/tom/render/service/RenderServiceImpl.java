package tom.render.service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.exceptions.PugException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;
import tom.api.services.RenderService;
import tom.config.ExternalProperties;
import tom.output.ExecutionResult;
import tom.workflow.model.ResultTemplate;
import tom.workflow.service.WorkflowService;

@Service
public class RenderServiceImpl implements RenderService {

	private final Logger logger = LogManager.getLogger(RenderServiceImpl.class);

	private final String pugFileLocation;
	private final String resultsDir;
	private final PugConfiguration pugConfiguration;
	private final WorkflowService workflowService;

	public RenderServiceImpl(PugConfiguration pugConfiguration, WorkflowService workflowService,
			ExternalProperties properties) {
		this.pugConfiguration = pugConfiguration;
		this.workflowService = workflowService;
		resultsDir = properties.get("workflowResultsLocation");
		pugFileLocation = properties.get("pugTemplates");
	}

	@Override
	public String renderPug(String template, ExecutionResult data) throws IOException {

		if (template == null || data == null || pugFileLocation == null || resultsDir == null) {
			throw new IllegalArgumentException("A Required parameter is null");
		}

		Path tempFilePath = null;

		try (StringWriter writer = new StringWriter()) {

			ResultTemplate resultTemplate = workflowService.getResultTemplate(template);

			tempFilePath = Files.createTempFile(Paths.get(pugFileLocation), "pug-" + UUID.randomUUID(), ".pug");
			Files.write(tempFilePath, resultTemplate.getContent().getBytes());

			PugTemplate pugTemplate = pugConfiguration.getTemplate(tempFilePath.toString());
			PugModel model = new PugModel(data.toMap());

			pugTemplate.process(model, writer);
			return writer.toString();

		} catch (PugException e) {
			logger.error("PugRenderService: Caught PugException: ", e);
			throw e;
		} catch (IOException e) {
			logger.error("PugRenderService: Caught IOException: ", e);
			throw e;
		} finally {
			if (tempFilePath != null) {
				tempFilePath.toFile().delete();
			}
		}
	}

	@Override
	public String renderJson(ExecutionResult data) throws IOException {

		if (data == null || resultsDir == null) {
			throw new IllegalArgumentException("A Required parameter is null");
		}

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.writerWithDefaultPrettyPrinter().writeValue(writer, data);

		return writer.toString();
	}

}
