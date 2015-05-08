/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley;

import android.net.TrafficStats;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import com.android.volley.VolleyLog.MarkerLog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

/**
 * 网络请求抽象类，
 * 包含请求URL 请求方式 请求Header 请求Body 请求的优先级等信息
 */
public abstract class Request<T> implements Comparable<Request<T>> {

    /**
     * 默认编码方式
     */
    private static final String DEFAULT_PARAMS_ENCODING = "UTF-8";

    /**
     * 支持的请求方式.
     */
    public interface Method {
        int DEPRECATED_GET_OR_POST = -1;
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        int HEAD = 4;
        int OPTIONS = 5;
        int TRACE = 6;
        int PATCH = 7;
    }

    private final MarkerLog mEventLog = MarkerLog.ENABLED ? new MarkerLog() : null;
    private final int mMethod;
    private final String mUrl;
    private String mRedirectUrl;
    /**
     * 请求的唯一标识
     */
    private String mIdentifier;
    private final int mDefaultTrafficStatsTag;
    private final Response.ErrorListener mErrorListener;
    /**
     * 请求在队列中的顺序
     */
    private Integer mSequence;
    private RequestQueue mRequestQueue;
    private boolean mShouldCache = true;
    /**
     * 网络和本地线程都会对这个进行判断，如果取消了就不请求
     */
    private boolean mCanceled = false;
    /**
     * 是否已经把数据发送到UI */
    private boolean mResponseDelivered = false;

    // A cheap variant of request tracing used to dump slow requests.
    private long mRequestBirthTime = 0;

    /** Threshold at which we should log the request (even when debug logging is not enabled). */
    private static final long SLOW_REQUEST_THRESHOLD_MS = 3000;

    /**
     * 请求策略，例如设置最大重试次数 */
    private RetryPolicy mRetryPolicy;

    /**
     * 当请求存在缓存，但是需要更新的情况下，先暂时把缓存放在这里，如果发现数据没有修改，直接读取这个数据
     */
    private Cache.Entry mCacheEntry = null;

    /** An opaque token tagging this request; used for bulk cancellation. */
    private Object mTag;


    @Deprecated
    public Request(String url, Response.ErrorListener listener) {
        this(Method.DEPRECATED_GET_OR_POST, url, listener);
    }

    /**
     * 创建请求实例
     * @param method 请求方式
     * @param url 请求URL
     * @param listener 请求错误的Listener
     */
    public Request(int method, String url, Response.ErrorListener listener) {
        mMethod = method;
        mUrl = url;
        mIdentifier = createIdentifier(method, url);
        mErrorListener = listener;
        setRetryPolicy(new DefaultRetryPolicy());
        mDefaultTrafficStatsTag = findDefaultTrafficStatsTag(url);
    }


    public int getMethod() {
        return mMethod;
    }

    /**
     * 设置这个请求的mTag，用于取消请求的标志
     */
    public Request<?> setTag(Object tag) {
        mTag = tag;
        return this;
    }

    public Object getTag() {
        return mTag;
    }

    public Response.ErrorListener getErrorListener() {
        return mErrorListener;
    }


    public int getTrafficStatsTag() {
        return mDefaultTrafficStatsTag;
    }

    /**
     * @return The hashcode of the URL's host component, or 0 if there is none.
     */
    private static int findDefaultTrafficStatsTag(String url) {
        if (!TextUtils.isEmpty(url)) {
            Uri uri = Uri.parse(url);
            if (uri != null) {
                String host = uri.getHost();
                if (host != null) {
                    return host.hashCode();
                }
            }
        }
        return 0;
    }

    /**
     *设置重试策略
     */
    public Request<?> setRetryPolicy(RetryPolicy retryPolicy) {
        mRetryPolicy = retryPolicy;
        return this;
    }


    public void addMarker(String tag) {
        if (MarkerLog.ENABLED) {
            mEventLog.add(tag, Thread.currentThread().getId());
        } else if (mRequestBirthTime == 0) {
            mRequestBirthTime = SystemClock.elapsedRealtime();
        }
    }

    void finish(final String tag) {
        if (mRequestQueue != null) {
            mRequestQueue.finish(this);
        }
        if (MarkerLog.ENABLED) {
            final long threadId = Thread.currentThread().getId();
            if (Looper.myLooper() != Looper.getMainLooper()) {
                // If we finish marking off of the main thread, we need to
                // actually do it on the main thread to ensure correct ordering.
                Handler mainThread = new Handler(Looper.getMainLooper());
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mEventLog.add(tag, threadId);
                        mEventLog.finish(this.toString());
                    }
                });
                return;
            }

            mEventLog.add(tag, threadId);
            mEventLog.finish(this.toString());
        } else {
            long requestTime = SystemClock.elapsedRealtime() - mRequestBirthTime;
            if (requestTime >= SLOW_REQUEST_THRESHOLD_MS) {
                VolleyLog.d("%d ms: %s", requestTime, this.toString());
            }
        }
    }


    public Request<?> setRequestQueue(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
        return this;
    }


    public final Request<?> setSequence(int sequence) {
        mSequence = sequence;
        return this;
    }


    public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }


    public String getUrl() {
        return (mRedirectUrl != null) ? mRedirectUrl : mUrl;
    }


    public String getOriginUrl() {
    	return mUrl;
    }


    public String getIdentifier() {
        return mIdentifier;
    }


    public void setRedirectUrl(String redirectUrl) {
    	mRedirectUrl = redirectUrl;
    }


    public String getCacheKey() {
        return getUrl();
    }


    public Request<?> setCacheEntry(Cache.Entry entry) {
        mCacheEntry = entry;
        return this;
    }


    public Cache.Entry getCacheEntry() {
        return mCacheEntry;
    }


    public void cancel() {
        mCanceled = true;
    }


    public boolean isCanceled() {
        return mCanceled;
    }

    /**
     * 返回一系列 HTTP 信息头
     */
    public Map<String, String> getHeaders() throws AuthFailureError {
        return Collections.emptyMap();
    }

    /**
     * 返回POST方式的参数
     */
    @Deprecated
    protected Map<String, String> getPostParams() throws AuthFailureError {
        return getParams();
    }


    @Deprecated
    protected String getPostParamsEncoding() {
        return getParamsEncoding();
    }

    @Deprecated
    public String getPostBodyContentType() {
        return getBodyContentType();
    }


    @Deprecated
    public byte[] getPostBody() throws AuthFailureError {
        // Note: For compatibility with legacy clients of volley, this implementation must remain
        // here instead of simply calling the getBody() function because this function must
        // call getPostParams() and getPostParamsEncoding() since legacy clients would have
        // overridden these two member functions for POST requests.
        Map<String, String> postParams = getPostParams();
        if (postParams != null && postParams.size() > 0) {
            return encodeParameters(postParams, getPostParamsEncoding());
        }
        return null;
    }

    protected Map<String, String> getParams() throws AuthFailureError {
        return null;
    }


    protected String getParamsEncoding() {
        return DEFAULT_PARAMS_ENCODING;
    }


    public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
    }


    public byte[] getBody() throws AuthFailureError {
        Map<String, String> params = getParams();
        if (params != null && params.size() > 0) {
            return encodeParameters(params, getParamsEncoding());
        }
        return null;
    }

    private byte[] encodeParameters(Map<String, String> params, String paramsEncoding) {
        StringBuilder encodedParams = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
                encodedParams.append('&');
            }
            return encodedParams.toString().getBytes(paramsEncoding);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
        }
    }

    public final Request<?> setShouldCache(boolean shouldCache) {
        mShouldCache = shouldCache;
        return this;
    }

    public final boolean shouldCache() {
        return mShouldCache;
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }

    public Priority getPriority() {
        return Priority.NORMAL;
    }

    public final int getTimeoutMs() {
        return mRetryPolicy.getCurrentTimeout();
    }

    public RetryPolicy getRetryPolicy() {
        return mRetryPolicy;
    }

    public void markDelivered() {
        mResponseDelivered = true;
    }

    public boolean hasHadResponseDelivered() {
        return mResponseDelivered;
    }

    /**
     * 解析原生的network response成Response
     */
    abstract protected Response<T> parseNetworkResponse(NetworkResponse response);


    protected VolleyError parseNetworkError(VolleyError volleyError) {
        return volleyError;
    }

    /**
     * 将response发送到UI
     */
    abstract protected void deliverResponse(T response);


    public void deliverError(VolleyError error) {
        if (mErrorListener != null) {
            mErrorListener.onErrorResponse(error);
        }
    }

    /**
     * Our comparator sorts from high to low priority, and secondarily by
     * sequence number to provide FIFO ordering.
     */
    @Override
    public int compareTo(Request<T> other) {
        Priority left = this.getPriority();
        Priority right = other.getPriority();
        // High-priority requests are "lesser" so they are sorted to the front.
        // Equal priorities are sorted by sequence number to provide FIFO ordering.
        return left == right ?
                this.mSequence - other.mSequence :
                right.ordinal() - left.ordinal();
    }

    @Override
    public String toString() {
        String trafficStatsTag = "0x" + Integer.toHexString(getTrafficStatsTag());
        return (mCanceled ? "[X] " : "[ ] ") + getUrl() + " " + trafficStatsTag + " "
                + getPriority() + " " + mSequence;
    }

    private static long sCounter;
    /**
     *  sha1(Request:method:url:timestamp:counter)
     * @param method http method
     * @param url               http request url
     * @return sha1 hash string
     */
    private static String createIdentifier(final int method, final String url) {
        return InternalUtils.sha1Hash("Request:" + method + ":" + url +
                ":" + System.currentTimeMillis() + ":" + (sCounter++));
    }
}
