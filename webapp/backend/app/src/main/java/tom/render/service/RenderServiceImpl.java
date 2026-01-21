package tom.render.service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;
import tom.api.services.RenderService;
import tom.api.task.ExecutionResult;
import tom.api.task.Packet;
import tom.config.MintyConfiguration;

@Service
public class RenderServiceImpl implements RenderService {

	private static final Logger logger = LogManager.getLogger(RenderServiceImpl.class);

	private final PugConfiguration pugConfiguration;
	private final Path tempFolder;
	private final Path pugFolder;

	public RenderServiceImpl(PugConfiguration pugConfiguration, MintyConfiguration properties) {
		this.pugConfiguration = pugConfiguration;
		this.tempFolder = properties.getConfig().fileStores().temp();
		this.pugFolder = properties.getConfig().fileStores().pug();
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
		pugInput.put("id", data.getId());
		pugInput.put("text", data.getText());
		pugInput.put("data", data.getData());

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

			tempFilePath = Files.createTempFile(tempFolder, "pug-" + UUID.randomUUID(), ".pug");
			Files.write(tempFilePath, template.getBytes(StandardCharsets.UTF_8));

			PugTemplate pugTemplate = pugConfiguration.getTemplate(tempFilePath.toString());

			model.put("helpers", new HelperFunctions());

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
	public String getInlinePugTemplate(String template) throws IOException {
		Path templatePath = pugFolder.resolve("inline").resolve(template);
		return Files.readString(templatePath, StandardCharsets.UTF_8);
	}

	@Override
	public String getOutputPugTemplate(String template) throws IOException {
		Path templatePath = pugFolder.resolve("output").resolve(template);
		return Files.readString(templatePath, StandardCharsets.UTF_8);
	}

	@Override
	public List<String> listInlinePugTemplates() throws IOException {
		Path inlineFolder = pugFolder.resolve("inline");
		if (!Files.isDirectory(inlineFolder)) {
			logger.warn("listInlinePugTemplates: pugFolder is not properly defined in configuration.");
			return List.of();
		}

		List<String> results = new ArrayList<>();

		try (Stream<Path> stream = Files.list(inlineFolder)) {
			stream.filter(Files::isRegularFile).forEach(path -> results.add(path.getFileName().toString()));
		}

		return results;
	}

	@Override
	public List<String> listOutputPugTemplates() throws IOException {
		Path outputFolder = pugFolder.resolve("output");
		if (!Files.isDirectory(outputFolder)) {
			logger.warn("listOutputPugTemplates: pugFolder is not properly defined in configuration.");
			return List.of();
		}

		List<String> results = new ArrayList<>();

		try (Stream<Path> stream = Files.list(outputFolder)) {
			stream.filter(Files::isRegularFile).forEach(path -> results.add(path.getFileName().toString()));
		}

		return results;
	}
}
