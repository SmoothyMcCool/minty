package tom.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class SingleFlightCache implements Cache {

	private static final long DefaultTtlMs = 5 * 60 * 1000L; // 5 minutes
	private static final Callable<Object> NullLoader = () -> null;

	private final String name;
	private final long ttlMs;
	private final ConcurrentHashMap<Object, CompletableFuture<CacheValue<Object>>> map = new ConcurrentHashMap<>();

	public SingleFlightCache(String name) {
		this(name, DefaultTtlMs);
	}

	public SingleFlightCache(String name, long ttlMs) {
		this.name = name;
		this.ttlMs = ttlMs;
	}

	/* ---------- Spring Cache API ---------- */

	@Override
	public @NonNull String getName() {
		return name;
	}

	@Override
	public @NonNull Object getNativeCache() {
		return map;
	}

	@Override
	public ValueWrapper get(@NonNull Object key) {
		Object value = get(key, (Callable<?>) NullLoader);
		return () -> value;
	}

	@Override
	public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
		ValueWrapper wrapper = get(key);
		if (wrapper == null) {
			return null;
		}
		Object val = wrapper.get();
		if (type != null && !type.isInstance(val)) {
			throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]");
		}
		@SuppressWarnings("unchecked")
		T result = type == null ? (T) val : type.cast(val);
		return result;
	}

	/**
	 * This is the method that Spring calls when {@code sync=true}. It guarantees
	 * that only one thread will actually invoke {@code valueLoader.call()}.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
		while (true) {
			CompletableFuture<CacheValue<Object>> future = map.get(key);

			if (future != null) {
				// Future already present
				if (future.isDone()) { // finished?
					CacheValue<Object> cached = future.getNow(null);
					if (cached != null && !isExpired(cached)) {
						return (T) cached.value(); // hit
					}
					// expired or null - try to replace it
					CompletableFuture<CacheValue<Object>> newFuture = new CompletableFuture<>();
					if (map.replace(key, future, newFuture)) {
						// we won the replace – load the value
						return loadAndComplete(key, newFuture, valueLoader);
					}
					// another thread beat us - loop again
					continue;
				} else {
					// Future is still running - wait for it
					try {
						CacheValue<Object> cv = future.get(); // blocks until the first loader finishes
						if (cv != null && !isExpired(cv)) {
							return (T) cv.value();
						}
						// value expired - loop to create a new future
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new ValueRetrievalException(key, valueLoader, e);
					} catch (ExecutionException e) {
						throw new ValueRetrievalException(key, valueLoader, e.getCause());
					}
				}
			} else {
				// No future yet - try to create one
				CompletableFuture<CacheValue<Object>> newFuture = new CompletableFuture<>();
				CompletableFuture<CacheValue<Object>> existing = map.putIfAbsent(key, newFuture);
				if (existing == null) {
					// we won the race - load the value
					return loadAndComplete(key, newFuture, valueLoader);
				}
				// another thread inserted a future - loop again
			}
		}
	}

	@Override
	public void put(@NonNull Object key, @Nullable Object value) {
		map.put(key, CompletableFuture.completedFuture(new CacheValue<>(value, System.currentTimeMillis())));
	}

	@Override
	public ValueWrapper putIfAbsent(@NonNull Object key, @Nullable Object value) {
		CompletableFuture<CacheValue<Object>> existing = map.get(key);
		if (existing == null) {
			final CompletableFuture<CacheValue<Object>> future = CompletableFuture
					.completedFuture(new CacheValue<>(value, System.currentTimeMillis()));
			CompletableFuture<CacheValue<Object>> prev = map.putIfAbsent(key, future);
			if (prev == null) {
				return null; // we won
			}
			// We lost the race; the value is already in the map.
			// Return a wrapper that points to the existing future.
			return () -> prev.getNow(null).value();
		}
		// We won the race; return a wrapper that points to the existing future.
		return () -> existing.getNow(null).value();
	}

	@Override
	public void evict(@NonNull Object key) {
		map.remove(key);
	}

	@Override
	public void clear() {
		map.clear();
	}

	/* ---------- Helpers ---------- */

	private boolean isExpired(CacheValue<Object> cv) {
		return System.currentTimeMillis() - cv.writeTimeMs() > ttlMs;
	}

	private <T> T loadAndComplete(Object key, CompletableFuture<CacheValue<Object>> future, Callable<T> valueLoader) {
		try {
			T loaded = valueLoader.call();
			CacheValue<Object> cv = new CacheValue<>(loaded, System.currentTimeMillis());
			future.complete(cv);
			return loaded;
		} catch (Throwable t) {
			future.completeExceptionally(t);
			map.remove(key, future); // clean up
			throw new ValueRetrievalException(key, valueLoader, t);
		}
	}
}