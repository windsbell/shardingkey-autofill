package com.windbell.shardingkey.autofill.strategy;

import lombok.Data;

/**
 * 分片键字段映射策略
 *
 * @author windbell
 */
@Data
public class ShardingKeyStrategy {

    private String tableShardKey;   // 分表键

    private String databaseShardKey; // 分库键

}
