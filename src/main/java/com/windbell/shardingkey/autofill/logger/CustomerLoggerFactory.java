package com.windbell.shardingkey.autofill.logger;

import org.slf4j.LoggerFactory;

public class CustomerLoggerFactory {

    // 打印开关
    private static boolean logEnabled = true;

    // 设置开关
    public static void init(Boolean logEnabled) {
        if (logEnabled != null) {
            CustomerLoggerFactory.logEnabled = logEnabled;
        }
    }

    public static CustomerLogger getLogger(Class<?> clazz) {
        return new CustomerLoggerImpl(LoggerFactory.getLogger(clazz), logEnabled);
    }

}
