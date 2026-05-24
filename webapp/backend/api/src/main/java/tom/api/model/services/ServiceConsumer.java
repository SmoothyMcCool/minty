package tom.api.model.services;

import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.services.PluginServices;

public interface ServiceConsumer {

	void setPluginServices(PluginServices pluginServices);

	// Optional method, if userId is not required.
	default void setUserId(UserId userId) {
	}

	// Optional method, if conversationId is not required.
	default void setConversationId(ConversationId conversationId) {

	}
}
