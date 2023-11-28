package com.windsbell.shardingkey.autofill.strategy;

import lombok.Data;

/**
 * 分片键对应字段值映射策略
 *
 * @author windbell
 */
@Data
public class ShardingValueStrategy {

    private String tableShardValue;   // 分表键对应值

    private String databaseShardValue; // 分库键对应值
}
