package tom.output.pug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.api.UserId;
import tom.api.services.PluginServices;
import tom.api.task.enumspec.EnumSpec;
import tom.api.task.enumspec.EnumSpecCreator;
import tom.api.task.enumspec.NameValuePair;

public class OutputTemplateHtmlSpecCreator implements EnumSpecCreator {

	public static final String EnumName = "Output Pug Template";
	private static final Logger logger = LogManager.getLogger(OutputTemplateHtmlSpecCreator.class);

	private final String enumName;
	private PluginServices pluginServices;

	public OutputTemplateHtmlSpecCreator() {
		enumName = EnumName;
		pluginServices = null;
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {

		EnumSpec spec = new EnumSpec();
		spec.setName(enumName);

		List<String> templateNames;
		try {
			templateNames = pluginServices.getRenderService().listOutputPugTemplates();
		} catch (IOException e) {
			logger.warn("Failed to list pug templates");
			return spec;
		}

		List<NameValuePair> pairs = new ArrayList<>();
		for (String templateName : templateNames) {
			pairs.add(new NameValuePair(templateName, templateName));
		}

		spec.setValues(pairs);
		return spec;
	}

	@Override
	public String getName() {
		return enumName;
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}
}
