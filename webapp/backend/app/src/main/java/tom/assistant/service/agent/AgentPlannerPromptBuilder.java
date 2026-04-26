package tom.assistant.service.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import tom.Pair;
import tom.assistant.service.agent.llm.LlmStatus;
import tom.assistant.service.agent.model.Agent;
import tom.assistant.service.agent.model.AgentInputField;
import tom.assistant.service.agent.model.AgentStep;
import tom.assistant.service.agent.model.AgentStepState;
import tom.assistant.service.agent.model.AgentStepType;
import tom.assistant.service.agent.model.PlanState;

public class AgentPlannerPromptBuilder {

	private static final int MaxWords = 12;
	private static final ObjectMapper Mapper = new ObjectMapper();

	public static String buildPrompt(String prompt, Collection<Agent> staticAgents, Collection<Agent> dynamicAgents,
			String chatHistory, PlanState state) {
		String dynamicAgentsBlock = "";
		if (dynamicAgents != null) {
			dynamicAgentsBlock = dynamicAgents.stream().map(agent -> getAgentSummary(agent))
					.collect(Collectors.joining("\n\n"));
		}

		String staticAgentsBlock = "";
		if (staticAgents != null) {
			staticAgentsBlock = staticAgents.stream().map(agent -> getAgentSummary(agent))
					.collect(Collectors.joining("\n\n"));
		}

		String stateAsString = "";
		if (state != null) {
			stateAsString = buildPlanContext(state);
		}

		return prompt.replace("{{STATIC_WORKERS}}", staticAgentsBlock).replace("{{DYNAMIC_AGENTS}}", dynamicAgentsBlock)
				.replace("{{PLAN_CONTEXT_JSON}}", stateAsString).replace("{{CHAT_HISTORY}}", chatHistory);
	}

	private static String buildPlanContext(PlanState state) {

		if (state == null || state.getSteps() == null) {
			return "{}";
		}

		List<Map<String, Object>> completedSteps = new ArrayList<>();
		List<Map<String, Object>> remainingSteps = new ArrayList<>();
		List<String> replanInstructions = new ArrayList<>();

		List<Pair<AgentStep, AgentStepState>> steps = state.getSteps();

		for (Pair<AgentStep, AgentStepState> pair : steps) {
			AgentStep step = pair.left();
			AgentStepState stepState = pair.right();
			LlmStatus status = stepState.getStatus();

			if (status == LlmStatus.PENDING) {
				Map<String, Object> m = new HashMap<>();
				m.put("id", step.getId());
				m.put("name", step.getName());
				m.put("worker", step.getWorker());
				m.put("type", step.getType());
				m.put("input", step.getInput());
				m.put("visibility", step.getVisibility());
				remainingSteps.add(m);

			} else {
				Map<String, Object> m = new HashMap<>();
				m.put("id", step.getId());
				m.put("name", step.getName());
				m.put("type", step.getType());
				m.put("status", status);

				if (step.getType() == AgentStepType.PLAN && status == LlmStatus.SUCCESS) {
					// extract the replan reason and surface it as a directive
					Object input = step.getInput();
					if (input instanceof Map<?, ?> inputMap) {
						Object reason = inputMap.get("reason");
						if (reason != null) {
							m.put("replanReason", reason);
							replanInstructions.add(reason.toString());
						}
					}
				} else if (stepState.getResponse() != null && stepState.getResponse().getData() != null) {
					m.put("output", stepState.getResponse().getData());
				}

				completedSteps.add(m);
			}
		}

		Map<String, Object> context = new LinkedHashMap<>();
		context.put("completedSteps", completedSteps);
		context.put("remainingSteps", remainingSteps);

		if (!replanInstructions.isEmpty()) {
			String directive = replanInstructions.size() == 1 ? replanInstructions.get(0)
					: String.join("; ", replanInstructions);
			context.put("replanInstruction",
					"A completed PLAN step requires you to insert new steps before the remaining steps. "
							+ "Insert them after the last completed step and before the first remaining step. "
							+ "Do NOT recreate the PLAN step itself. Reason: " + directive);
		}

		try {
			return Mapper.writeValueAsString(context);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String createAgentDefinition(Agent agent) {
		StringBuilder sb = new StringBuilder();

		appendLine(sb, "Agent: " + agent.getName());
		appendLine(sb, "Purpose: " + agent.getPurpose());

		if (!isEmpty(agent.getWhenToUse())) {
			appendLine(sb, "Use when:");
			for (String rule : agent.getWhenToUse()) {
				appendLine(sb, "- " + rule);
			}
		}

		if (!isEmpty(agent.getWhenNotToUse())) {
			appendLine(sb, "Avoid when:");
			for (String rule : agent.getWhenNotToUse()) {
				appendLine(sb, "- " + rule);
			}
		}

		if (agent.getInputs() != null && !agent.getInputs().properties.isEmpty()) {
			List<AgentInputField> sortedInputs = sortInputs(agent.getInputs());

			List<AgentInputField> runtimeInputs = sortedInputs.stream()
					.filter(f -> f.getSource().name().equals("runtime")).collect(Collectors.toList());

			List<AgentInputField> plannerInputs = sortedInputs.stream()
					.filter(f -> f.getSource().name().equals("planner")).collect(Collectors.toList());

			if (!runtimeInputs.isEmpty()) {
				appendLine(sb, "Inputs (runtime-injected - do not include in step input):");
				for (AgentInputField f : runtimeInputs) {
					String line = "- " + f.getName() + " (" + f.getType() + ")";
					if (f.isRequired()) {
						line += " [required]";
					}
					if (f.getDescription() != null) {
						line += ": " + f.getDescription();
					}
					appendLine(sb, line);
				}
			}

			if (!plannerInputs.isEmpty()) {
				appendLine(sb, "Inputs (planner-provided - include in step input as needed):");
				for (AgentInputField f : plannerInputs) {
					String line = "- " + f.getName() + " (" + f.getType() + ")";
					if (f.isRequired())
						line += " [required]";
					if (f.getDescription() != null)
						line += ": " + f.getDescription();
					appendLine(sb, line);
				}
			}
		}

		if (agent.getOutput() != null && !agent.getOutput().isBlank()) {
			appendLine(sb, "Output:");
			appendLine(sb, "- " + agent.getOutput());
		}

		if (agent.getPrompt() != null && !agent.getPrompt().isBlank()) {
			appendLine(sb, "Prompt summary:");
			appendLine(sb, "- " + shorten(agent.getPrompt()));
		}

		return sb.toString().trim();
	}

	private static String getAgentSummary(Agent agent) {
		StringBuilder sb = new StringBuilder();

		appendLine(sb, "Agent: " + agent.getName());
		appendLine(sb, "Purpose: " + shorten(agent.getPurpose()));

		if (!isEmpty(agent.getWhenToUse())) {
			appendLine(sb, "Use when: " + agent.getWhenToUse().get(0)); // first rule only
		}

		if (!isEmpty(agent.getWhenNotToUse())) {
			appendLine(sb, "Avoid when: " + agent.getWhenNotToUse().get(0)); // first rule only
		}

		return sb.toString().trim();
	}

	private static void appendLine(StringBuilder sb, String line) {
		sb.append(line.trim()).append("\n");
	}

	private static boolean isEmpty(List<?> list) {
		return list == null || list.isEmpty();
	}

	private static List<AgentInputField> sortInputs(Agent.Inputs inputs) {
		return inputs.properties.entrySet().stream().map(entry -> new AgentInputField(entry.getKey(), entry.getValue()))
				.sorted(Comparator.comparing((AgentInputField f) -> !f.isRequired())).collect(Collectors.toList());
	}

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
