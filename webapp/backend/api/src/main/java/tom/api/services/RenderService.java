package tom.api.services;

import java.io.IOException;

import tom.output.ExecutionResult;

public interface RenderService {

	String renderPug(String template, ExecutionResult data) throws IOException;

	String renderJson(ExecutionResult data) throws IOException;

}
