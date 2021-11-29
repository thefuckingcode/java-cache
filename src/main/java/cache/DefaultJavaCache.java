package cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultJavaCache<T> implements JavaCache<T> {
    private Long defaultExpiration;
    private ConcurrentHashMap<String, Item<T>> items;
    private ReentrantReadWriteLock readWriteLock;
    private EvictedHandler<T> evictedHandler;
    private ScheduledThreadPoolExecutor janitor;

    private DefaultJavaCache() {

    }

    private DefaultJavaCache(Long defaultExpiration, ConcurrentHashMap<String, Item<T>> items, ScheduledThreadPoolExecutor janitor, EvictedHandler<T> evictedHandler) {
        this.defaultExpiration = defaultExpiration;
        this.items = items;
        this.janitor = janitor;
        this.evictedHandler = evictedHandler;
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    public static <T> JavaCache<T> New(Long defaultExpiration, Long cleanupInterval, ConcurrentHashMap<String, Item<T>> items) {
        return New(defaultExpiration, cleanupInterval, items, null);
    }

    public static <T> JavaCache<T> New(Long defaultExpiration, Long cleanupInterval, ConcurrentHashMap<String, Item<T>> items, EvictedHandler<T> evictedHandler) {
        return newDefaultJavaCache(defaultExpiration, cleanupInterval, items, evictedHandler == null ? new DefaultEvictedHandler<T>() : evictedHandler);
    }

    private static <T> JavaCache<T> newDefaultJavaCache(Long defaultExpiration, Long cleanupInterval, ConcurrentHashMap<String, Item<T>> items, EvictedHandler<T> evictedHandler) {
        ScheduledThreadPoolExecutor janitor = new ScheduledThreadPoolExecutor(1);
        DefaultJavaCache<T> defaultJavaCache = new DefaultJavaCache<>(defaultExpiration, items, janitor, evictedHandler);
        defaultJavaCache.getJanitor().scheduleWithFixedDelay(() -> {
            for (Map.Entry<String, Item<T>> entry : items.entrySet()) {
                if (entry.getValue().getExpiration() < System.currentTimeMillis()) {
                    items.remove(entry.getKey());
                }
            }
        }, 0, cleanupInterval, TimeUnit.MILLISECONDS);
        return defaultJavaCache;
    }

    @Override
    public void put(String key, T value, Long duration) {
        readWriteLock.writeLock().lock();
        try {
            Item<T> item = new Item<>(value, duration);
            this.items.put(key, item);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public T get(String key) {
        readWriteLock.readLock().lock();
        try {
            Item<T> item = this.items.get(key);
            if (item != null && item.getExpiration() > System.currentTimeMillis()) {
                return item.getValue();
            } else {
                return null;
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        this.evictedHandler.onEvicted(this, null);
    }

    public Long getDefaultExpiration() {
        return defaultExpiration;
    }

    public void setDefaultExpiration(Long defaultExpiration) {
        this.defaultExpiration = defaultExpiration;
    }

    public ConcurrentHashMap<String, Item<T>> getItems() {
        return items;
    }

    public void setItems(ConcurrentHashMap<String, Item<T>> items) {
        this.items = items;
    }

    public ReentrantReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    public void setReadWriteLock(ReentrantReadWriteLock readWriteLock) {
        this.readWriteLock = readWriteLock;
    }

    public EvictedHandler<T> getEvictedHandler() {
        return evictedHandler;
    }

    public void setEvictedHandler(EvictedHandler<T> evictedHandler) {
        this.evictedHandler = evictedHandler;
    }

    public ScheduledThreadPoolExecutor getJanitor() {
        return janitor;
    }

    public void setJanitor(ScheduledThreadPoolExecutor janitor) {
        this.janitor = janitor;
    }

    public static class Item<T> {
        private T value;
        private Long expiration;

        public Item(T value, Long duration) {
            this.value = value;
            this.expiration = System.currentTimeMillis() + duration;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public Long getExpiration() {
            return expiration;
        }

        public void setExpiration(Long expiration) {
            this.expiration = expiration;
        }
    }

    public static class DefaultEvictedHandler<T> implements EvictedHandler<T> {
        @Override
        public void onEvicted(JavaCache<T> javaCache, Object args) {
            ((DefaultJavaCache<T>) javaCache).getJanitor().shutdown();
        }
    }
}
