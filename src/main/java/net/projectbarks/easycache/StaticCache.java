package net.projectbarks.easycache;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.lang.instrument.Instrumentation;

/**
 * Created by brandon on 10/4/14.
 */
public class StaticCache {

    private static List<String> cacheKeys;
    private static List<CachedObject> cacheValues;
    private static long maxSize;

    static {
        cacheKeys = new ArrayList<String>();
        cacheValues = new ArrayList<CachedObject>();
        maxSize = EasyCacheDiskUnit.Megabyte.getAmount(100);
    }

    /**
     * This function adds string-object pairs into a ConcurrentHashMap. The object
     * will undergo a serialization process and inserted into the map. The serialization
     * hard clones an object removing all references to the prior object.
     * {@link #getCacheObject(String, Class) getCachedObject} can be called later to
     * demoralize the object and reuse it.
     *
     * Note it is important to have your object compatible with json or be a native object
     * otherwise the object may fail to serialize and an error will be thrown.
     *
     * @param key the key to be used later
     * @param value the value to be found for later
     * @param lifetime set how long a object lives for to be removed from cache.
     */
    public static void storeCacheObject(final String key, final Object value, TimeUnit unit,  Long lifetime) {
        if (isNull(key, lifetime)) {
            throw new NullPointerException("You can not use a null" + (key == null ? "key" : "lifetime"));
        }
        long finalTime = System.currentTimeMillis() + unit.toMillis(lifetime);
        finalTime = lifetime <= -1 ? -1 : finalTime;
        CachedObject cachedObject = new CachedObject(value, finalTime);

        int index = Collections.binarySearch(cacheValues, cachedObject);
        if (index < 0) index = ~index;
        cacheValues.add(index, cachedObject);
        cacheKeys.add(index, key);
    }

    /**
     * This function will attempt to find a cached object with the type inserted.
     * An exception will be thrown when a key cannot be found or when a type is
     * invalid.
     *
     * @param key the id you used for store
     * @param type the type you stored the object as
     * @param <T> the return type you used in type
     * @return will return the casted value if it has not failed
     */
    public static <T> T getCacheObject(final String key, Class<T> type) {
        if (isNull(key, type)) {
            throw new NullPointerException("You can not use a null" + (key == null ? "key" : "type"));
        }
        if (!cacheKeys.contains(key)) {
            return null;
        }
        T value;
        try {
            value = type.cast(cacheValues.get(cacheKeys.indexOf(key)).getValue());
        } catch (ClassCastException exception) {
            exception.printStackTrace();
            throw new ClassCastException("Invalid type " + type.getName() + " for value!");
        }
        return value;
    }

    /**
     * This function will clear all previously stored cache. This will free up additional memory depending on how many
     * objects are stored.
     */
    public static void clearCache() {
        cacheKeys.clear();
        cacheValues.clear();
    }

    /**
     * This function will delete a single entry from the cache. This will remove irrelevant information
     *
     * @param key the key to delete along with its associated value
     */
    public static void deleteEntryFromCache(final String key) {
        if (isNull(key)) {
            throw new NullPointerException("You can not use a null key");
        }
        if(cacheKeys.contains(key)) {
            int i = cacheKeys.indexOf(key);
            cacheKeys.remove(i);
            cacheValues.remove(i);
        } else {
            throw new NullPointerException("Unable to find key!");
        }
    }

    public static void setMaxSize(EasyCacheDiskUnit unit, int amount) {
        maxSize = unit.toBytes(amount);
    }

    protected static void checkLifetime() {
        List<String> keysToRemove = new ArrayList<String>();
        ListIterator<CachedObject> li = cacheValues.listIterator(cacheValues.size());
        int index = cacheValues.size() - 1;
        while (li.hasPrevious()) {
            CachedObject entry = li.previous();
            if (entry.getLifeTime() <= -1) {
                break;
            }
            if (entry.getLifeTime() > System.currentTimeMillis()) {
                break;
            }
            keysToRemove.add(cacheKeys.get(index));
            index--;
        }
        for (String key : keysToRemove) {
            int i = cacheKeys.indexOf(key);
            cacheKeys.remove(i);
            cacheValues.remove(i);
        }
    }

    private static boolean isNull(Object... nulls) {
        boolean failed = false;
        for (Object o : nulls) {
            if (o != null) {
                continue;
            }
            failed = true;
            break;
        }
        return failed;
    }
}