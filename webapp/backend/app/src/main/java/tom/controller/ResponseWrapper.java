package tom.controller;

import java.util.ArrayList;
import java.util.List;

import tom.ApiError;

public class ResponseWrapper<T> {

	private boolean ok;
	private int code;
	private List<String> messages;
	private T data;

	public static <T> ResponseWrapper<T> SuccessResponse(T data) {
		return new ResponseWrapper<>(true, 200, null, data);
	}

	public static <T> ResponseWrapper<T> FailureResponse(int code, String message) {
		List<String> strMsg = new ArrayList<>();
		strMsg.add(message);
		return new ResponseWrapper<>(false, code, strMsg, null);
	}

	public static <T> ResponseWrapper<T> FailureResponse(int code, List<String> messages) {
		return new ResponseWrapper<>(false, code, messages, null);
	}

	public static <T> ResponseWrapper<T> ApiFailureResponse(int code, List<ApiError> messages) {
		List<String> strMsg = new ArrayList<>();
		for (ApiError as : messages) {
			strMsg.add(as.name());
		}
		return new ResponseWrapper<>(false, code, strMsg, null);
	}

	private ResponseWrapper() {
		this.ok = true;
		this.code = 200;
		this.messages = null;
		this.data = null;
	}

	private ResponseWrapper(boolean ok, int code, List<String> messages, T data) {
		this.ok = ok;
		this.code = code;
		this.messages = messages;
		this.data = data;
	}

	public boolean isOk() {
		return ok;
	}

	public void setOk(boolean ok) {
		this.ok = ok;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public void setData(T data) {
		this.data = data;
	}

	public T getData() {
		return data;
	}

	public void setMessages(List<String> messages) {
		this.messages = messages;
	}

	public List<String> getMessages() {
		return messages;
	}
}
