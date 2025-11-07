package tom.output.pug;

import java.util.ArrayList;
import java.util.List;

import tom.api.UserId;
import tom.api.services.TaskServices;
import tom.task.enumspec.EnumSpec;
import tom.task.enumspec.EnumSpecCreator;
import tom.task.enumspec.NameValuePair;

public class PugTemplateListEnumSpecCreator implements EnumSpecCreator {

	private TaskServices taskServices;
	private final String enumName;

	public PugTemplateListEnumSpecCreator() {
		enumName = "Pug Template";
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {
		List<String> templates = taskServices.getRenderService().listPugTemplates();

		List<NameValuePair> pairs = new ArrayList<>();
		for (String template : templates) {
			pairs.add(new NameValuePair(template, template));
		}

		EnumSpec spec = new EnumSpec();
		spec.setName(enumName);
		spec.setValues(pairs);
		return spec;
	}

	@Override
	public String getName() {
		return enumName;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}
}
