package com.windsbell.shardingkey.autofill.finder;


import com.windsbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.ShardingValueStrategy;

/**
 * 分片键值对处理器
 *
 * @author windbell
 */
public interface ShardingValueHandler {

    /**
     * 查找分片键值对
     * BusinessKeyStrategy：业务分片键字段映射策略
     * ShardingValueFinder：分片键值查找器
     */
    ShardingValueStrategy doFind(BusinessKeyStrategy businessKeyStrategy, ShardingValueFinder shardingValueFinder);

}
