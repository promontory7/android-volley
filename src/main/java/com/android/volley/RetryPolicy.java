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
 * 请求重试策略接口
 */
public interface RetryPolicy {

    /**
     * 获取当前请求用时
     */
    public int getCurrentTimeout();

    /**
     * 重试的次数
     */
    public int getCurrentRetryCount();

    /**
     * 确定是否重试，参数为异常的具体信息，在请求异常时此接口会被调用，可以在此函数实现中抛出传入异常表示停止重试
     */
    public void retry(VolleyError error) throws VolleyError;
}
