package tom.api.services;

import java.io.IOException;
import java.util.List;

import tom.task.ExecutionResult;
import tom.task.Packet;

public interface RenderService {

	String renderPug(String template, ExecutionResult data) throws IOException;

	String renderPug(String template, Packet data) throws IOException;

	String renderJson(ExecutionResult data) throws IOException;

	List<String> listPugTemplates();

}
