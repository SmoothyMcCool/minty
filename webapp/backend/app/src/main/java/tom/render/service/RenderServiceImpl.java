package tom.render.service;

import java.io.IOException;
import java.io.StringWriter;

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
import tom.api.services.RenderService;
import tom.output.ExecutionResult;

@Service
public class RenderServiceImpl implements RenderService {

	private final Logger logger = LogManager.getLogger(RenderServiceImpl.class);

	@Value("${pugTemplates}")
	private String pugFileLocation;

	@Value("${workflowResultsLocation}")
	private String resultsDir;

	private final PugConfiguration pugConfiguration;

	public RenderServiceImpl(PugConfiguration pugConfiguration) {
		this.pugConfiguration = pugConfiguration;
	}

	@Override
	public String renderPug(String template, ExecutionResult data) throws IOException {

		if (template == null || data == null || pugFileLocation == null || resultsDir == null) {
			throw new IllegalArgumentException("A Required parameter is null");
		}

		try (StringWriter writer = new StringWriter()) {
			PugTemplate pugTemplate = pugConfiguration.getTemplate(pugFileLocation + "/" + template);
			PugModel model = new PugModel(data.toMap());
			pugTemplate.process(model, writer);
			return writer.toString();
		} catch (PugException e) {
			logger.error("PugRenderService: Caught PugException: ", e);
			throw e;
		} catch (IOException e) {
			logger.error("PugRenderService: Caught IOException: ", e);
			throw e;
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
