package tom.tasks.flowcontrol.grouping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.tasks.noop.NullTaskConfig;

/**
 * Unit tests for {@link GroupBy} using JUnit 5 and Mockito.
 */
@ExtendWith(MockitoExtension.class)
class GroupByTest {

	@Mock
	private OutputPort output1;

	@Mock
	private OutputPort output2;

	@Mock
	private TaskLogger logger;

	private GroupBy groupBy;

	@BeforeEach
	void setUp() {
		groupBy = new GroupBy();
		groupBy.setLogger(logger);
		groupBy.setOutputConnectors(List.of(output1, output2));
	}

	/* --------------------------------------------------------------------- */
	/* Specification tests */
	/* --------------------------------------------------------------------- */

	@Test
	void getSpecification_returnsCorrectValues() {
		TaskSpec spec = groupBy.getSpecification();

		assertEquals(2, spec.numInputs(), "Number of inputs");
		assertEquals(1, spec.numOutputs(), "Number of outputs");
		assertTrue(spec.expects().contains("One packet on input 1"), "expects() description");
		assertTrue(spec.produces().contains("For each input on port 2"), "produces() description");
		assertEquals("GroupBy", spec.taskName(), "taskName");
		assertEquals("Flow Control", spec.group(), "group");
		assertNotNull(spec.taskConfiguration(), "taskConfiguration()");
		assertNotNull(spec.taskConfiguration(Map.of()), "taskConfiguration(Map)");
		assertEquals(NullTaskConfig.class, spec.taskConfiguration().getClass(), "taskConfiguration() instance type");
	}

	/* --------------------------------------------------------------------- */
	/* Input handling tests */
	/* --------------------------------------------------------------------- */

	@Test
	void wantsInput_keyPacketNull_returnsTrueAndReadyToRunFalse() {
		Packet dummy = new Packet();
		dummy.setId("ignored");

		boolean result = groupBy.wantsInput(0, dummy);

		assertTrue(result, "should accept first packet");
		assertFalse(groupBy.readyToRun(), "readyToRun should be false");
	}

	@Test
	void wantsInput_keyPacketSetIdMatches_returnsTrueAndReadyToRunFalse() {
		Packet key = new Packet();
		key.setId("K1");
		groupBy.giveInput(0, key); // set key

		Packet data = new Packet();
		data.setId("K1");

		boolean result = groupBy.wantsInput(1, data);

		assertTrue(result, "should accept matching packet");
		assertFalse(groupBy.readyToRun(), "readyToRun should still be false");
	}

	@Test
	void wantsInput_keyPacketSetIdNotMatches_returnsFalseAndReadyToRunTrue() {
		Packet key = new Packet();
		key.setId("K1");
		groupBy.giveInput(0, key); // set key

		Packet data = new Packet();
		data.setId("K2");

		boolean result = groupBy.wantsInput(1, data);

		assertFalse(result, "should reject non‑matching packet");
		assertTrue(groupBy.readyToRun(), "readyToRun should be true");
	}

	@Test
	void giveInput_input0_setsKeyAndReturnsTrue() {
		Packet key = new Packet();
		key.setId("K1");

		boolean result = groupBy.giveInput(0, key);

		assertTrue(result, "giveInput should return true for first key");
		verify(logger).debug("GroupBy: Setting key with Id K1");
	}

	@Test
	void giveInput_input0_whenKeyAlreadySet_throwsException() {
		Packet key1 = new Packet();
		key1.setId("K1");
		groupBy.giveInput(0, key1);

		Packet key2 = new Packet();
		key2.setId("K2");

		RuntimeException ex = assertThrows(RuntimeException.class, () -> groupBy.giveInput(0, key2));
		assertTrue(ex.getMessage().contains("key Packet already received"));
	}

	@Test
	void giveInput_input1_aggregatesDataAndTextAndReturnsFalse() {
		// Setup key
		Packet key = new Packet();
		key.setId("K1");
		groupBy.giveInput(0, key);

		// Data packet
		Packet data = new Packet();
		data.setId("K1");
		data.addText("dataText");
		Map<String, Object> map = new HashMap<>();
		map.put("field", "value");
		data.addData(map);

		boolean result = groupBy.giveInput(1, data);

		assertFalse(result, "giveInput on data input should return false");
		verify(logger).debug("GroupBy: Adding packet with Id K1");

		// Capture the packet that will be written by run()
		ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
		groupBy.inputTerminated(1); // mark input 2 finished
		groupBy.run(); // write to outputs
		verify(output1).write(captor.capture());
		Packet written = captor.getValue();

		// The result packet should contain the data list and text from the data packet
		assertEquals(1, written.getData().size(), "dataList size");
		assertEquals("value", written.getData().get(0).get("field"));
		assertEquals(1, written.getText().size(), "text size");
		assertEquals("dataText", written.getText().get(0));
	}

	@Test
	void giveInput_invalidInputNumber_throwsExceptionAndSetsFailed() {
		RuntimeException ex = assertThrows(RuntimeException.class, () -> groupBy.giveInput(2, new Packet()));
		assertTrue(ex.getMessage().contains("Workflow misconfiguration"));
		assertTrue(groupBy.failed(), "failed flag should be true");
	}

	/* --------------------------------------------------------------------- */
	/* Lifecycle tests */
	/* --------------------------------------------------------------------- */

	@Test
	void inputTerminated_setsReadyToRunOnlyWhenKeyPresent() {
		// No key yet
		groupBy.inputTerminated(1);
		assertFalse(groupBy.readyToRun(), "readyToRun should remain false");

		// Set key
		Packet key = new Packet();
		key.setId("K1");
		groupBy.giveInput(0, key);

		groupBy.inputTerminated(1);
		assertTrue(groupBy.readyToRun(), "readyToRun should be true now");
	}

	@Test
	void run_writesResultToAllOutputs_andLogsMessage() {
		// Setup key and data
		Packet key = new Packet();
		key.setId("K1");
		groupBy.giveInput(0, key);

		Packet data = new Packet();
		data.setId("K1");
		data.addText("t1");
		Map<String, Object> map = new HashMap<>();
		map.put("a", 1);
		data.addData(map);
		groupBy.giveInput(1, data);

		groupBy.inputTerminated(1); // mark finished
		groupBy.run();

		ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
		verify(output1).write(captor.capture());
		verify(output2).write(captor.capture());

		// Both outputs should receive the same packet
		Packet out1 = captor.getAllValues().get(0);
		Packet out2 = captor.getAllValues().get(1);
		assertEquals(out1.getText(), out2.getText(), "text lists should match");
		assertEquals(out1.getData(), out2.getData(), "data lists should match");

		// Verify debug logging
		verify(logger, times(2)).debug("GroupBy: Writing out items for key K1");
	}

	/* --------------------------------------------------------------------- */
	/* Miscellaneous tests */
	/* --------------------------------------------------------------------- */

	@Test
	void getResult_and_getError_returnNull() {
		assertNull(groupBy.getResult(), "getResult should be null");
		assertNull(groupBy.getError(), "getError should be null");
	}
}
