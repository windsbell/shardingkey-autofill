package com.windsbell.shardingkey.autofill.finder.cache;


import com.windsbell.shardingkey.autofill.strategy.ShardingValueStrategy;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * 定制缓存器
 *
 * @author windbell
 */
public class CustomerCacheDecorator implements ShardingValueCache {

    /**
     * key过期时间
     */
    private final Long expire;

    /**
     * customer cache
     */
    private final ShardingValueCache cache;

    /**
     * 异步监听器
     */
    private final AsyncExpireListener asyncExpireListener;


    public CustomerCacheDecorator(ShardingValueCache shardingValueCache, Long expire) {
        this.expire = expire * 1000;
        asyncExpireListener = getFromServices();
        cache = shardingValueCache;
        asyncExpireListener.setCache(cache);
        asyncExpireListener.startListening();
    }

    private AsyncExpireListener getFromServices() {
        ServiceLoader<AsyncExpireListener> serviceLoader = ServiceLoader.load(AsyncExpireListener.class);
        Iterator<AsyncExpireListener> iterator = serviceLoader.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return new DefaultAsyncExpireListener(); // 默认异步过期监听器
    }

    @Override
    public ShardingValueStrategy get(String key) {
        asyncExpireListener.checkExpireBeforeGet(key);
        return cache.get(key);
    }

    @Override
    public void put(String key, ShardingValueStrategy ShardingValueStrategy) {
        cache.put(key, ShardingValueStrategy);
        asyncExpireListener.put(new ExpireKey(key, System.currentTimeMillis() + expire));
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

}