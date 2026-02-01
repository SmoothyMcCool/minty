package tom.cache.service;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import tom.api.services.cache.Cache;
import tom.api.services.cache.CacheService;

@Service
public class CacheServiceImpl implements CacheService {

	private final CacheManager cacheManager;

	private final Map<String, Cache> exposedCaches = new ConcurrentHashMap<>();

	public CacheServiceImpl(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	public Cache getCache(String name) {
		return exposedCaches.computeIfAbsent(name, this::createCache);
	}

	private Cache createCache(String name) {
		org.springframework.cache.Cache delegate = cacheManager.getCache(name);
		if (delegate == null) {
			throw new IllegalStateException("CacheManager could not create cache named '" + name + "'");
		}
		return new SpringCacheFacade(delegate);
	}

	private static final class SpringCacheFacade implements Cache {
		private final org.springframework.cache.Cache delegate;

		SpringCacheFacade(org.springframework.cache.Cache delegate) {
			this.delegate = delegate;
		}

		@Override
		public <T> T get(Object key, Class<T> type) {
			org.springframework.cache.Cache.ValueWrapper wrapper = delegate.get(key);
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

		@Override
		public <T> T get(Object key, Callable<T> valueLoader) {
			return delegate.get(key, valueLoader);
		}

		@Override
		public void put(Object key, Object value) {
			delegate.put(key, value);
		}

		@Override
		public void evict(Object key) {
			delegate.evict(key);
		}

		@Override
		public void clear() {
			delegate.clear();
		}

	}

}
