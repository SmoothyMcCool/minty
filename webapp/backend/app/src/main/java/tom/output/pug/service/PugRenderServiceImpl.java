package tom.output.pug.service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.exceptions.PugException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;
import tom.task.services.PugRenderService;

@Service
public class PugRenderServiceImpl implements PugRenderService {

	private final Logger logger = LogManager.getLogger(PugRenderServiceImpl.class);

	@Value("${pugTemplates}")
	private String pugFileLocation;

	@Value("${results.location}")
	private String resultsDir;

	private final PugConfiguration pugConfiguration;

	public PugRenderServiceImpl(PugConfiguration pugConfiguration) {
		this.pugConfiguration = pugConfiguration;
	}

	@Override
	public Path render(String template, String outfileName, Map<String, Object> data) throws IOException {

		Path location = Path.of(resultsDir + "/" + outfileName);

		try (FileWriter writer = new FileWriter(location.toString())) {
			PugTemplate pugTemplate = pugConfiguration.getTemplate(pugFileLocation + "/" + template);
			PugModel model = new PugModel(data);
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

}
