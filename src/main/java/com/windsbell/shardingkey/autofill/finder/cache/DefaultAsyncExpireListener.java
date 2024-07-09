package com.windsbell.shardingkey.autofill.finder.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 默认异步过期监听器
 *
 * @author windbell
 */
@Slf4j
public class DefaultAsyncExpireListener extends AbstractAsyncExpireListener {

    private final static long INTERVAL_INSPECTION_TIME = 5; // 间隔检查时间（s）

    private final static float INSPECTION_RATIO = 0.25f; // 抽查比例系数

    private final static String ASYNC_EXPIRE_LISTENER_NAME = "asyncExpireListener"; // 异步过期监听器名称

    /**
     * 启用核心守护线程作为过期监听器
     */
    private final static ScheduledExecutorService asyncExpireListener = Executors.newSingleThreadScheduledExecutor(r -> {
        final Thread t = new Thread(r, ASYNC_EXPIRE_LISTENER_NAME);
        t.setDaemon(true);
        if (Thread.NORM_PRIORITY != t.getPriority()) t.setPriority(Thread.NORM_PRIORITY);
        log.info("load asyncExpireListener: {}", DefaultAsyncExpireListener.class.getName());
        return t;
    });

    @Override
    public boolean isExpire(ExpireKey expireKey) {
        return System.currentTimeMillis() > expireKey.getExpire();
    }

    @Override
    public void checkExpireBeforeGet(String key) {
        ExpireKey expireKey = expireMap.get(key);
        if (expireKey != null && this.isExpire(expireKey)) {
            this.removeExpire(expireKey);
        }
    }

    @Override
    public void removeExpire(ExpireKey expireKey) {
        expireMap.remove(expireKey.getKey());
        cache.remove(expireKey.getKey());
    }

    @Override
    public void put(ExpireKey expireKey) {
        expireQueue.offer(expireKey);
        expireMap.put(expireKey.getKey(), expireKey);
    }

    @Override
    public void setCache(ShardingValueCache cache) {
        super.cache = cache;
    }

    @Override
    public void startListening() {
        asyncExpireListener.scheduleAtFixedRate(() -> {
            if (!expireQueue.isEmpty()) {
                // 检查过期队列一定比例数量的内容是否过期。对于过期的清理掉缓存，未过期的重新置入队列
                long inspectionNum = expireQueue.size();
                if (inspectionNum > INITIAL_CAPACITY) {
                    inspectionNum = new Random().nextInt((int) (inspectionNum * INSPECTION_RATIO));
                }
                for (int i = 0; i < inspectionNum; i++) {
                    ExpireKey expireKey = expireQueue.poll();
                    if (expireKey == null) break; // 队列不再有元素 结束循环
                    if (this.isExpire(expireKey)) {
                        this.removeExpire(expireKey);
                        log.info("remove expire key:{}", expireKey.getKey());
                    } else {
                        // 未过期则重新放入队列，同时后面元素也是未过期，直接结束本轮检查
                        expireQueue.offer(expireKey);
                        break;
                    }
                }

            }
        }, INTERVAL_INSPECTION_TIME, INTERVAL_INSPECTION_TIME, TimeUnit.SECONDS);
    }

}