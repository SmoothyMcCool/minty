package tom.config.model;

public record DatabaseConfig(String url, int port, String name, String user, String password, long maxPacketSize,
		boolean useCompression) {
}
