package tom.api.services;

public interface DocumentService {

	String fileBytesToText(byte[] bytes);

	String fileBytesToMarkdown(byte[] bytes);
}
