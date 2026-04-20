package tom.assistant.service.agent.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmResponse {

	private static final ObjectMapper mapper = new ObjectMapper();

	private LlmStatus status;
	private String message;
	private Object data;

	private boolean structured;

	@Override
	public String toString() {
		return "LlmResponse [status=" + status + ", message=" + message + ", data=" + data + ", structured="
				+ structured + "]";
	}

	public LlmStatus getStatus() {
		return status;
	}

	public void setStatus(LlmStatus status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public static LlmParseResult parse(String raw) {
		try {
			LlmResponse response = mapper.readValue(raw, LlmResponse.class);
			return new LlmParseResult(true, response, null);

		} catch (Exception e) {
			return new LlmParseResult(false, null, raw);
		}
	}

}
