package com.windsbell.shardingkey.autofill.finder.cache;

import com.windsbell.shardingkey.autofill.strategy.ShardingValueStrategy;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;


/**
 * 分片键值对内容缓存器：Spring cache（自定义过期时间，同应用依赖装载的spring cache bean配置的过期时间会相互干扰，建议配置一致！）
 *
 * @author windbell
 */
public class SpringShardingValueCache implements ShardingValueCache {

    private final static String CACHE_NAME = "shardingValueStrategy";

    private final Cache cache;

    SpringShardingValueCache(CacheManager cacheManager) {
        cache = cacheManager.getCache(CACHE_NAME);
    }

    @Override
    public ShardingValueStrategy get(String key) {
        return cache.get(key, ShardingValueStrategy.class);
    }

    @Override
    public void put(String key, ShardingValueStrategy ShardingValueStrategy) {
        cache.put(key, ShardingValueStrategy);
    }

    @Override
    public void remove(String key) {
        cache.evictIfPresent(key);
    }


}







