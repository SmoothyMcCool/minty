package tom.cache.service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;

import tom.cache.SingleFlightCache;

public class SingleFlightCacheManager implements CacheManager {

	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

	@Override
	public Cache getCache(@NonNull String name) {
		return caches.computeIfAbsent(name, SingleFlightCache::new);
	}

	@Override
	public @NonNull Collection<String> getCacheNames() {
		return caches.keySet();
	}
}
