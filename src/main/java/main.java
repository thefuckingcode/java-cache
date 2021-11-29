import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import cache.DefaultJavaCache;
import cache.JavaCache;

public class main {
    public static void main(String[] args) {
        ConcurrentHashMap<String, DefaultJavaCache.Item<Integer>> items = new ConcurrentHashMap<>(10);
        JavaCache<Integer> javaCache = DefaultJavaCache.New(1000L, 3000L, items);
        javaCache.put("one", 1, TimeUnit.SECONDS.toMillis(5L));
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(javaCache.get("one"));
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(javaCache.get("one"));
        javaCache.close();
    }
}
