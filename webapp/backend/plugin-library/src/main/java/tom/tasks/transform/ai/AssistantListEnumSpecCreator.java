package tom.tasks.transform.ai;

import java.util.ArrayList;
import java.util.List;

import tom.api.UserId;
import tom.api.model.assistant.Assistant;
import tom.api.services.PluginServices;
import tom.api.task.enumspec.EnumSpec;
import tom.api.task.enumspec.EnumSpecCreator;
import tom.api.task.enumspec.NameValuePair;

public class AssistantListEnumSpecCreator implements EnumSpecCreator {

	public static final String EnumName = "Assistant";
	private PluginServices pluginServices;
	private final String enumName;

	public AssistantListEnumSpecCreator() {
		enumName = EnumName;
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {
		List<Assistant> assistants = pluginServices.getAssistantManagementService().listAssistants(userId);

		List<NameValuePair> pairs = new ArrayList<>();
		for (Assistant assistant : assistants) {
			pairs.add(new NameValuePair(assistant.name(), assistant.id().getValue().toString()));
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
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}
}
