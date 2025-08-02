package tom.result.service;

import java.io.IOException;
import java.util.List;

public interface ResultService {

	List<String> getAvailableResults() throws IOException;

	String getResult(String taskName) throws IOException;

	boolean deleteResult(String resultName) throws IOException;

}
