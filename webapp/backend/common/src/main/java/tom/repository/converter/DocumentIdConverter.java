package tom.repository.converter;

import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tom.api.DocumentId;

@Converter(autoApply = true)
public class DocumentIdConverter implements AttributeConverter<DocumentId, UUID> {

	@Override
	public UUID convertToDatabaseColumn(DocumentId attribute) {
		return attribute == null ? null : attribute.value();
	}

	@Override
	public DocumentId convertToEntityAttribute(UUID dbData) {
		return dbData == null ? null : new DocumentId(dbData);
	}

}
