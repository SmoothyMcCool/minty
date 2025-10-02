package tom.repository.converter;

import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tom.api.AssistantId;

@Converter(autoApply = true)
public class AssistantIdConverter implements AttributeConverter<AssistantId, UUID> {

	@Override
	public UUID convertToDatabaseColumn(AssistantId attribute) {
		return attribute == null ? null : attribute.value();
	}

	@Override
	public AssistantId convertToEntityAttribute(UUID dbData) {
		return dbData == null ? null : new AssistantId(dbData);
	}

}
