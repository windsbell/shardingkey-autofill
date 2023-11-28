package com.windsbell.shardingkey.autofill.logger;

import org.slf4j.Logger;

/**
 * 手动代理 Logger,增加开关判断
 */
public class CustomerLoggerImpl implements CustomerLogger {

    // sl4j日志门面
    protected Logger log;

    // 打印开关
    private final boolean logEnabled;

    CustomerLoggerImpl(Logger log, boolean logEnabled) {
        this.log = log;
        this.logEnabled = logEnabled;
    }

    public void info(String var1) {
        if (logEnabled) {
            log.info(var1);
        }
    }

    @Override
    public void info(String var1, Object var2) {
        if (logEnabled) {
            log.info(var1, var2);
        }
    }

    @Override
    public void info(String var1, Object var2, Object var3) {
        if (logEnabled) {
            log.info(var1, var2, var3);
        }
    }

    @Override
    public void info(String var1, Object... var2) {
        if (logEnabled) {
            log.info(var1, var2);
        }
    }

    @Override
    public void info(String var1, Throwable var2) {
        if (logEnabled) {
            log.info(var1, var2);
        }
    }

    @Override
    public void debug(String var1) {
        if (logEnabled) {
            log.debug(var1);
        }
    }

    @Override
    public void debug(String var1, Object var2) {
        if (logEnabled) {
            log.debug(var1, var2);
        }
    }

    @Override
    public void debug(String var1, Object var2, Object var3) {
        if (logEnabled) {
            log.debug(var1, var2, var3);
        }
    }

    @Override
    public void debug(String var1, Object... var2) {
        if (logEnabled) {
            log.debug(var1, var2);
        }
    }

    @Override
    public void debug(String var1, Throwable var2) {
        if (logEnabled) {
            log.debug(var1, var2);
        }
    }


    @Override
    public void error(String var1) {
        if (logEnabled) {
            log.error(var1);
        }
    }

    @Override
    public void error(String var1, Object var2) {
        if (logEnabled) {
            log.error(var1, var2);
        }
    }

    @Override
    public void error(String var1, Object var2, Object var3) {
        if (logEnabled) {
            log.error(var1, var2, var3);
        }
    }

    @Override
    public void error(String var1, Object... var2) {
        if (logEnabled) {
            log.error(var1, var2);
        }
    }

    @Override
    public void error(String var1, Throwable var2) {
        if (logEnabled) {
            log.error(var1, var2);
        }
    }

    @Override
    public void warn(String var1) {
        if (logEnabled) {
            log.warn(var1);
        }
    }

    @Override
    public void warn(String var1, Object var2) {
        if (logEnabled) {
            log.warn(var1, var2);
        }
    }

    @Override
    public void warn(String var1, Object var2, Object var3) {
        if (logEnabled) {
            log.warn(var1, var2, var3);
        }
    }

    @Override
    public void warn(String var1, Object... var2) {
        if (logEnabled) {
            log.warn(var1, var2);
        }
    }

    @Override
    public void warn(String var1, Throwable var2) {
        if (logEnabled) {
            log.warn(var1, var2);
        }
    }

}
