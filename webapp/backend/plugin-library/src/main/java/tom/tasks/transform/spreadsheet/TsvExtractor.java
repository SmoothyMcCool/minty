package tom.tasks.transform.spreadsheet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import tom.api.task.Packet;

/**
 * Utility to convert the raw Tika text extract of TSV data into a list of
 * records.
 *
 * <p>
 * Supported data types (in order of detection):
 * </p>
 * <ul>
 * <li>Boolean (case-insensitive “true” / “false”)</li>
 * <li>Character (single-character string)</li>
 * <li>Byte (as Integer), Short (as Integer), Integer, Long (numeric
 * ranges)</li>
 * <li>Float, Double (decimal numbers)</li>
 * <li>LocalDate (formats: yyyy/MM/dd or yyyy-MM-dd)</li>
 * <li>String (fallback)</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 *     String extract = /* Tika output * /;
 *     List&lt;Map&lt;String, Object&gt;&gt; rows = ExcelExtractor.parse(extract);
 * </pre>
 */
public final class TsvExtractor {

	// Pattern that matches the formatting lines that start with '&'
	private static final Pattern FORMAT_LINE = Pattern.compile("^&.*");

	// Date formats that are commonly produced by Tika/Excel
	private static final DateTimeFormatter[] DATE_FORMATTERS = { DateTimeFormatter.ofPattern("yyyy/MM/dd"),
			DateTimeFormatter.ISO_LOCAL_DATE };

	private TsvExtractor() {
		// Utility class – no instances
	}

	/**
	 * Parses the raw Tika extract of an Excel sheet into a list of records.
	 *
	 * @param extract the raw text extract returned by Tika
	 * @return a list of maps, each map representing a row with column names as keys
	 * @throws IllegalArgumentException if the extract is null or does not contain a
	 *                                  header row
	 */
	public static List<Packet> parse(String extract) {
		if (extract == null) {
			throw new IllegalArgumentException("Extract cannot be null");
		}

		List<Packet> result = new ArrayList<>();

		String[] lines = extract.split("\\r?\\n");
		List<String> headerTokens = null;

		for (String line : lines) {

			// Skip completely empty lines
			if (line.isEmpty()) {
				continue;
			}

			// Skip formatting lines (e.g. &"Times New Roman,Regular"&12&A\t)
			if (FORMAT_LINE.matcher(line).matches()) {
				continue;
			}

			// Any line containing a tab is either a header or data row
			if (!line.contains("\t")) {
				continue;
			}

			// Header line: first tab-delimited line encountered
			if (headerTokens == null) {
				headerTokens = splitLine(line.trim());
				continue;
			}

			// Data row
			List<String> tokens = splitLine(line.trim());
			Map<String, Object> row = new LinkedHashMap<>();

			for (int i = 0; i < headerTokens.size(); i++) {
				String key = headerTokens.get(i);
				String value = i < tokens.size() ? tokens.get(i) : null;
				row.put(key, parseValue(value));
			}

			Packet rowPacket = new Packet();
			rowPacket.addData(row);
			result.add(rowPacket);
		}

		if (headerTokens == null) {
			throw new IllegalArgumentException("No header row found in extract");
		}

		return result;
	}

	/**
	 * Splits a tab-separated line into tokens, trimming leading/trailing
	 * whitespace.
	 *
	 * @param line the line to split
	 * @return a list of tokens
	 */
	private static List<String> splitLine(String line) {
		// Remove the leading tab that Tika puts before the header/data rows
		if (line.startsWith("\t")) {
			line = line.substring(1);
		}
		String[] parts = line.split("\\t");
		List<String> tokens = new ArrayList<>(parts.length);
		for (String part : parts) {
			tokens.add(part.trim());
		}
		return tokens;
	}

	/**
	 * Attempts to convert a string value to a more specific type. Detection order:
	 * Boolean -> Character -> Byte -> Short -> Integer -> Long -> Float -> Double
	 * -> LocalDate -> String.
	 *
	 * @param raw the raw string value
	 * @return the parsed value, or {@code null} if {@code raw} is {@code null} or
	 *         empty
	 */
	private static Object parseValue(String raw) {
		if (raw == null || raw.isEmpty()) {
			return null;
		}

		// Boolean
		String lc = raw.toLowerCase(Locale.ROOT);
		if ("true".equals(lc) || "false".equals(lc)) {
			return Boolean.parseBoolean(lc);
		}

		// Character (single-character string)
		if (raw.length() == 1) {
			return raw.charAt(0);
		}

		// Integer
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException ignored) {
		}

		// Long
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException ignored) {
		}

		// Float
		try {
			return Float.parseFloat(raw);
		} catch (NumberFormatException ignored) {
		}

		// Double
		try {
			return Double.parseDouble(raw);
		} catch (NumberFormatException ignored) {
		}

		// LocalDate
		for (DateTimeFormatter fmt : DATE_FORMATTERS) {
			try {
				return LocalDate.parse(raw, fmt).format(DateTimeFormatter.ISO_LOCAL_DATE);
			} catch (DateTimeParseException ignored) {
			}
		}

		// Fallback: plain string
		return raw;
	}
}
