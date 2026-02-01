package tom.api.services.cache;

public interface CacheService {

	Cache getCache(String name);

	default <T> T get(String cacheName, Object key, Class<T> type) {
		return getCache(cacheName).get(key, type);
	}

	default void put(String cacheName, Object key, Object value) {
		getCache(cacheName).put(key, value);
	}

	default void evict(String cacheName, Object key) {
		getCache(cacheName).evict(key);
	}

	default void clear(String cacheName) {
		getCache(cacheName).clear();
	}
}
