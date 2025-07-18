package tom;

import java.util.ArrayList;
import java.util.List;

public class ApiException extends Exception {

	private static final long serialVersionUID = -6048445391269492873L;
	List<ApiError> errors = new ArrayList<>();

	public ApiException(ApiError error) {
		super(error.name());
		errors.add(error);
	}

	public ApiException(List<ApiError> errors) {
		super(ListToString(errors));
		this.errors.addAll(errors);
	}

	public List<ApiError> getApiErrors() {
		return errors;
	}

	static private String ListToString(List<ApiError> strings) {
		StringBuilder sb = new StringBuilder();
		for (ApiError as : strings) {
			sb.append(as.name());
		}
		return sb.toString();
	}
}
