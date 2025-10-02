package tom.api.services;

import java.util.Map;

import tom.api.UserId;

public interface HttpService {

	// Non-HTTPS, No authentication
	<ResponseType> ResponseType getNoAuth(String url, Map<String, String> parameters, ResponseType dummy);

	// Non-HTTPS, BASIC authentication
	<ResponseType> ResponseType getBasicAuth(UserId userId, String url, Map<String, String> parameters,
			ResponseType dummy);

	// Non-HTTPS, Form-based authentication for Java EE
	<ResponseType> ResponseType getJavaEEFormAuth(UserId userId, String url, Map<String, String> parameters,
			ResponseType dummy);
}
