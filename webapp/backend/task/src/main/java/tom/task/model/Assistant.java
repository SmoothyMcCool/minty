package tom.task.model;

public record Assistant(Integer id, String name, String prompt, Integer numFiles, Integer processedFiles,
        AssistantState state, Integer ownerId, boolean shared) {

}
