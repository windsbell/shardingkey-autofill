package com.windsbell.shardingkey.autofill.finder.cache;


import com.windsbell.shardingkey.autofill.properties.CacheProperty;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * 分片键值对内容缓存实例工厂： 目前提供可支持:本地缓存（默认）、redis、spring cache
 *
 * @author windbell
 */
public class ShardingValueCacheFactory {

    private static final Long DEFAULT_EXPIRE = 60L; // 未配置过期时间则默认为60s

    // 分片键值对缓存装饰器
    private static ShardingValueCacheDecorator shardingValueCacheDecorator = null;

    public static ShardingValueCacheDecorator newInstance(Object[] args) {
        Object cachePropertyObj = args[0];
        long expire = DEFAULT_EXPIRE;
        String type = null;
        if (cachePropertyObj != null) {
            CacheProperty cacheProperty = (CacheProperty) cachePropertyObj;
            if (Objects.nonNull(cacheProperty.getExpire())) expire = cacheProperty.getExpire();
            if (Objects.nonNull(cacheProperty.getType())) type = cacheProperty.getType().trim();
        }
        ShardingValueCache shardingValueCache;
        Class<?> cacheClass;
        switch (CacheEnum.getCache(type)) {
            case REDIS:
                RedisConnectionFactory redisConnectionFactory = (RedisConnectionFactory) args[1];
                Assert.notNull(redisConnectionFactory, "未检查到应用装配redis,请配置后进行使用！");
                shardingValueCache = new RedisShardingValueCache(redisConnectionFactory, expire);
                cacheClass = shardingValueCache.getClass();
                break;
            case SPRING:
                CacheManager cacheManager = (CacheManager) args[2];
                Assert.notNull(cacheManager, "未检查到应用装配spring cache,请配置后进行使用！");
                SpringShardingValueCache springShardingValueCache = new SpringShardingValueCache(cacheManager);
                cacheClass = springShardingValueCache.getClass();
                shardingValueCache = new CustomerCacheDecorator(springShardingValueCache, expire);
                break;
            case DEFAULT:
            default:
                DefaultShardingValueCache defaultShardingValueCache = new DefaultShardingValueCache();
                cacheClass = defaultShardingValueCache.getClass();
                shardingValueCache = new CustomerCacheDecorator(defaultShardingValueCache, expire);
        }
        // 创建分片键值装饰器
        shardingValueCacheDecorator = new ShardingValueCacheDecorator(shardingValueCache, cacheClass);
        return shardingValueCacheDecorator;
    }

    protected static ShardingValueCacheDecorator getInstance() {
        Assert.notNull(shardingValueCacheDecorator, "未初始化分片键值对内容缓存实例!");
        return shardingValueCacheDecorator;
    }


}
