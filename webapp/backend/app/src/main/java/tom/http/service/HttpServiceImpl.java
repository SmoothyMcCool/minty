package tom.http.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.api.services.HttpService;
import tom.user.repository.User;
import tom.user.repository.UserRepository;
import tom.user.service.UserServiceInternal;

@Service
public class HttpServiceImpl implements HttpService {

	private static final Logger logger = LogManager.getLogger(HttpServiceImpl.class);

	private final UserServiceInternal userService;
	private final UserRepository userRepository;

	public HttpServiceImpl(UserServiceInternal userService, UserRepository userRepository) {
		this.userService = userService;
		this.userRepository = userRepository;
	}

	@Override
	public <ResponseType> ResponseType getNoAuth(String url, Map<String, String> parameters, ResponseType dummy) {
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

			final HttpGet request = new HttpGet(url);

			if (parameters != null) {
				URIBuilder uriBuilder = new URIBuilder(request.getUri());
				parameters.forEach((key, value) -> {
					uriBuilder.addParameter(key, value);
				});
				request.setUri(uriBuilder.build());
			}

			ResponseType response = client.execute(request, new HttpResponseHandler<ResponseType>(dummy));
			return response;

		} catch (URISyntaxException e) {
			logger.error("get: Invalid URL: " + url);
		} catch (IOException e) {
			logger.error("get: failed to execute request ", e);
		}

		return null;
	}

	@Override
	public <ResponseType> ResponseType getBasicAuth(UUID userId, String url, Map<String, String> parameters,
			ResponseType dummy) {

		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

			// Get credentials and create the Basic auth header
			User user = userService.decrypt(userRepository.findById(userId).get());
			final String auth = ""; // TODO construct an auth string from some credentials somewhere.
			final byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
			final String authHeader = "Basic " + new String(encodedAuth);

			final HttpGet request = new HttpGet(url);
			request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

			if (parameters != null) {
				URIBuilder uriBuilder = new URIBuilder(request.getUri());
				parameters.forEach((key, value) -> {
					uriBuilder.addParameter(key, value);
				});
				request.setUri(uriBuilder.build());
			}

			ResponseType response = client.execute(request, new HttpResponseHandler<ResponseType>(dummy));
			return response;

		} catch (JsonProcessingException e) {
			logger.error("get: Failed to get User object for userId " + userId);
		} catch (URISyntaxException e) {
			logger.error("get: Invalid URL: " + url);
		} catch (IOException e) {
			logger.error("get: failed to execute request ", e);
		}

		return null;
	}

	@Override
	public <ResponseType> ResponseType getJavaEEFormAuth(UUID userId, String url, Map<String, String> parameters,
			ResponseType dummy) {

		// TODO
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

			// Get credentials and create the Basic auth header
			User user = userService.decrypt(userRepository.findById(userId).get());
			final String auth = ""; // TODO construct an auth string from some credentials somewhere.
			final byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
			final String authHeader = "Basic " + new String(encodedAuth);

			final HttpGet request = new HttpGet(url);
			request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

			if (parameters != null) {
				URIBuilder uriBuilder = new URIBuilder(request.getUri());
				parameters.forEach((key, value) -> {
					uriBuilder.addParameter(key, value);
				});
				request.setUri(uriBuilder.build());
			}

			ResponseType response = client.execute(request, new HttpResponseHandler<ResponseType>(dummy));

			return response;

		} catch (JsonProcessingException e) {
			logger.error("get: Failed to get User object for userId " + userId);
		} catch (URISyntaxException e) {
			logger.error("get: Invalid URL: " + url);
		} catch (IOException e) {
			logger.error("get: failed to execute request ", e);
		}

		return null;
	}
}
