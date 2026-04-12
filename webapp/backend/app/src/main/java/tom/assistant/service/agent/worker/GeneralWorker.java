package tom.assistant.service.agent.worker;

public class GeneralWorker implements WorkerHandler {

	public GeneralWorker() {
	}

	@Override
	public WorkerDecision handle(WorkerContext ctx) {
		return WorkerDecision.llmCall("general");
	}

}
