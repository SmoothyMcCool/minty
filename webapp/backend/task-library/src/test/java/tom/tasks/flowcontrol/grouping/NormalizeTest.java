package tom.tasks.flowcontrol.grouping;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.TaskLogger;
import tom.task.TaskSpec;

/**
 * Unit tests for {@link Normalize}.
 */
@ExtendWith(MockitoExtension.class)
class NormalizeTest {

	@Mock
	private OutputPort mockOutput;

	@Mock
	private TaskLogger mockLogger;

	private Normalize normalize;

	@BeforeEach
	void setUp() {
		normalize = new Normalize();
		normalize.setLogger(mockLogger);
		normalize.setOutputConnectors(Collections.singletonList(mockOutput));
	}

	/* ------------------------------------------------------------------ */
	/* 1. Basic behaviour (ready, giveInput, getResult / getError) */
	/* ------------------------------------------------------------------ */

	@Test
	@DisplayName("readyToRun() is false until input is given")
	void testReadyToRunInitiallyFalse() {
		assertFalse(normalize.readyToRun(), "Task should not be ready before input");
	}

	@Test
	@DisplayName("giveInput() stores packet and logs info")
	void testGiveInputStoresPacketAndLogs() {
		Packet packet = new Packet();
		packet.setId("42");
		packet.addText("hello");
		packet.addData(Collections.singletonMap("key", "value"));

		boolean result = normalize.giveInput(0, packet);

		assertTrue(result, "giveInput should return true");
		assertTrue(normalize.readyToRun(), "Task should now be ready");
	}

	@Test
	@DisplayName("getResult() and getError() return null")
	void testGetResultAndError() {
		assertNull(normalize.getResult(), "Result should be null");
		assertNull(normalize.getError(), "Error should be null");
	}

	@Test
	@DisplayName("failed() always returns false")
	void testFailedFlag() {
		assertFalse(normalize.failed(), "Task should never be marked as failed");
	}

	@Test
	@DisplayName("inputTerminated() does nothing and does not throw")
	void testInputTerminatedDoesNothing() {
		assertDoesNotThrow(() -> normalize.inputTerminated(0));
	}

	/* ------------------------------------------------------------------ */
	/* 2. run() – data vs text length */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("run() writes correct packets")
	class RunWritesPackets {

		private ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);

		private void runWithInput(Packet packet) {
			normalize.giveInput(0, packet);
			normalize.run();
		}

		@Test
		@DisplayName("data longer than text – data-only packets after first")
		void dataLongerThanText() {
			Packet input = new Packet();
			input.setId("id-1");
			input.addData(Map.of("a", 1));
			input.addData(Map.of("b", 2));
			input.addData(Map.of("c", 3));
			input.addText("only-one");

			runWithInput(input);

			verify(mockOutput, times(3)).write(packetCaptor.capture());
			List<Packet> written = packetCaptor.getAllValues();

			assertEquals(3, written.size());

			// first packet
			Packet p0 = written.get(0);
			assertEquals("id-1", p0.getId());
			assertEquals(1, p0.getDataList().size());
			assertEquals(1, p0.getText().size());

			// second packet – no text
			Packet p1 = written.get(1);
			assertEquals("id-1", p1.getId());
			assertEquals(1, p1.getDataList().size());
			assertTrue(p1.getText().isEmpty());

			// third packet – no text
			Packet p2 = written.get(2);
			assertEquals("id-1", p2.getId());
			assertEquals(1, p2.getDataList().size());
			assertTrue(p2.getText().isEmpty());
		}

		@Test
		@DisplayName("text longer than data – text-only packets after first")
		void textLongerThanData() {
			Packet input = new Packet();
			input.setId("id-2");
			input.addData(Map.of("x", 10));
			input.addText("first");
			input.addText("second");
			input.addText("third");

			runWithInput(input);

			verify(mockOutput, times(3)).write(packetCaptor.capture());
			List<Packet> written = packetCaptor.getAllValues();

			assertEquals(3, written.size());

			// first packet
			Packet p0 = written.get(0);
			assertEquals("id-2", p0.getId());
			assertEquals(1, p0.getDataList().size());
			assertEquals(1, p0.getText().size());

			// second packet – no data
			Packet p1 = written.get(1);
			assertEquals("id-2", p1.getId());
			assertTrue(p1.getDataList().isEmpty());
			assertEquals(1, p1.getText().size());

			// third packet – no data
			Packet p2 = written.get(2);
			assertEquals("id-2", p2.getId());
			assertTrue(p2.getDataList().isEmpty());
			assertEquals(1, p2.getText().size());
		}

		@Test
		@DisplayName("data and text equal – each packet has one data & one text")
		void dataEqualText() {
			Packet input = new Packet();
			input.setId("id-3");
			input.addData(Map.of("d1", "v1"));
			input.addData(Map.of("d2", "v2"));
			input.addText("t1");
			input.addText("t2");

			runWithInput(input);

			verify(mockOutput, times(2)).write(packetCaptor.capture());
			List<Packet> written = packetCaptor.getAllValues();

			assertEquals(2, written.size());

			Packet p0 = written.get(0);
			assertEquals("id-3", p0.getId());
			assertEquals(1, p0.getDataList().size());
			assertEquals(1, p0.getText().size());

			Packet p1 = written.get(1);
			assertEquals("id-3", p1.getId());
			assertEquals(1, p1.getDataList().size());
			assertEquals(1, p1.getText().size());
		}
	}

	/* ------------------------------------------------------------------ */
	/* 3. Multiple outputs – each receives the same packets */
	/* ------------------------------------------------------------------ */

	@Test
	@DisplayName("run() writes to all configured outputs")
	void testRunWithMultipleOutputs() {
		OutputPort output2 = mock(OutputPort.class);
		normalize.setOutputConnectors(Arrays.asList(mockOutput, output2));

		Packet input = new Packet();
		input.setId("multi");
		input.addData(Map.of("k", "v"));
		input.addText("text");

		normalize.giveInput(0, input);
		normalize.run();

		verify(mockOutput, times(1)).write(any(Packet.class));
		verify(output2, times(1)).write(any(Packet.class));
	}

	/* ------------------------------------------------------------------ */
	/* 4. Specification sanity checks */
	/* ------------------------------------------------------------------ */

	@Test
	@DisplayName("TaskSpec provides correct metadata")
	void testSpecification() {
		TaskSpec spec = normalize.getSpecification();
		assertEquals("Normalizer", spec.taskName());
		assertEquals("Flow Control", spec.group());
		assertEquals(1, spec.numInputs());
		assertEquals(1, spec.numOutputs());
		assertEquals("Any data packet.", spec.expects());
		assertEquals(
				"If the packet data contains a list of objects, it is sent out as a sequence of packets, each containing one element of the list.",
				spec.produces());
		assertNotNull(spec.taskConfiguration());
	}

	/* ------------------------------------------------------------------ */
	/* 5. Edge case: no outputs configured */
	/* ------------------------------------------------------------------ */

	@Test
	@DisplayName("run() does nothing when no outputs are configured")
	void testRunWithNoOutputs() {
		normalize.setOutputConnectors(Collections.emptyList());

		Packet input = new Packet();
		input.setId("none");
		input.addText("only");

		normalize.giveInput(0, input);
		assertDoesNotThrow(() -> normalize.run());
	}

}
