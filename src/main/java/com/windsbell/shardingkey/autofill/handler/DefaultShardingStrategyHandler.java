package com.windsbell.shardingkey.autofill.handler;

import com.windsbell.shardingkey.autofill.config.ShardingStrategyHandlerFactory;
import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import net.sf.jsqlparser.statement.Statement;

import java.util.List;

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
    public void parse(Statement statement, Object parameterObject, List<TableShardingKeyStrategy> tableShardingKeyStrategyList) {
        ShardingStrategyHandlerFactory.getAllInstances()
                .stream().filter(shardingStrategyHandler -> shardingStrategyHandler.getClass().getAnnotation(Deprecated.class) == null) // 过滤出有效的处理器
                .forEach(shardingStrategyHandler -> shardingStrategyHandler.parse(statement, parameterObject, tableShardingKeyStrategyList));
    }


}
