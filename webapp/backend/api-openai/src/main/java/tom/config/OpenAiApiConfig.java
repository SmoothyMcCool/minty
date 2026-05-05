package tom.config;

import java.net.URI;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;

import jakarta.annotation.PreDestroy;
import reactor.core.scheduler.Schedulers;

@Configuration
public class OpenAiApiConfig {

	private static final Logger logger = LogManager.getLogger(OpenAiApiConfig.class);

	@PreDestroy
	public void shutdownReactor() {
		Schedulers.shutdownNow();
	}

	@Bean
	OpenAIClient openAiClient(MintyConfiguration properties) {
		URI openAiApiUri = properties.getConfig().llm().uri();
		Duration responseTimeout = properties.getConfig().llm().apiTimeout();

		logger.info("Sync: OpenAI API URI is " + openAiApiUri);

		return OpenAIOkHttpClient.builder().baseUrl(openAiApiUri.toString())
				.apiKey(properties.getConfig().llm().openaiApiKey()).timeout(responseTimeout).build();
	}

	@Bean
	OpenAIClientAsync openAiClientAsync(MintyConfiguration properties) {
		URI openAiApiUri = properties.getConfig().llm().uri();
		Duration responseTimeout = properties.getConfig().llm().apiTimeout();

		logger.info("Async: OpenAI API URI is " + openAiApiUri);

		return OpenAIOkHttpClientAsync.builder().baseUrl(openAiApiUri.toString())
				.apiKey(properties.getConfig().llm().openaiApiKey()).timeout(responseTimeout).build();
	}

}
