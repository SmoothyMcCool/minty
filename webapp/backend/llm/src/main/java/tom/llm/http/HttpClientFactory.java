package tom.llm.http;

import java.time.Duration;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;

public final class HttpClientFactory {

	private HttpClientFactory() {
	}

	public static CloseableHttpClient build(Duration connectTimeout, Duration socketTimeout, int maxConns) {
		PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(
						ConnectionConfig.custom().setConnectTimeout(Timeout.of(connectTimeout)).build())
				.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(Timeout.of(socketTimeout)).build())
				.setMaxConnTotal(maxConns).setMaxConnPerRoute(maxConns).build();

		return HttpClients.custom().setConnectionManager(cm)
				.setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.of(socketTimeout)).build())
				.disableAutomaticRetries().build();
	}
}