package tom.config.model;

import java.net.URI;
import java.time.Duration;

public record EndpointConfig(String name, ProviderType provider, URI url, Duration apiConnectionTimeout,
		String apiKeyName, boolean requiresKey, Duration apiTimeout) {

}
