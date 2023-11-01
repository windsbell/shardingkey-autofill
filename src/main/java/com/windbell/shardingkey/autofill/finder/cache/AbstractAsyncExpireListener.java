package com.windbell.shardingkey.autofill.finder.cache;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 抽象异步过期监听器
 *
 * @author windbell
 */
public abstract class AbstractAsyncExpireListener implements AsyncExpireListener {

    // 过期队列初始容量
    protected final static int INITIAL_CAPACITY = 1000;

    // 过期队列：淘汰算法为移除最先过期的key元素
    protected static final PriorityQueue<ExpireKey> expireQueue = new PriorityQueue<>(INITIAL_CAPACITY, Comparator.comparingLong(ExpireKey::getExpire));

    // 过期key储存map
    protected static final ConcurrentHashMap<String, ExpireKey> expireMap = new ConcurrentHashMap<>();

    // 分片键值对内容缓存器
    protected ShardingValueCache cache;


}