package tom.assistant.service.agent.worker;

import tom.assistant.service.agent.response.LlmResponse;

public class MermaidGeneratorWorker implements WorkerHandler {

	public MermaidGeneratorWorker() {
	}

	@Override
	public WorkerDecision handle(WorkerContext ctx) {

		LlmResponse parsed = (LlmResponse) ctx.state.get("diagram_parser");

		if (parsed == null) {
			return WorkerDecision.error("Missing parsed diagram");
		}

		switch (parsed.getStatus()) {

		case SUCCESS -> {
			return WorkerDecision.llmCall("mermaid_generate");
		}

		case NEED_INFO -> {
			// Pass structured info upward, don't directly ask
			return WorkerDecision.needInfo(parsed);
		}

		case ERROR -> {
			return WorkerDecision.error("Failed to parse diagram: " + parsed.getMessage());
		}

		default -> {
			return WorkerDecision.error("Unknown LLM status");
		}
		}
	}

}