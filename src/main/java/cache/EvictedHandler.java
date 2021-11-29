package cache;

@FunctionalInterface
public interface EvictedHandler<T> {
    void onEvicted(JavaCache<T> javaCache, Object args);
}
