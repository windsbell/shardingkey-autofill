package com.windsbell.shardingkey.autofill.handler;

import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import net.sf.jsqlparser.statement.Statement;

import java.util.List;

/**
 * 自动填充分片键策略接口
 *
 * @author windbell
 */
public interface ShardingStrategyHandler {

    /**
     * statement: 预处理语句
     * parameterObject: 替换参数对象
     * tableShardingKeyStrategyList: 表分片键映射策略 （单表一条，多表多条）
     */
    void parse(Statement statement, Object parameterObject, List<TableShardingKeyStrategy> tableShardingKeyStrategyList);

}
