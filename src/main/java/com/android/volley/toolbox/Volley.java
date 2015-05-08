

package com.android.volley.toolbox;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.android.volley.Network;
import com.android.volley.RequestQueue;

import java.io.File;

/**
 * 要用于创建一个请求队列，
 * 创建后调用start会启动线程，不断监听里面是否有请求
 *
 */
public class Volley {
    /**
     * 默认缓存目录
     * */
    private static final String DEFAULT_CACHE_DIR = "volley";

    /**
     * @param context
     * @param stack 网络请求方式
     * @param maxDiskCacheBytes 最大缓存， -1表示默认大小
     * @return 返回队列实例
     *
     * 创建一个默认请求队列，请求创建后，放在这个队列里面
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack, int maxDiskCacheBytes) {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);//缓存位置
        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (NameNotFoundException e) {
        }

        //若网络请求方式为空，则根据API版本号选择更加合适的请求方式
        //HurlStack 以 HttpURLConnection方式     HttpClientStack 以HttpClient方式
        if (stack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                stack = new HurlStack();
            } else {
                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
            }
        }
        Network network = new BasicNetwork(stack);//建立网络请求封装类，使用stack进行网络请求
        RequestQueue queue;

        //创建请求队列，传入缓存目录和网络请求
        if (maxDiskCacheBytes <= -1)
        {
        	queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        }
        else
        {
        	queue = new RequestQueue(new DiskBasedCache(cacheDir, maxDiskCacheBytes), network);
        }

        queue.start();//启动请求队列，请求队列会一直监听是否有请求加进来，有的话就执行
        return queue;
    }


    public static RequestQueue newRequestQueue(Context context, int maxDiskCacheBytes) {
        return newRequestQueue(context, null, maxDiskCacheBytes);
    }
    
    public static RequestQueue newRequestQueue(Context context, HttpStack stack)
    {
    	return newRequestQueue(context, stack, -1);
    }
    
    public static RequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, null);
    }

}

