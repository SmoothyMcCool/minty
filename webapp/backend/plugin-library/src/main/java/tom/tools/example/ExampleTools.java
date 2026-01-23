package tom.tools.example;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.ToolParam;

import tom.api.tool.MintyTool;
import tom.api.tool.MintyToolResponse;

public class ExampleTools implements MintyTool {

	private static final Logger logger = LogManager.getLogger(ExampleTools.class);

	// @Tool(name = "get_current_local_time", description = "Get the current local
	// time")
	MintyToolResponse<String> getCurrentLocalTime() {
		String result = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
		return MintyToolResponse.SuccessResponse(result);
	}

	// @Tool(name = "get_weather", description = "Get the current weather for a city
	// by specifying its latitude and longitude")
	MintyToolResponse<String> getCurrentWeather(@ToolParam() double latitude, @ToolParam() double longitude) {

		String forecast = getForecast(latitude, longitude);
		return MintyToolResponse.SuccessResponse(forecast);
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
		return "Example Weather and Time Tools";
	}

	@Override
	public String description() {
		return "Example tools just for fun, to return weather forecasts for a location, and to get the current local time.";
	}

}
