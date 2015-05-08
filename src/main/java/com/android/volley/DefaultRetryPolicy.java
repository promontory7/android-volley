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

/**
 * volly默认的重试实现类
 */
public class DefaultRetryPolicy implements RetryPolicy {
    /**
     * 表示当前重试的timeout时间
     */
    private int mCurrentTimeoutMs;

    /**
     * 已经重试的次数
     */
    private int mCurrentRetryCount;

    /**
     * 最大尝试次数
     */
    private final int mMaxNumRetries;

    /** 每次重试之前timeout该乘以的因子*/
    private final float mBackoffMultiplier;

    /** 默认延迟时间 */
    public static final int DEFAULT_TIMEOUT_MS = 2500;

    /** 默认最大尝试次数 */
    public static final int DEFAULT_MAX_RETRIES = 0;

    /** 默认重试之前timeout该乘以的因子 */
    public static final float DEFAULT_BACKOFF_MULT = 1f;


    public DefaultRetryPolicy() {
        this(DEFAULT_TIMEOUT_MS, DEFAULT_MAX_RETRIES, DEFAULT_BACKOFF_MULT);
    }

    /**
     * 创建一个请求重试策略
     */
    public DefaultRetryPolicy(int initialTimeoutMs, int maxNumRetries, float backoffMultiplier) {
        mCurrentTimeoutMs = initialTimeoutMs;
        mMaxNumRetries = maxNumRetries;
        mBackoffMultiplier = backoffMultiplier;
    }


    @Override
    public int getCurrentTimeout() {
        return mCurrentTimeoutMs;
    }

    @Override
    public int getCurrentRetryCount() {
        return mCurrentRetryCount;
    }

    public float getBackoffMultiplier() {
        return mBackoffMultiplier;
    }

    /**
     * 确定是否重试，参数为异常的具体信息，在请求异常时此接口会被调用，可以在此函数实现中抛出传入异常表示停止重试
     */
    @Override
    public void retry(VolleyError error) throws VolleyError {
        mCurrentRetryCount++;
        mCurrentTimeoutMs += (mCurrentTimeoutMs * mBackoffMultiplier);
        if (!hasAttemptRemaining()) {
            throw error;
        }
    }


    protected boolean hasAttemptRemaining() {
        return mCurrentRetryCount <= mMaxNumRetries;
    }
}
