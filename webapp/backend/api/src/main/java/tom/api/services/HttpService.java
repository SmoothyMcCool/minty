package tom.api.services;

import java.util.Map;
import java.util.UUID;

public interface HttpService {

	// Non-HTTPS, No authentication
	<ResponseType> ResponseType getNoAuth(String url, Map<String, String> parameters, ResponseType dummy);

	// Non-HTTPS, BASIC authentication
	<ResponseType> ResponseType getBasicAuth(UUID userId, String url, Map<String, String> parameters,
			ResponseType dummy);

	// Non-HTTPS, Form-based authentication for Java EE
	<ResponseType> ResponseType getJavaEEFormAuth(UUID userId, String url, Map<String, String> parameters,
			ResponseType dummy);
}
