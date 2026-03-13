package tom.tasks.transform.formattext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tom.api.task.Packet;

public class TemplateRendererTest {

	private Packet packet;
	private TemplateRenderer renderer;

	@BeforeEach
	void setup() {

		renderer = new TemplateRenderer();

		packet = new Packet();
		packet.setId("a");

		Map<String, Object> record = new HashMap<>();
		record.put("name", "asdf");

		List<Map<String, Object>> comments = new ArrayList<>();

		Map<String, Object> c1 = new HashMap<>();
		c1.put("from", "alice");
		c1.put("comment", "this is comment 1");

		Map<String, Object> c2 = new HashMap<>();
		c2.put("from", "bob");
		c2.put("comment", "this is comment 2");

		comments.add(c1);
		comments.add(c2);

		record.put("comments", comments);

		packet.addData(record);
	}

	@Test
	void testSimpleFieldReplacement() {

		String template = "ID = {id}";

		List<String> result = renderer.render(packet, template);

		assertEquals(1, result.size());
		assertEquals("ID = a", result.get(0));
	}

	@Test
	void testNestedPathResolution() {

		String template = "Name: {data[0].name}";

		List<String> result = renderer.render(packet, template);

		assertEquals("Name: asdf", result.get(0));
	}

	@Test
	void testLoopExpansion() {

		String template = """
				{#data[0].comments}
				{from}: {comment}
				{/data[0].comments}
				""";

		List<String> result = renderer.render(packet, template);

		assertEquals(2, result.size());

		assertEquals("alice: this is comment 1", result.get(0).trim());
		assertEquals("bob: this is comment 2", result.get(1).trim());
	}

	@Test
	void testLoopWithHeader() {

		String template = """
				Comments:
				{#data[0].comments}
				- {from}
				{/data[0].comments}
				""";

		List<String> result = renderer.render(packet, template);

		assertEquals("Comments:", result.get(0));
		assertEquals("- alice", result.get(1).trim());
		assertEquals("- bob", result.get(2).trim());
	}

	@Test
	void testNestedLoops() {

		Map<String, Object> reply = new HashMap<>();
		reply.put("from", "charlie");
		reply.put("comment", "nested reply");

		List<Map<String, Object>> replies = new ArrayList<>();
		replies.add(reply);

		((Map<String, Object>) packet.getData().get(0)).put("replies", replies);

		String template = """
				{#data}
				Post: {name}
				{#replies}
				reply from {from}
				{/replies}
				{/data}
				""";

		List<String> result = renderer.render(packet, template);

		assertTrue(result.stream().anyMatch(s -> s.contains("Post: asdf")));
		assertTrue(result.stream().anyMatch(s -> s.contains("reply from charlie")));
	}

	@Test
	void testMissingFieldPreserved() {

		String template = "Unknown: {data[0].missing}";

		List<String> result = renderer.render(packet, template);

		assertEquals("Unknown: {data[0].missing}", result.get(0));
	}

	@Test
	void testMultipleFieldsSameLine() {

		String template = "{data[0].name} has id {id}";

		List<String> result = renderer.render(packet, template);

		assertEquals("asdf has id a", result.get(0));
	}

	@Test
	void testEmptyLoopProducesNothing() {

		String template = """
				{#data[0].nonexistent}
				should not appear
				{/data[0].nonexistent}
				""";

		List<String> result = renderer.render(packet, template);

		assertTrue(result.isEmpty());
	}

}
