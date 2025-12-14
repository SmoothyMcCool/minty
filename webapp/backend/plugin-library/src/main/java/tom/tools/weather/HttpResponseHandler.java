package tom.tools.weather;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpResponseHandler<T> implements HttpClientResponseHandler<T> {

	private T type;

	public HttpResponseHandler(T type) {
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
		int status = response.getCode();

		if (status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION) {
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				return null;
			}

			if (type instanceof String) {
				return (T) new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
			}

			ObjectMapper mapper = new ObjectMapper();
			T result = (T) mapper.readValue(entity.getContent(), type.getClass());
			return result;
		}

		return null;
	}

}
