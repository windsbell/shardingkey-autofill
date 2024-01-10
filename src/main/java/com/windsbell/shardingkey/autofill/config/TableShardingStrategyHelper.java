package com.windsbell.shardingkey.autofill.config;

import com.windsbell.shardingkey.autofill.jsqlparser.TableNameFinder;
import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import net.sf.jsqlparser.statement.Statement;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * 表分片键映射策略助手
 *
 * @author windbell
 */
public class TableShardingStrategyHelper {

    /**
     * 表分片键策略缓存 key：表名 value：表策略
     */
    private final static ConcurrentHashMap<String, TableShardingKeyStrategy> TABLE_STRATEGY_CACHE = new ConcurrentHashMap<>();

    /**
     * statement:通过预处理语句，获取对应表的分片键策略
     */
    public static List<TableShardingKeyStrategy> find(Statement statement) {
        TableNameFinder tableNameFinder = new TableNameFinder(statement);
        return tableNameFinder.getTableList()
                .stream().map(TABLE_STRATEGY_CACHE::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected static void put(String suitableTable, TableShardingKeyStrategy tableShardingStrategy) {
        TABLE_STRATEGY_CACHE.put(suitableTable, tableShardingStrategy);
    }

    protected static ConcurrentHashMap<String, TableShardingKeyStrategy> showALl() {
        return new ConcurrentHashMap<>(TABLE_STRATEGY_CACHE);
    }

}
