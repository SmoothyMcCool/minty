package tom.tasks.transform.htmlformatter;

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

public class InlineTemplateHtmlSpecCreator implements EnumSpecCreator {

	public static final String EnumName = "Inline Pug Template";
	private static final Logger logger = LogManager.getLogger(InlineTemplateHtmlSpecCreator.class);

	private final String enumName;
	private PluginServices pluginServices;

	public InlineTemplateHtmlSpecCreator() {
		enumName = EnumName;
		pluginServices = null;
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {

		EnumSpec spec = new EnumSpec();
		spec.setName(enumName);

		List<String> templateNames;
		try {
			templateNames = pluginServices.getRenderService().listInlinePugTemplates();
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
