package tom.api.services.cache;

import java.util.concurrent.Callable;

public interface Cache {
	<T> T get(Object key, Class<T> type);

	<T> T get(Object key, Callable<T> valueLoader);

	void put(Object key, Object value);

	void evict(Object key);

	void clear();
}
