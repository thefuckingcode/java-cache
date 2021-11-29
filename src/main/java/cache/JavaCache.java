package cache;

public interface JavaCache<T> {
    void put(String key, T value, Long duration);

    T get(String key);

    void close();
}
