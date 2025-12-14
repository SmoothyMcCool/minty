package tom.tasks.transform.ai;

import java.util.ArrayList;
import java.util.List;

import tom.api.UserId;
import tom.api.model.Assistant;
import tom.api.services.TaskServices;
import tom.api.task.enumspec.EnumSpec;
import tom.api.task.enumspec.EnumSpecCreator;
import tom.api.task.enumspec.NameValuePair;

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
