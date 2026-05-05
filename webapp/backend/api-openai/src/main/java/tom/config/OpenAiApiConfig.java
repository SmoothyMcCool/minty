package tom.config;

import java.net.URI;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import jakarta.annotation.PreDestroy;
import reactor.core.scheduler.Schedulers;

@Configuration
public class OpenAiApiConfig {

	private static final Logger logger = LogManager.getLogger(OpenAiApiConfig.class);

	MintyConfigurationImpl properties;

	public OpenAiApiConfig(MintyConfigurationImpl properties) {
		this.properties = properties;
	}

	@PreDestroy
	public void shutdownReactor() {
		Schedulers.shutdownNow();
	}

	@Bean
	OpenAIClient openAiClient() {
		URI openAiApiUri = properties.getConfig().llm().uri();
		Duration responseTimeout = properties.getConfig().llm().apiTimeout();

		logger.info("OpenAI API URI is " + openAiApiUri);

		return OpenAIOkHttpClient.builder().baseUrl(openAiApiUri.toString()).apiKey("doesn't matter")
				.timeout(responseTimeout).build();
	}

}
