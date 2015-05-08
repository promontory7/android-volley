package com.android.volley;

import java.util.Collections;
import java.util.Map;

/**
 *获取请求结果 存储请求结果的缓存
 */
public interface Cache {

    /**
     * 获取请求的缓存实体
     */
    public Entry get(String key);

    /**
     * 存储缓存实体
     */
    public void put(String key, Entry entry);

    /**
     * Performs any potentially long-running actions needed to initialize the cache;
     * will be called from a worker thread.
     */
    public void initialize();

    /**
     * Invalidates an entry in the cache.
     * @param key Cache key
     * @param fullExpire True to fully expire the entry, false to soft expire
     */
    public void invalidate(String key, boolean fullExpire);

    /**
     * Removes an entry from the cache.
     * @param key Cache key
     */
    public void remove(String key);

    /**
     * Empties the cache.
     */
    public void clear();

    /**
     * Data and metadata for an entry returned by the cache.
     */
    public static class Entry {
        /**
         * 数据
         */
        public byte[] data;

        /**
         * http响应首部中用于缓存新鲜度验证的ETag
         */
        public String etag;

        /**
         * http响应首部中响应产生的时间
         */
        public long serverDate;

        /** 最后修改的时间 */
        public long lastModified;

        /** 缓存的过期时间*/
        public long ttl;

        /**  缓存的新鲜时间*/
        public long softTtl;

        /** 响应的Headers */
        public Map<String, String> responseHeaders = Collections.emptyMap();

        /** 缓存是否过期 */
        public boolean isExpired() {
            return this.ttl < System.currentTimeMillis();
        }

        /**  判断是否新鲜 */
        public boolean refreshNeeded() {
            return this.softTtl < System.currentTimeMillis();
        }
    }

}
