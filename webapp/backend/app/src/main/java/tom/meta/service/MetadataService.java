package tom.meta.service;

import java.util.UUID;

public interface MetadataService {

	void newAssistant(UUID userId);

	void userLoggedIn(UUID userId);

	void workflowCreated(UUID userId);

	void workflowExecuted(UUID userId);

	void newConversation(UUID userId);

	void addUser(UUID userId);

}
