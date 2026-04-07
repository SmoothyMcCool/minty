package tom.meta.controller;

import java.time.LocalDate;

public record Metadata(String id, int totalAssistantsCreated, int totalConversations, int totalWorkflowsCreated,
		int totalWorkflowRuns, int totalLogins, LocalDate lastLogin) {

}
