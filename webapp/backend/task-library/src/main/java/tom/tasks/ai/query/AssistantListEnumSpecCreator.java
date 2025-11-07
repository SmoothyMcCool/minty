package tom.tasks.ai.query;

import java.util.ArrayList;
import java.util.List;

import tom.api.UserId;
import tom.api.services.TaskServices;
import tom.model.Assistant;
import tom.task.enumspec.EnumSpec;
import tom.task.enumspec.EnumSpecCreator;
import tom.task.enumspec.NameValuePair;

public class AssistantListEnumSpecCreator implements EnumSpecCreator {

	private TaskServices taskServices;
	private final String enumName;

	public AssistantListEnumSpecCreator() {
		enumName = "Assistant";
	}

	@Override
	public EnumSpec getEnumList(UserId userId) {
		List<Assistant> assistants = taskServices.getAssistantManagementService().listAssistants(userId);

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
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}
}
