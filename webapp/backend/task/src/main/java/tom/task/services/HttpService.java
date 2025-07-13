package tom.task.services;

import java.util.Map;

public interface HttpService {

	// Non-HTTPS, No authentication
	<ResponseType> ResponseType getNoAuth(String url, Map<String, String> parameters, ResponseType dummy);

	// Non-HTTPS, BASIC authentication
	<ResponseType> ResponseType getBasicAuth(int userId, String url, Map<String, String> parameters, ResponseType dummy);

	// Non-HTTPS, Form-based authentication for Java EE
	<ResponseType> ResponseType getJavaEEFormAuth(int userId, String url, Map<String, String> parameters, ResponseType dummy);
}
