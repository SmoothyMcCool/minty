package tom.config;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PreDestroy;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class VllmConfig {

	private static final Logger logger = LogManager.getLogger(VllmConfig.class);

	MintyConfigurationImpl properties;

	public VllmConfig(MintyConfigurationImpl properties) {
		this.properties = properties;
	}

	@PreDestroy
	public void shutdownReactor() {
		Schedulers.shutdownNow();
	}

	@Bean(destroyMethod = "dispose")
	ConnectionProvider vllmConnectionProvider() {
		return ConnectionProvider.builder("vllmPool").maxConnections(50).pendingAcquireMaxCount(50).build();
	}

	@Bean
	WebClient.Builder openAiWebClientBuilder(ConnectionProvider vllmConnectionProvider) {
		Duration responseTimeout = properties.getConfig().llm().apiTimeout();
		Duration connectTimeout = properties.getConfig().llm().apiConnectTimeout();

		// vllm required HTTP/1.1
		HttpClient httpClient = HttpClient.create(vllmConnectionProvider).protocol(HttpProtocol.HTTP11)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
				.responseTimeout(responseTimeout)
				.doOnConnected(conn -> conn
						.addHandlerLast(new ReadTimeoutHandler(responseTimeout.toMillis(), TimeUnit.MILLISECONDS))
						.addHandlerLast(new WriteTimeoutHandler(responseTimeout.toMillis(), TimeUnit.MILLISECONDS)));
		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)); // 16 MB
	}

	@Bean
	OpenAiApi openAiApi(WebClient.Builder openAiWebClientBuilder) {
		URI vllmUri = properties.getConfig().llm().uri();
		logger.info("vllm URI is " + vllmUri);

		ApiKey apiKey = new SimpleApiKey("doesn't matter");
		return OpenAiApi.builder().baseUrl(vllmUri.toString()).apiKey(apiKey).webClientBuilder(openAiWebClientBuilder)
				.build();
	}

}
