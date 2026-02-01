package tom.cache;

public record CacheValue<V>(V value, long writeTimeMs) {
}