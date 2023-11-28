package com.windsbell.shardingkey.autofill.finder.cache;


import com.windsbell.shardingkey.autofill.strategy.ShardingValueStrategy;

/**
 * 分片键值对内容缓存器接口
 *
 * @author windbell
 */
public interface ShardingValueCache {

    /**
     * 获取
     */
    ShardingValueStrategy get(String key);

    /**
     * 添加
     */
    void put(String key, ShardingValueStrategy ShardingValueStrategy);

    /**
     * 移除
     */
    void remove(String key);

}
