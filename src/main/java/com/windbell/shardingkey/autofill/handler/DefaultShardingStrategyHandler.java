package com.windbell.shardingkey.autofill.handler;

import com.windbell.shardingkey.autofill.config.ShardingStrategyHandlerFactory;
import com.windbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import net.sf.jsqlparser.statement.Statement;

/**
 * 默认分片键策略
 * 也可以自定义分片键策略，继承AbstractShardingParserHandler即可
 *
 * @author windbell
 */
public class DefaultShardingStrategyHandler extends AbstractShardingStrategyHandler {

    /**
     * 默认分片键策略【wrapper分片键策略+mapper分片键策略一起】
     */
    @Override
    public void parse(Statement statement, Object parameterObject, TableShardingKeyStrategy tableShardingKeyStrategy) {
        ShardingStrategyHandlerFactory.getAllInstances()
                .forEach(shardingStrategyHandler -> shardingStrategyHandler.parse(statement, parameterObject, tableShardingKeyStrategy));
    }


}
