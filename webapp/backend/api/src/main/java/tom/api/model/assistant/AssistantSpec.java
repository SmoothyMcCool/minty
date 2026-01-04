package tom.api.model.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.AssistantId;
import tom.api.services.assistant.AssistantManagementService;

public class AssistantSpec {

	private AssistantId assistantId;
	private Assistant assistant;

	public AssistantSpec() {
		this.assistantId = AssistantManagementService.DefaultAssistantId;
		this.assistant = null;
	}

	public AssistantSpec(AssistantId assistantId, Assistant assistant) {
		this.assistantId = assistantId;
		this.assistant = assistant;
	}

	public AssistantId getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(AssistantId assistantId) {
		this.assistantId = assistantId;
	}

	public Assistant getAssistant() {
		return assistant;
	}

	public void setAssistant(Assistant assistant) {
		this.assistant = assistant;
	}

	public boolean useId() {
		return assistantId != null && !assistantId.equals(AssistantManagementService.DefaultAssistantId);
	}

	public String toJson() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "";
		}
	}
}
