package tom.assistant.service.agent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import tom.assistant.service.agent.model.Agent;
import tom.assistant.service.agent.model.AgentInputField;
import tom.assistant.service.agent.model.AgentStep;
import tom.assistant.service.agent.model.AgentStepState;
import tom.assistant.service.agent.model.PlanState;

public class AgentPlannerPromptBuilder {

	private static final ObjectMapper Mapper = new ObjectMapper();

	private static final int MaxUseWhen = 4;
	private static final int MaxAvoidWhen = 3;
	private static final int MaxInputs = 6;
	private static final int MaxWords = 12;

	public static String buildPrompt(String prompt, Collection<Agent> agents, Collection<String> staticWorkers,
			PlanState state) {
		String dynamicAgentsBlock = "";
		if (agents != null) {
			dynamicAgentsBlock = agents.stream().map(agent -> toCompact(agent)).collect(Collectors.joining("\n\n"));
		}

		String staticWorkersBlock = "";
		if (staticWorkers != null) {
			staticWorkersBlock = String.join("\n- ", prependDash(staticWorkers));
		}

		String stateAsString = "";
		if (state != null) {
			stateAsString = buildPlanContext(state);
		}

		return prompt.replace("{{STATIC_WORKERS}}", staticWorkersBlock)
				.replace("{{DYNAMIC_AGENTS}}", dynamicAgentsBlock).replace("{{PLAN_CONTEXT_JSON}}", stateAsString);
	}

	private static String buildPlanContext(PlanState state) {

		if (state == null || state.getSteps() == null) {
			return "{}";
		}

		List<Map<String, Object>> compact = state.getSteps().stream().map(pair -> {
			AgentStep step = pair.left();
			AgentStepState stepState = pair.right();

			Map<String, Object> m = new HashMap<>();
			m.put("id", step.getId());
			m.put("name", step.getName());
			m.put("worker", step.getWorker());
			m.put("type", step.getType());
			m.put("status", stepState.getStatus());
			return m;
		}).toList();

		try {
			return Mapper.writeValueAsString(compact);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// =========================
	// COMPACT TRANSFORMATION
	// =========================

	private static String toCompact(Agent agent) {
		StringBuilder sb = new StringBuilder();

		appendLine(sb, "Agent: " + agent.getName());
		appendLine(sb, "Purpose: " + shorten(agent.getPurpose()));

		if (!isEmpty(agent.getWhenToUse())) {
			appendLine(sb, "Use when:");
			for (String rule : limit(agent.getWhenToUse(), MaxUseWhen)) {
				appendLine(sb, "- " + shorten(rule));
			}
		}

		if (!isEmpty(agent.getWhenNotToUse())) {
			appendLine(sb, "Avoid when:");
			for (String rule : limit(agent.getWhenNotToUse(), MaxAvoidWhen)) {
				appendLine(sb, "- " + shorten(rule));
			}
		}

		if (agent.getInputs() != null && !agent.getInputs().properties.isEmpty()) {
			List<AgentInputField> sortedInputs = sortInputs(agent.getInputs());

			List<AgentInputField> runtimeInputs = sortedInputs.stream()
					.filter(f -> f.getSource().name().equals("runtime")).collect(Collectors.toList());

			List<AgentInputField> plannerInputs = sortedInputs.stream()
					.filter(f -> f.getSource().name().equals("planner")).collect(Collectors.toList());

			if (!runtimeInputs.isEmpty()) {
				appendLine(sb, "Inputs (runtime-injected — do not include in step input):");
				for (AgentInputField f : limit(runtimeInputs, MaxInputs)) {
					String line = "- " + f.getName() + " (" + f.getType() + ")";
					if (f.isRequired())
						line += " [required]";
					appendLine(sb, line);
				}
			}

			if (!plannerInputs.isEmpty()) {
				appendLine(sb, "Inputs (planner-provided — include in step input as needed):");
				for (AgentInputField f : limit(plannerInputs, MaxInputs)) {
					String line = "- " + f.getName() + " (" + f.getType() + ")";
					if (f.isRequired())
						line += " [required]";
					appendLine(sb, line);
				}
			}
		}

		if (agent.getOutput() != null && !agent.getOutput().isBlank()) {
			appendLine(sb, "Output:");
			appendLine(sb, "- " + shorten(agent.getOutput()));
		}

		return sb.toString().trim();
	}

	// =========================
	// HELPERS
	// =========================

	private static void appendLine(StringBuilder sb, String line) {
		sb.append(line.trim()).append("\n");
	}

	private static boolean isEmpty(List<?> list) {
		return list == null || list.isEmpty();
	}

	private static <T> List<T> limit(List<T> list, int max) {
		return list.stream().limit(max).collect(Collectors.toList());
	}

	private static List<AgentInputField> sortInputs(Agent.Inputs inputs) {
		return inputs.properties.entrySet().stream().map(entry -> new AgentInputField(entry.getKey(), entry.getValue()))
				.sorted(Comparator.comparing((AgentInputField f) -> !f.isRequired())).collect(Collectors.toList());
	}

	private static List<String> prependDash(Collection<String> items) {
		return items.stream().map(item -> "- " + item).collect(Collectors.toList());
	}

	// Basic shortening heuristic
	private static String shorten(String text) {
		if (text == null)
			return "";

		String cleaned = text.replaceAll("\\s+", " ").trim();

		String[] words = cleaned.split(" ");
		if (words.length <= MaxWords) {
			return cleaned;
		}

		return String.join(" ", Arrays.copyOfRange(words, 0, MaxWords));
	}
}
