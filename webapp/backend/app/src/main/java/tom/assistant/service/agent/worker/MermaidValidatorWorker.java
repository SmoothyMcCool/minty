package tom.assistant.service.agent.worker;

import tom.assistant.service.agent.response.LlmResponse;

public class MermaidValidatorWorker implements WorkerHandler {

	public MermaidValidatorWorker() {
	}

	@Override
	public WorkerDecision handle(WorkerContext ctx) {
		LlmResponse generated = (LlmResponse) ctx.state.get("mermaid_generate");

		if (generated == null || generated.getRawText() == null || generated.getRawText().isBlank()) {
			return WorkerDecision.error("Missing generated diagram");
		}

		return WorkerDecision.llmCall("mermaid_validate");
	}

}