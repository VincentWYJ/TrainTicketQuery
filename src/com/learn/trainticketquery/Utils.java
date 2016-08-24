package com.learn.trainticketquery;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class Utils {
	private static final String TAG = "MainActivity";

    public static Context mContext;
    
    public static boolean mIsBackFromSetNetwork;

    /*
     * 判断网络连接情况
     */
    public static boolean isNetWorkConnected(){
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    /*
     * Toast
     */
    public static void showToast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    /*
     * Log
     */
    public static void showLog(Object message) {
        Log.i(TAG, ""+message);
    }
}
