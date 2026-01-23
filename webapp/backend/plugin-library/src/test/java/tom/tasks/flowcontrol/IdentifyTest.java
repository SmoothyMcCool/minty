package tom.tasks.flowcontrol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;

/**
 * JUnit‑5 test‑suite for {@link Identify}. All external collaborators are
 * mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class IdentifyTest {

	/*
	 * ----------------------------------------------------- * Mocks
	 * -----------------------------------------------------
	 */
	@Mock
	private IdentifyConfig config;
	@Mock
	private TaskLogger logger;
	@Mock
	private OutputPort outputPort1;
	@Mock
	private OutputPort outputPort2;

	/*
	 * ----------------------------------------------------- * Helper: create a
	 * packet that contains a single map
	 * -----------------------------------------------------
	 */
	private static Packet createPacketWithData(Map<String, Object> dataMap) {
		Packet p = new Packet();
		p.addData(dataMap);
		return p;
	}

	/*
	 * ----------------------------------------------------- * Test: simple key
	 * extraction -----------------------------------------------------
	 */
	@Test
	void run_setsIdAndWritesToAllOutputs_whenKeyExists() {
		// Arrange
		when(config.getIdElement()).thenReturn("name");
		Identify id = new Identify(config);
		id.setLogger(logger);
		id.setOutputConnectors(List.of(outputPort1, outputPort2));

		Map<String, Object> data = Map.of("name", "Alice");
		Packet packet = createPacketWithData(data);
		id.giveInput(0, packet);

		// Act
		id.run();

		// Assert
		assertEquals("Alice", packet.getId());
		verify(outputPort1).write(packet);
		verify(outputPort2).write(packet);
		verify(logger, never()).warn(anyString());
	}

	/*
	 * ----------------------------------------------------- * Test: nested key
	 * extraction (user.id) -----------------------------------------------------
	 */
	@Test
	void run_setsIdFromNestedMap_whenKeyExists() {
		when(config.getIdElement()).thenReturn("user.id");
		Identify id = new Identify(config);
		id.setLogger(logger);
		id.setOutputConnectors(List.of(outputPort1));

		Map<String, Object> user = Map.of("id", 42);
		Map<String, Object> data = Map.of("user", user);
		Packet packet = createPacketWithData(data);
		id.giveInput(0, packet);

		id.run();

		assertEquals("42", packet.getId()); // int -> string
		verify(outputPort1).write(packet);
		verify(logger, never()).warn(anyString());
	}

	/*
	 * ----------------------------------------------------- * Test: list index
	 * extraction (users.0.id) -----------------------------------------------------
	 */
	@Test
	void run_setsIdFromListIndex_whenKeyExists() {
		when(config.getIdElement()).thenReturn("users.0.id");
		Identify id = new Identify(config);
		id.setLogger(logger);
		id.setOutputConnectors(List.of(outputPort1));

		Map<String, Object> entry = Map.of("id", "bob");
		List<Map<String, Object>> users = List.of(entry);
		Map<String, Object> data = Map.of("users", users);
		Packet packet = createPacketWithData(data);
		id.giveInput(0, packet);

		id.run();

		assertEquals("bob", packet.getId());
		verify(outputPort1).write(packet);
		verify(logger, never()).warn(anyString());
	}

	/*
	 * ----------------------------------------------------- * Test: missing key →
	 * warning logged, id unchanged
	 * -----------------------------------------------------
	 */
	@Test
	void run_logsWarning_whenKeyMissing() {
		when(config.getIdElement()).thenReturn("missing");
		Identify id = new Identify(config);
		id.setLogger(logger);
		id.setOutputConnectors(List.of(outputPort1));

		Map<String, Object> data = Map.of("foo", "bar");
		Packet packet = createPacketWithData(data);
		id.giveInput(0, packet);

		id.run();

		assertEquals("", packet.getId()); // id stays empty
		verify(logger).warn(contains("Identify: could not find the ID key"));
		verify(outputPort1).write(packet); // still forwarded
	}

	/*
	 * ----------------------------------------------------- * Test: readyToRun &
	 * giveInput -----------------------------------------------------
	 */
	@Test
	void readyToRun_returnsTrueOnlyAfterInputIsGiven() {
		Identify id = new Identify(config);
		assertFalse(id.readyToRun());

		Packet packet = createPacketWithData(Map.of("x", 1));
		id.giveInput(0, packet);

		assertTrue(id.readyToRun());
	}

	@Test
	void giveInput_returnsTrueAndStoresPacket() {
		Identify id = new Identify(config);
		Packet packet = createPacketWithData(Map.of("x", 1));

		assertTrue(id.giveInput(0, packet));
	}

	/*
	 * ----------------------------------------------------- * Test: getResult,
	 * getError, failed -----------------------------------------------------
	 */
	@Test
	void getResultAndErrorReturnNull() {
		Identify id = new Identify(config);
		assertNull(id.getResult());
		assertNull(id.getError());
	}

	@Test
	void failedReturnsFalseByDefault() {
		Identify id = new Identify(config);
		assertFalse(id.failed());
	}

	/*
	 * ----------------------------------------------------- * Test: specification
	 * -----------------------------------------------------
	 */
	@Test
	void specificationHasExpectedValues() {
		Identify id = new Identify(config);
		TaskSpec spec = id.getSpecification();

		assertEquals("Identify", spec.taskName());
		assertEquals("Flow Control", spec.group());
		assertEquals(1, spec.numOutputs());
		assertEquals(1, spec.numInputs());
		// The config type is not relevant for this test
	}

	/*
	 * ----------------------------------------------------- * Test: run writes to
	 * all configured outputs -----------------------------------------------------
	 */
	@Test
	void runWritesToAllConfiguredOutputs() {
		when(config.getIdElement()).thenReturn("name");
		Identify id = new Identify(config);
		id.setLogger(logger);
		id.setOutputConnectors(List.of(outputPort1, outputPort2));

		Map<String, Object> data = Map.of("name", "Alice");
		Packet packet = createPacketWithData(data);
		id.giveInput(0, packet);

		id.run();

		verify(outputPort1).write(packet);
		verify(outputPort2).write(packet);
	}

	/*
	 * ----------------------------------------------------- * Test: run does not
	 * throw when no outputs are configured
	 * -----------------------------------------------------
	 */
	@Test
	void runDoesNotThrowWhenNoOutputs() {
		when(config.getIdElement()).thenReturn("name");
		Identify id = new Identify(config);
		id.setLogger(logger);
		id.setOutputConnectors(Collections.emptyList());

		Map<String, Object> data = Map.of("name", "Alice");
		Packet packet = createPacketWithData(data);
		id.giveInput(0, packet);

		assertDoesNotThrow(id::run);
	}
}
