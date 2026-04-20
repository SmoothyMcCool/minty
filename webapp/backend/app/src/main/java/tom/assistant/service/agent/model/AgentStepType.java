package tom.assistant.service.agent.model;

public enum AgentStepType {
	ACTION, // Calls an agent (default case)
	ASK, // Ask user for input
	PLAN // Trigger replanning
}
