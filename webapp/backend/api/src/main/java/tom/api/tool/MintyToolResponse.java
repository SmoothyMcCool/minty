package tom.api.tool;

public class MintyToolResponse<T> {

	private final boolean success;
	private final String message;
	private final T data;

	public static <T> MintyToolResponse<T> SuccessResponse(T data) {
		return new MintyToolResponse<>(true, null, data);
	}

	public static <T> MintyToolResponse<T> FailureResponse(String message) {
		return new MintyToolResponse<>(false, message, null);
	}

	private MintyToolResponse() {
		this.success = true;
		this.message = "";
		this.data = null;
	}

	private MintyToolResponse(boolean success, String message, T data) {
		this.success = success;
		this.message = message;
		this.data = data;
	}

	public boolean isSuccess() {
		return success;
	}

	public T getData() {
		return data;
	}

	public String getMessage() {
		return message;
	}
}
