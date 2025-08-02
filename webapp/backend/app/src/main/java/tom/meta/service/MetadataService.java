package tom.meta.service;

public interface MetadataService {

	void newAssistant(int userId);

	void userLoggedIn(int userId);

	void workflowCreated(int userId);

	void workflowExecuted(int userId);

	void newConversation(int userId);

	void addUser(int userId);

}
