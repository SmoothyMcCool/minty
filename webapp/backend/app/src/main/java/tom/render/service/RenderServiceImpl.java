package tom.render.service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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

	private final PugConfiguration pugConfiguration;
	private final WorkflowService workflowService;
	private final String tempFolder;

	public RenderServiceImpl(PugConfiguration pugConfiguration, WorkflowService workflowService,
			ExternalProperties properties) {
		this.pugConfiguration = pugConfiguration;
		this.workflowService = workflowService;
		this.tempFolder = properties.get("tempFileStore");
	}

	@Override
	public String renderPug(String template, ExecutionResult data) throws PugException, IOException {

		if (template == null || data == null || tempFolder == null) {
			throw new IllegalArgumentException("A Required parameter is null");
		}

		Path tempFilePath = null;

		try (StringWriter writer = new StringWriter()) {

			ResultTemplate resultTemplate = workflowService.getResultTemplate(template);
			if (resultTemplate == null) {
				throw new RuntimeException("resultTemplate is null!");
			}

			tempFilePath = Files.createTempFile(Paths.get(tempFolder), "pug-" + UUID.randomUUID(), ".pug");
			Files.write(tempFilePath, resultTemplate.getContent().getBytes(StandardCharsets.UTF_8));

			PugTemplate pugTemplate = pugConfiguration.getTemplate(tempFilePath.toString());
			PugModel model = new PugModel(data.toMap());

			pugTemplate.process(model, writer);
			return writer.toString();

		} finally {
			if (tempFilePath != null) {
				tempFilePath.toFile().delete();
			}
		}
	}

	@Override
	public String renderJson(ExecutionResult data) throws IOException {

		if (data == null) {
			throw new IllegalArgumentException("Data is null");
		}

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.writerWithDefaultPrettyPrinter().writeValue(writer, data);

		return writer.toString();
	}

}
