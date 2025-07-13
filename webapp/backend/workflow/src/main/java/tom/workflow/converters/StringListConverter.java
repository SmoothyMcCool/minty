package tom.workflow.converters;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.AttributeConverter;

public class StringListConverter implements AttributeConverter<List<String>, String>{

	private static final String SPLIT_CHAR = ";";

	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		if (attribute == null) {
			return "";
		}

		return String.join(SPLIT_CHAR, attribute);
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return List.of();
		}

		return Arrays.asList(dbData.split(SPLIT_CHAR));
	}

}
