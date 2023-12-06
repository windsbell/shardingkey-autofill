package com.windsbell.shardingkey.autofill.finder;

import com.windsbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.ShardingValueStrategy;

/**
 * 分片键值对默认处理器
 *
 * @author windbell
 */
public class DefaultShardingHandler implements ShardingValueHandler {


    /**
     * 公共方法：通过业务键策略，使用分片键查找器查到对应分片键值内容(直接调用，不置入缓存)
     */
    public ShardingValueStrategy doFind(BusinessKeyStrategy businessKeyStrategy, ShardingValueFinder shardingValueFinder) {
        return shardingValueFinder.find(businessKeyStrategy);
    }


}
