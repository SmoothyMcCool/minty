package tom.assistant.service.agent.worker;

public interface WorkerHandler {
	WorkerDecision handle(WorkerContext ctx);
}
