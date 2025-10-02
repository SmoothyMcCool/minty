package tom.meta.service;

import tom.api.UserId;

public interface MetadataService {

	void newAssistant(UserId userId);

	void userLoggedIn(UserId userId);

	void workflowCreated(UserId userId);

	void workflowExecuted(UserId userId);

	void newConversation(UserId userId);

	void addUser(UserId userId);

}
