package tom.api.services;

import java.io.IOException;
import java.nio.file.Path;

import tom.output.ExecutionResult;

public interface RenderService {

	Path renderPug(String template, String outfileName, ExecutionResult data) throws IOException;

	Path renderJson(String outfileName, ExecutionResult data) throws IOException;
}
