package com.windsbell.shardingkey.autofill.config;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.windsbell.shardingkey.autofill.jsqlparser.TableNameFinder;
import com.windsbell.shardingkey.autofill.strategy.ShardingKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import lombok.Getter;
import net.sf.jsqlparser.statement.Statement;

import java.util.*;
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
    private final static Map<String, TableShardingKeyStrategy> TABLE_STRATEGY_CACHE = new HashMap<>();

    /**
     * 必要字段映射策略集合缓存 key：表名 value：分片键字段映射策略
     */
    private final static Map<String, List<ShardingKeyStrategy>> NECESSARY_TABLE_SHARDING_KEY_STRATEGY_CACHE = new HashMap<>();

    /**
     * 任意字段映射策略集合缓存 key：表名 value：分片键字段映射策略
     */
    private final static Map<String, List<ShardingKeyStrategy>> ANYONE_TABLE_SHARDING_KEY_STRATEGY_CACHE = new HashMap<>();

    private final static Set<String> necessaryBusinessKeySet = new HashSet<>();

    private final static Set<String> anyOneBusinessKeySet = new HashSet<>();

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

    protected static void putTableStrategy(String suitableTable, TableShardingKeyStrategy tableShardingStrategy) {
        TABLE_STRATEGY_CACHE.put(suitableTable, tableShardingStrategy);
    }

    protected static void putNecessaryBusinessStrategy(ShardingKeyStrategy shardingKeyStrategy, List<String> necessaryBusinessKeys) {
        parseBusiness(NECESSARY_TABLE_SHARDING_KEY_STRATEGY_CACHE, necessaryBusinessKeys, shardingKeyStrategy, necessaryBusinessKeySet);
    }

    protected static void putAnyOneBusinessStrategy(ShardingKeyStrategy shardingKeyStrategy, List<String> necessaryBusinessKeys) {
        parseBusiness(ANYONE_TABLE_SHARDING_KEY_STRATEGY_CACHE, necessaryBusinessKeys, shardingKeyStrategy, anyOneBusinessKeySet);
    }

    public static Map<String, TableShardingKeyStrategy> getTableStrategyMap() {
        return new HashMap<>(TABLE_STRATEGY_CACHE);
    }

    public static Map<String, List<ShardingKeyStrategy>> getNecessaryTableShardingKeyStrategyMap() {
        return new HashMap<>(NECESSARY_TABLE_SHARDING_KEY_STRATEGY_CACHE);
    }

    public static Map<String, List<ShardingKeyStrategy>> getAnyOneTableShardingKeyStrategyMap() {
        return new HashMap<>(ANYONE_TABLE_SHARDING_KEY_STRATEGY_CACHE);
    }

    private static void parseBusiness(Map<String, List<ShardingKeyStrategy>> tableShardingKeyStrategyMap, List<String> businessKeys, ShardingKeyStrategy shardingKeyStrategy, Set<String> businessKeySet) {
        if (CollectionUtils.isNotEmpty(businessKeys)) {
            for (String businessKey : businessKeys) {
                String concat = businessKey + shardingKeyStrategy.getDatabaseShardKey() + shardingKeyStrategy.getTableShardKey();
                if (!businessKeySet.contains(concat)) {
                    List<ShardingKeyStrategy> shardingKeyStrategyList = tableShardingKeyStrategyMap.getOrDefault(businessKey, new ArrayList<>());
                    shardingKeyStrategyList.add(shardingKeyStrategy);
                    tableShardingKeyStrategyMap.put(businessKey, shardingKeyStrategyList);
                    businessKeySet.add(concat);
                }
            }
        }
    }

}
