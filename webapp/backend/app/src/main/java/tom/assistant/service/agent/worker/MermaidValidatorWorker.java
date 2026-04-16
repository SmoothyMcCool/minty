package tom.assistant.service.agent.worker;

import java.util.Optional;

import tom.Pair;
import tom.assistant.service.agent.model.AgentStep;
import tom.assistant.service.agent.model.AgentStepState;

public class MermaidValidatorWorker implements WorkerHandler {

	public MermaidValidatorWorker() {
	}

	@Override
	public WorkerDecision handle(WorkerContext ctx) {
		Optional<Pair<AgentStep, AgentStepState>> step = ctx.state.findLastCompletedStep();

		if (step.isEmpty()) {
			return WorkerDecision.error("Missing generated diagram");
		}

		Pair<AgentStep, AgentStepState> state = step.get();

		String raw = state.right().getResponse().getRawText();
		if (raw == null || raw.isBlank()) {
			return WorkerDecision.error("Missing generated diagram");
		}

		return WorkerDecision.llmCall("mermaid_validator");
	}

}