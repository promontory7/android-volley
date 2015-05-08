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
 * vollyĬ�ϵ�����ʵ����
 */
public class DefaultRetryPolicy implements RetryPolicy {
    /**
     * ��ʾ��ǰ���Ե�timeoutʱ��
     */
    private int mCurrentTimeoutMs;

    /**
     * �Ѿ����ԵĴ���
     */
    private int mCurrentRetryCount;

    /**
     * ����Դ���
     */
    private final int mMaxNumRetries;

    /** ÿ������֮ǰtimeout�ó��Ե�����*/
    private final float mBackoffMultiplier;

    /** Ĭ���ӳ�ʱ�� */
    public static final int DEFAULT_TIMEOUT_MS = 2500;

    /** Ĭ������Դ��� */
    public static final int DEFAULT_MAX_RETRIES = 0;

    /** Ĭ������֮ǰtimeout�ó��Ե����� */
    public static final float DEFAULT_BACKOFF_MULT = 1f;


    public DefaultRetryPolicy() {
        this(DEFAULT_TIMEOUT_MS, DEFAULT_MAX_RETRIES, DEFAULT_BACKOFF_MULT);
    }

    /**
     * ����һ���������Բ���
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
     * ȷ���Ƿ����ԣ�����Ϊ�쳣�ľ�����Ϣ���������쳣ʱ�˽ӿڻᱻ���ã������ڴ˺���ʵ�����׳������쳣��ʾֹͣ����
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
