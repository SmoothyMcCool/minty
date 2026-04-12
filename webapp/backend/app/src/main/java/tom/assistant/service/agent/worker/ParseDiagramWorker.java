package tom.assistant.service.agent.worker;

public class ParseDiagramWorker implements WorkerHandler {

	public ParseDiagramWorker() {
	}

	@Override
	public WorkerDecision handle(WorkerContext ctx) {
		return WorkerDecision.llmCall("diagram_parser");
	}

}
