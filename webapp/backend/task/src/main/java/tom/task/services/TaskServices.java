package tom.task.services;

public interface TaskServices {

    AssistantService getAssistantService();

    ConversationService getConversationService();

    DocumentService getDocumentService();

    HttpService getHttpService();

    PythonService getPythonService();
}
