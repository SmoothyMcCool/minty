package tom.tasks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TaskUtils {

	private static final ObjectMapper mapper = new ObjectMapper();

	private TaskUtils() {
	}

	/**
	 * Attempts to convert the given object into the specified target type. Returns
	 * null if conversion fails.
	 *
	 * @param value      the source object
	 * @param targetType the target class
	 * @param <T>        the target type
	 * @return converted instance, or null if conversion is not valid
	 */
	public static <T> T safeConvert(Object value, Class<T> targetType) {
		if (value == null) {
			return null;
		}
		try {
			return mapper.convertValue(value, targetType);
		} catch (IllegalArgumentException e) {
			// conversion failed
			return null;
		}
	}

	/**
	 * Attempts to convert the given object into the specified target type. Returns
	 * null if conversion fails.
	 *
	 * @param value      the source object
	 * @param targetType the target class
	 * @param <T>        the target type
	 * @return converted instance, or null if conversion is not valid
	 */
	public static <T> T safeConvert(Object value, TypeReference<T> typeRef) {
		if (value == null) {
			return null;
		}
		try {
			return mapper.convertValue(value, typeRef);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
