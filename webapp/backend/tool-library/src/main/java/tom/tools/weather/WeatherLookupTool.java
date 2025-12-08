package tom.tools.weather;

import java.io.IOException;
import java.net.URI;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import tom.http.service.HttpResponseHandler;
import tom.tool.MintyTool;

public class WeatherLookupTool implements MintyTool {

	private static final Logger logger = LogManager.getLogger(WeatherLookupTool.class);

	@Tool(description = "Get the current weather for a city by specifying its latitude and longitude")
	String getCurrentWeather(@ToolParam() double latitude, @ToolParam() double longitude) {

		String forecast = getForecast(latitude, longitude);
		return forecast;
	}

	private String getForecast(double lat, double lon) {
		URI url = URI.create("https://weather.gc.ca/api/app/v3/en/Location/" + lat + "," + lon + "?type-city");

		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

			final HttpGet request = new HttpGet(url);

			String dummy = "";
			String response = client.execute(request, new HttpResponseHandler<>(dummy));
			return response;

		} catch (IOException e) {
			logger.error("get: failed to execute request ", e);
		}

		return null;
	}

	@Override
	public String name() {
		return "Get Weather Forecast";
	}

	@Override
	public String description() {
		return "Returns the weather forecast for a given latitude and longitude, from weather.gc.ca.";
	}
}
