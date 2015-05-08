package com.android.volley;

import android.os.Process;

import java.util.concurrent.BlockingQueue;

/**
 * 本地线程
 */
public class CacheDispatcher extends Thread {

    private static final boolean DEBUG = VolleyLog.DEBUG;

    /**
     * T
     * 本地队列，从RequestQueue中传递进来
     */
    private final BlockingQueue<Request<?>> mCacheQueue;

    /**
     * 网络请求队列，当本地队列没有命中的时候，需要把本地队列加入到网络队列
     */
    private final BlockingQueue<Request<?>> mNetworkQueue;

    /**
     * 磁盘缓存
     */
    private final Cache mCache;

    /**
     * 用于从子线程向UI发送数据
     */
    private final ResponseDelivery mDelivery;

    private volatile boolean mQuit = false;

    /**
     *用于创建一个本地线程
     *
     * @param cacheQueue 本地队列
     * @param networkQueue 网络请求队列
     * @param cache 磁盘缓存
     * @param delivery 请求结果发送到UI线程
     */
    public CacheDispatcher(
            BlockingQueue<Request<?>> cacheQueue, BlockingQueue<Request<?>> networkQueue,
            Cache cache, ResponseDelivery delivery) {
        mCacheQueue = cacheQueue;
        mNetworkQueue = networkQueue;
        mCache = cache;
        mDelivery = delivery;
    }

    /**
     * 中断
     */
    public void quit() {
        mQuit = true;
        interrupt();
    }

    @Override
    public void run() {
        if (DEBUG) VolleyLog.v("start new dispatcher");
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // 缓存初始化，将磁盘中的数据读入内存
        mCache.initialize();

        while (true) {
            try {
                // Get a request from the cache triage queue, blocking until
                // at least one is available.从本地队列中取出请求
                final Request<?> request = mCacheQueue.take();
                request.addMarker("cache-queue-take");

                // If the request has been canceled, don't bother dispatching it.
                if (request.isCanceled()) {
                    request.finish("cache-discard-canceled");
                    continue;
                }

                //  从缓存中取出数据
                Cache.Entry entry = mCache.get(request.getCacheKey());
                if (entry == null) {
                    request.addMarker("cache-miss");
                    // .没有命中，就将请求放入网络
                    mNetworkQueue.put(request);
                    continue;
                }

                //  如果已经过期，将请求放入网络队列
                if (entry.isExpired()) {
                    request.addMarker("cache-hit-expired");
                    request.setCacheEntry(entry);
                    mNetworkQueue.put(request);
                    continue;
                }

                // 本地命中
                request.addMarker("cache-hit");
                Response<?> response = request.parseNetworkResponse(
                        new NetworkResponse(entry.data, entry.responseHeaders));
                request.addMarker("cache-hit-parsed");

                if (!entry.refreshNeeded()) {
                    //.命中并且不需要刷新
                    mDelivery.postResponse(request, response);
                } else {
                    // 命中 需要刷新，将请求放入网络队列
                    request.addMarker("cache-hit-refresh-needed");
                    request.setCacheEntry(entry);//先把entry暂存，如果发现数据一样的话就直接取这个值
                    response.intermediate = true;
                    mDelivery.postResponse(request, response, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mNetworkQueue.put(request);
                            } catch (InterruptedException e) {
                            }
                        }
                    });
                }

            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    return;
                }
                continue;
            }
        }
    }
}
