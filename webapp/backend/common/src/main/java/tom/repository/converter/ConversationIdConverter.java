package tom.repository.converter;

import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tom.api.ConversationId;

@Converter(autoApply = true)
public class ConversationIdConverter implements AttributeConverter<ConversationId, UUID> {

	@Override
	public UUID convertToDatabaseColumn(ConversationId attribute) {
		return attribute == null ? null : attribute.value();
	}

	@Override
	public ConversationId convertToEntityAttribute(UUID dbData) {
		return dbData == null ? null : new ConversationId(dbData);
	}

}
