package jp.co.cyberagent.android.gpuimage.sample.utils;

import android.text.TextUtils;

/**
 * 数字转换工具类
 *
 * @author Benhero
 */
public class NumberUtils {

    public static long getLong(String value) {
        if (TextUtils.isEmpty(value)) {
            return 0;
        }
        return Long.valueOf(value);
    }

    public static int getInteger(String value) {
        if (TextUtils.isEmpty(value)) {
            return 0;
        }
        return Integer.valueOf(value);
    }
}
