

package com.android.volley;

/**
 * ������������ķ�װ��ӿ�
 */
public interface Network {
    /**
     * Performs the specified request.
     * @param request ������
     * @return ���ݺͻ�������
     */
    public NetworkResponse performRequest(Request<?> request) throws VolleyError;
}
