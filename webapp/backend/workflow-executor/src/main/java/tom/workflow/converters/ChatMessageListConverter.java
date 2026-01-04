package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.api.model.conversation.ChatMessage;
import tom.util.ClassConverter;

@Converter
public class ChatMessageListConverter extends ClassConverter<ChatMessage> {
	public ChatMessageListConverter() {
		super(new TypeReference<ChatMessage>() {
		});
	}
}
