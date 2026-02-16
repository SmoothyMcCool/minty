package tom.prioritythreadpool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import tom.api.ConversationId;

class PriorityThreadPoolTaskExecutorTest {

	@Test
	void tasksAreExecutedByPriorityThenOldestFirst() throws InterruptedException {
		PriorityThreadPoolTaskExecutor executor = new PriorityThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.initialize();

		List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());
		CountDownLatch latch = new CountDownLatch(6);
		ConversationId cid = new ConversationId(UUID.randomUUID());

		// Simple task factory
		BiFunction<String, CountDownLatch, Runnable> makeTask = (name, l) -> () -> {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			executionOrder.add(name);
			l.countDown();
		};

		// Submit tasks in mixed order
		executor.execute(makeTask.apply("Low1", latch), cid, TaskPriority.Low);
		executor.execute(makeTask.apply("High1", latch), cid, TaskPriority.High);
		executor.execute(makeTask.apply("Medium1", latch), cid, TaskPriority.Medium);
		executor.execute(makeTask.apply("High2", latch), cid, TaskPriority.High);
		executor.execute(makeTask.apply("Low2", latch), cid, TaskPriority.Low);
		executor.execute(makeTask.apply("Medium2", latch), cid, TaskPriority.Medium);

		// Wait for all tasks to complete
		assertTrue(latch.await(2, TimeUnit.SECONDS), "Tasks did not complete in time");

		// With single-threaded executor, first submitted task runs immediately,
		// then queued tasks are executed in priority order (High → Medium → Low), FIFO
		// per priority
		List<String> expectedOrder = List.of("Low1", // ran immediately
				"High1", // highest priority in queue
				"High2", // next-highest, FIFO
				"Medium1", // next priority
				"Medium2", "Low2" // remaining low
		);

		assertEquals(expectedOrder, executionOrder, "Tasks did not execute in expected priority + FIFO order");

		executor.shutdown();
	}

}
