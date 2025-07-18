package tom.http.service;

import java.io.IOException;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpResponseHandler<T> implements HttpClientResponseHandler<T> {

	private T type;

	public HttpResponseHandler(T type) {
	}

	@Override
	public T handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
		int status = response.getCode();

		if (status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION) {
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				return null;
			}

			ObjectMapper mapper = new ObjectMapper();
			@SuppressWarnings("unchecked")
			T result = (T) mapper.readValue(entity.getContent(), type.getClass());
			return result;
		}

		return null;
	}

}
