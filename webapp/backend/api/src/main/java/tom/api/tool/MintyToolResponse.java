package tom.api.tool;

public class MintyToolResponse<T> {

	private final ToolStatus status;
	private final String message;
	private final T data;

	public static <T> MintyToolResponse<T> SuccessResponse(T data) {
		return new MintyToolResponse<>(ToolStatus.Ok, null, data);
	}

	public static <T> MintyToolResponse<T> FailureResponse(String message) {
		return new MintyToolResponse<>(ToolStatus.Error, message, null);
	}

	private MintyToolResponse() {
		this.status = ToolStatus.Ok;
		this.message = "";
		this.data = null;
	}

	private MintyToolResponse(ToolStatus status, String message, T data) {
		this.status = status;
		this.message = message;
		this.data = data;
	}

	public ToolStatus getStatus() {
		return status;
	}

	public T getData() {
		return data;
	}

	public String getMessage() {
		return message;
	}
}
