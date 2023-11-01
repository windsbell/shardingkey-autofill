package com.windbell.shardingkey.autofill.finder.cache;

import com.windbell.shardingkey.autofill.strategy.ShardingValueStrategy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.TimeUnit;

/**
 * 分片键值对内容缓存器：Redis
 *
 * @author windbell
 */
public class RedisShardingValueCache implements ShardingValueCache {

    /**
     * key过期时间
     */
    private final Long expire;

    /**
     * redisTemplate
     */
    private final RedisTemplate<String, ShardingValueStrategy> redisTemplate;

    RedisShardingValueCache(RedisConnectionFactory redisConnectionFactory, Long expire) {
        RedisTemplate<String, ShardingValueStrategy> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();
        this.redisTemplate = redisTemplate;
        this.expire = expire;
    }

    @Override
    public ShardingValueStrategy get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    @Override
    public void put(String key, ShardingValueStrategy ShardingValueStrategy) {
        redisTemplate.opsForValue().set(key, ShardingValueStrategy, expire, TimeUnit.SECONDS);
    }

    @Override
    public void remove(String key) {
        redisTemplate.delete(key);
    }

}
