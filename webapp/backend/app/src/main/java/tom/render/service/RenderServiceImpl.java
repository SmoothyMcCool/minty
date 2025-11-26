package tom.render.service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;
import tom.api.services.RenderService;
import tom.config.ExternalProperties;
import tom.output.pug.HelperFunctions;
import tom.task.ExecutionResult;
import tom.task.Packet;
import tom.workflow.service.WorkflowService;

@Service
public class RenderServiceImpl implements RenderService {

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
	public String renderPug(String template, ExecutionResult data) throws IOException {
		if (data == null) {
			throw new IllegalArgumentException("Data is null");
		}
		PugModel model = new PugModel(data.toMap());
		return internalRenderPug(template, model);
	}

	@Override
	public String renderPug(String template, Packet data) throws IOException {
		if (data == null) {
			throw new IllegalArgumentException("Data is null");
		}
		Map<String, Object> pugInput = new HashMap<>();
		pugInput.put("Id", data.getId());
		pugInput.put("Text", data.getText());
		pugInput.put("Data", data.getData());

		PugModel model = new PugModel(pugInput);
		return internalRenderPug(template, model);
	}

	private String internalRenderPug(String template, PugModel model) throws IOException {
		if (template == null) {
			throw new IllegalArgumentException("Template is null");
		} else if (tempFolder == null) {
			throw new IllegalArgumentException("tempFolder is null");
		}

		Path tempFilePath = null;

		try (StringWriter writer = new StringWriter()) {

			tempFilePath = Files.createTempFile(Paths.get(tempFolder), "pug-" + UUID.randomUUID(), ".pug");
			Files.write(tempFilePath, template.getBytes(StandardCharsets.UTF_8));

			PugTemplate pugTemplate = pugConfiguration.getTemplate(tempFilePath.toString());

			model.put("Helpers", new HelperFunctions());

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

	@Override
	public List<String> listPugTemplates() {
		return workflowService.listResultTemplates();
	}

}
