package tom.api.services;

import java.io.IOException;
import java.util.List;

import tom.api.task.ExecutionResult;
import tom.api.task.Packet;

public interface RenderService {

	String renderPug(String template, ExecutionResult data) throws IOException;

	String renderPug(String template, Packet data) throws IOException;

	String renderJson(ExecutionResult data) throws IOException;

	String getPugTemplate(String template) throws IOException;

	List<String> listInlinePugTemplates() throws IOException;

	List<String> listOutputPugTemplates() throws IOException;
}
