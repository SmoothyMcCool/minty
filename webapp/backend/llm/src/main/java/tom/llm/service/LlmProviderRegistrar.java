package tom.llm.service;

import java.util.Map;

public interface LlmProviderRegistrar {

	void registerEndpoints(Map<String, LlmEndpointService> endpointServices, Map<String, String> modelToEndpoint);

}
