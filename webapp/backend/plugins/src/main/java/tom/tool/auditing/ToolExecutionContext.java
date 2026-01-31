package tom.tool.auditing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolExecutionContext {

	public static final String REQUEST_ID = "requestId";
	public static final String USER_ID = "userId";
	public static final String ASSISTANT_ID = "assistantId";

	// Thread‑safe, lock‑free map
	private static final Map<String, Map<String, String>> ContextMap = new ConcurrentHashMap<>();

	// Private constructor to prevent instantiation
	private ToolExecutionContext() {
	}

	/**
	 * Store a map of parameters for the given key. The supplied map is defensively
	 * copied to avoid external mutation.
	 */
	public static void set(String key, Map<String, String> params) {
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null");
		}
		if (params == null) {
			throw new IllegalArgumentException("params cannot be null");
		}
		ContextMap.put(key, new HashMap<>(params));
	}

	/**
	 * Atomically retrieve the map for {@code key} and remove it. Returns an empty
	 * immutable map if the key was not present.
	 */
	public static Map<String, String> getAndClear(String key) {
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null");
		}
		Map<String, String> map = ContextMap.remove(key);
		return map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
	}

	/**
	 * Explicitly remove the entry for {@code key}. Returns {@code true} if an entry
	 * was removed.
	 */
	public static boolean remove(String key) {
		return ContextMap.remove(key) != null;
	}

	/**
	 * Clear all stored context entries. Useful for tests or shutdown.
	 */
	public static void clearAll() {
		ContextMap.clear();
	}
}
