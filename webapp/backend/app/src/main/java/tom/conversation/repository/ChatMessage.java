package tom.conversation.repository;

public class ChatMessage {

	private boolean user;
	private String message;

	public ChatMessage() {
		user = false;
		message = "";
	}

	public ChatMessage(boolean user, String message) {
		this.user = user;
		this.message = message;
	}

	public boolean isUser() {
		return user;
	}

	public void setUser(boolean user) {
		this.user = user;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
