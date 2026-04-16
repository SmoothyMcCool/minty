package tom.assistant.service.agent.worker;

import java.util.Optional;

import tom.Pair;
import tom.assistant.service.agent.model.AgentStep;
import tom.assistant.service.agent.model.AgentStepState;

public class MermaidGeneratorWorker implements WorkerHandler {

	public MermaidGeneratorWorker() {
	}

	@Override
	public WorkerDecision handle(WorkerContext ctx) {

		Optional<Pair<AgentStep, AgentStepState>> parsed = ctx.state.findLastCompletedStep();

		if (parsed.isEmpty()) {
			// An empty input is ok. We can still try, we might be the first step in the
			// chain.
			return WorkerDecision.llmCall("mermaid_generator");
		}

		AgentStepState stepState = parsed.get().right();
		switch (stepState.getStatus()) {

		case SUCCESS -> {
			return WorkerDecision.llmCall("mermaid_generator");
		}

		case NEED_INFO -> {
			return WorkerDecision.needInfo(stepState.getResponse());
		}

		case ERROR -> {
			return WorkerDecision.error("Failed to parse diagram: " + stepState.getResponse().getMessage());
		}

		default -> {
			return WorkerDecision.error("Unknown LLM status");
		}
		}
	}

}