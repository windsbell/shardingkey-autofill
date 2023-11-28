package com.windsbell.shardingkey.autofill.finder;


import com.windsbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.ShardingValueStrategy;
import org.springframework.lang.Nullable;


/**
 * 分片键值查找器接口
 *
 * @author windbell
 */
public interface ShardingValueFinder {

    /**
     * 可通过业务关键字段，检索出分表、分库字段值
     * 入参：BusinessKeyStrategy [1.shardingKeyStrategy：分片键字段映射策略 （分表键、分库键） 2.necessaryBusinessKeys：必要业务键列表 3.anyOneBusinessKeys：任意业务键列表]
     * 响应：ShardingValueStrategy [tableShardValue：分表建的值 databaseShardValue：分库键的值]
     */
    @Nullable
    ShardingValueStrategy apply(BusinessKeyStrategy businessKeyStrategy);


}