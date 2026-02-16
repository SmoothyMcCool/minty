package tom.config;

import java.net.URI;
import java.time.Duration;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PreDestroy;
import reactor.core.scheduler.Schedulers;

@Configuration
public class OllamaConfig {

	private static final Logger logger = LogManager.getLogger(OllamaConfig.class);

	MintyConfigurationImpl properties;

	public OllamaConfig(MintyConfigurationImpl properties) {
		this.properties = properties;
	}

	@PreDestroy
	public void shutdownReactor() {
		Schedulers.shutdownNow();
	}

	@Bean(destroyMethod = "close")
	HttpClientConnectionManager ollamaConnectionManager() {
		Duration ollamaTimeout = properties.getConfig().llm().apiTimeout();
		Duration ollamaConnectTimeout = properties.getConfig().llm().apiConnectTimeout();

		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setConnectTimeout(Timeout.of(ollamaConnectTimeout)).build();
		SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(Timeout.of(ollamaTimeout)).build();

		return PoolingHttpClientConnectionManagerBuilder.create().setDefaultConnectionConfig(connectionConfig)
				.setDefaultSocketConfig(socketConfig).setMaxConnTotal(50).setMaxConnPerRoute(50).build();
	}

	@Bean(destroyMethod = "close")
	CloseableHttpClient ollamaHttpClient(HttpClientConnectionManager ollamaConnectionManager) {
		Duration ollamaTimeout = properties.getConfig().llm().apiTimeout();
		return HttpClients.custom().setConnectionManager(ollamaConnectionManager)
				.setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.of(ollamaTimeout)).build())
				.disableAutomaticRetries().build();
	}

	@Bean
	OllamaApi ollamaApi(CloseableHttpClient ollamaHttpClient) {
		URI ollamaUri = properties.getConfig().llm().uri();
		logger.info("ollama URI is " + ollamaUri);

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				ollamaHttpClient);

		RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);

		return OllamaApi.builder().baseUrl(ollamaUri.toString()).restClientBuilder(restClientBuilder).build();
	}

}
