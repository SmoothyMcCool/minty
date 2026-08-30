package tom.api.model.conversation;

public class ChatMessage {

	private MessageType type;
	private String message;

	public ChatMessage() {
		type = MessageType.ASSISTANT;
		message = "";
	}

	public ChatMessage(MessageType type, String message) {
		this.type = type;
		this.message = message;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
