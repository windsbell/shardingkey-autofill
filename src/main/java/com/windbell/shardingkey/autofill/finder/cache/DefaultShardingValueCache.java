package com.windbell.shardingkey.autofill.finder.cache;

import com.windbell.shardingkey.autofill.strategy.ShardingValueStrategy;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;

/**
 * 分片键值对内容缓存器：本地（默认）
 *
 * @author windbell
 */
public class DefaultShardingValueCache implements ShardingValueCache {

    /**
     * cache使用软引用本地内存hashmap
     * key：业务字段和值组合key，可通过过对应内容找到对应分表、分库字段等值   value: <ShardingValueStrategy:分表键对应值,分库键对应值>
     */
    private final Map<String, ShardingValueStrategy> SHARDING_VALUE_CACHE = new ConcurrentReferenceHashMap<>();

    @Override
    public ShardingValueStrategy get(String key) {
        return SHARDING_VALUE_CACHE.get(key);
    }

    @Override
    public void put(String key, ShardingValueStrategy value) {
        SHARDING_VALUE_CACHE.put(key, value);
    }

    @Override
    public void remove(String key) {
        SHARDING_VALUE_CACHE.remove(key);
    }


}

