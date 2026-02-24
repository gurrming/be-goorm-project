package com.example.heartbit.util;

public class RedisKeyUtils {
    public static String getTickerKey(Long categoryId) {
        return "ticker:" + categoryId;
    }
}
