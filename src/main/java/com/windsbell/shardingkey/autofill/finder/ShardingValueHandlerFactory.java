package com.windsbell.shardingkey.autofill.finder;


import com.windsbell.shardingkey.autofill.finder.cache.*;
import com.windsbell.shardingkey.autofill.properties.ShardingValueCacheProperty;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * 分片键值对内容处理器实例工厂
 * DefaultShardingHandler 默认：直接使用分片键值对内容查找器
 * ShardingValueCachedHandler：使用缓存的分片键值对内容处理器 目前提供可支持:本地缓存（默认）、redis、spring cache
 *
 * @author windbell
 */
public class ShardingValueHandlerFactory {

    private static final Long DEFAULT_EXPIRE = 60L; // 未配置过期时间则默认为60s

    // 分片键值对内容处理器
    private static ShardingValueHandler shardingValueHandler = initDefaultHandlerInstance();

    // 初始化分片键值对内容处理器实例
    public static ShardingValueHandler initInstance(Object[] args) {
        Object cachePropertyObj = args[0];
        if (cachePropertyObj != null) {
            ShardingValueCacheProperty shardingValueCacheProperty = (ShardingValueCacheProperty) cachePropertyObj;
            if (Objects.nonNull(shardingValueCacheProperty.getEnabled()) && shardingValueCacheProperty.getEnabled()) {
                initCachedFinderInstance(args, shardingValueCacheProperty);
            }
        }
        return shardingValueHandler;
    }

    private static ShardingValueHandler initDefaultHandlerInstance() {
        return new DefaultShardingHandler();
    }

    private static void initCachedFinderInstance(Object[] args, ShardingValueCacheProperty shardingValueCacheProperty) {
        long expire = DEFAULT_EXPIRE;
        String type = null;
        if (Objects.nonNull(shardingValueCacheProperty.getExpire())) expire = shardingValueCacheProperty.getExpire();
        if (Objects.nonNull(shardingValueCacheProperty.getType())) type = shardingValueCacheProperty.getType().trim();
        ShardingValueCache shardingValueCache;
        Class<?> cacheClass;
        switch (CacheEnum.getCache(type)) {
            case REDIS:
                RedisConnectionFactory redisConnectionFactory = (RedisConnectionFactory) args[1];
                Assert.notNull(redisConnectionFactory, "application assembly not inspected: spring.redis" +
                        ",please configure it before using it！");
                shardingValueCache = new RedisShardingValueCache(redisConnectionFactory, expire);
                cacheClass = shardingValueCache.getClass();
                break;
            case SPRING:
                CacheManager cacheManager = (CacheManager) args[2];
                Assert.notNull(cacheManager, "application assembly not inspected: spring cache" +
                        ",please configure it before using it！");
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
        shardingValueHandler = new ShardingValueCachedHandler(shardingValueCache, cacheClass);
    }

    protected static ShardingValueHandler getHandler() {
        return shardingValueHandler;
    }


}
