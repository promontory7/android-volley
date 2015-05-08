

package com.android.volley;

/**
 * 处理网络请求的封装类接口
 */
public interface Network {
    /**
     * Performs the specified request.
     * @param request 请求类
     * @return 数据和缓存数据
     */
    public NetworkResponse performRequest(Request<?> request) throws VolleyError;
}
