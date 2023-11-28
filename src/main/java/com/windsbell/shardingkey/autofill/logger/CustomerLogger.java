package com.windsbell.shardingkey.autofill.logger;

public interface CustomerLogger {

    void info(String var1);

    void info(String var1, Object var2);

    void info(String var1, Object var2, Object var3);

    void info(String var1, Object... var2);

    void info(String var1, Throwable var2);

    void debug(String var1);

    void debug(String var1, Object var2);

    void debug(String var1, Object var2, Object var3);

    void debug(String var1, Object... var2);

    void debug(String var1, Throwable var2);

    void error(String var1);

    void error(String var1, Object var2);

    void error(String var1, Object var2, Object var3);

    void error(String var1, Object... var2);

    void error(String var1, Throwable var2);

    void warn(String var1);

    void warn(String var1, Object var2);

    void warn(String var1, Object var2, Object var3);

    void warn(String var1, Object... var2);

    void warn(String var1, Throwable var2);
}
