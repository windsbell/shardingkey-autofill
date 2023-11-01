package com.windbell.shardingkey.autofill.finder.cache;

/**
 * 异步监听器接口
 *
 * @author windbell
 */
interface AsyncExpireListener {

    /**
     * 是否过期
     */
    boolean isExpire(ExpireKey expireKey);

    /**
     * 获取前检查
     */
    void checkExpireBeforeGet(String key);

    /**
     * 移除过期
     */
    void removeExpire(ExpireKey expireKey);

    /**
     * 添加
     */
    void put(ExpireKey expireKey);

    /**
     * 设置缓存器
     */
    void setCache(ShardingValueCache cache);

    /**
     * 启动监听
     */
    void startListening();

}