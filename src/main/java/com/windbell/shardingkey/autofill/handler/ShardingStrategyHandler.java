package com.windbell.shardingkey.autofill.handler;

import net.sf.jsqlparser.statement.Statement;

/**
 * 自动填充分片键策略接口
 *
 * @author windbell
 */
public interface ShardingStrategyHandler {

    /**
     * statement: 预处理语句
     * parameterObject: 替换参数对象
     */
    void parse(Statement statement, Object parameterObject);

}
