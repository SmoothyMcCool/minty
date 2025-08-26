package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.model.ChatMessage;

@Converter
public class ChatMessageListConverter extends ClassConverter<ChatMessage> {
	public ChatMessageListConverter() {
		super(new TypeReference<ChatMessage>() {
		});
	}
}
