package tom.api.model.assistant;

import com.fasterxml.jackson.annotation.JsonCreator;

import tom.api.AssistantId;
import tom.api.MintyObjectMapper;
import tom.api.services.assistant.AssistantManagementService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class AssistantSpec {

	private AssistantId assistantId;
	private Assistant assistant;

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static AssistantSpec from(Object input) {
		if (input == null) {
			return null;
		}

		if (input instanceof AssistantSpec spec) {
			return spec;
		}

		if (input instanceof AssistantId id) {
			return new AssistantSpec(id);
		}

		if (input instanceof Assistant assistant) {
			return new AssistantSpec(assistant);
		}

		if (input instanceof String str) {
			return new AssistantSpec(new AssistantId(str));
		}

		ObjectMapper mapper = MintyObjectMapper.StandardJsonMapper;

		// JSON object case
		if (input instanceof java.util.Map<?, ?> map) {

			if (map.containsKey("assistantId") || map.containsKey("assistant")) {
				AssistantSpec spec = new AssistantSpec();

				if (map.get("assistantId") != null) {
					AssistantId id = mapper.convertValue(map.get("assistantId"), AssistantId.class);
					spec.setAssistantId(id);
				}

				if (map.get("assistant") != null) {
					Assistant assistant = mapper.convertValue(map.get("assistant"), Assistant.class);
					spec.setAssistant(assistant);
				}

				return spec;
			}

			try {
				Assistant assistant = mapper.convertValue(map, Assistant.class);
				return new AssistantSpec(assistant);
			} catch (Exception ignored) {
			}

			try {
				AssistantId id = mapper.convertValue(map, AssistantId.class);
				return new AssistantSpec(id);
			} catch (Exception ignored) {
			}
		}

		throw new IllegalArgumentException("Cannot convert to AssistantSpec from: " + input.getClass());
	}

	public AssistantSpec() {
		this.assistantId = AssistantManagementService.DefaultAssistantId;
		this.assistant = null;
	}

	public AssistantSpec(AssistantId assistantId) {
		this.assistantId = assistantId;
		this.assistant = null;
	}

	public AssistantSpec(Assistant assistant) {
		this.assistantId = null;
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
		ObjectMapper mapper = MintyObjectMapper.StandardJsonMapper;
		try {
			return mapper.writeValueAsString(this);
		} catch (JacksonException e) {
			return "";
		}
	}

	@Override
	public String toString() {
		return "AssistantSpec [assistantId=" + assistantId + ", assistant=" + assistant + "]";
	}

}
