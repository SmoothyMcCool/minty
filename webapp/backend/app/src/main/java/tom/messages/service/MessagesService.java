package tom.messages.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class MessagesService {

	private final List<String> messages;
	private final String motd;

	public MessagesService() {
		try {
			String path = System.getProperty("catalina.base") + "/conf/Minty/did-you-know.txt";

			Resource resource = new FileSystemResource(path);
			if (!resource.exists()) {
				throw new RuntimeException("Properties file not found at: " + path);
			}

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

				messages = reader.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
			}

		} catch (IOException e) {
			throw new IllegalStateException("Unable to load did-you-know.txt", e);
		}

		try {
			String path = System.getProperty("catalina.base") + "/conf/Minty/motd.txt";

			Resource resource = new FileSystemResource(path);
			if (!resource.exists()) {
				throw new RuntimeException("Properties file not found at: " + path);
			}

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

				motd = reader.lines().map(String::trim).filter(line -> !line.isEmpty()).findFirst().orElse("");
			}

		} catch (IOException e) {
			throw new IllegalStateException("Unable to load motd.txt", e);
		}

	}

	public List<String> getMessages() {
		return messages;
	}

	public String getRandomMessage() {
		if (messages.isEmpty()) {
			return "";
		}

		return messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
	}

	public String getMessageOfTheDay() {
		return motd;
	}
}
