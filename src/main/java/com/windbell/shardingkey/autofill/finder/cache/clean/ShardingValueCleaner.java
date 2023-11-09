package com.windbell.shardingkey.autofill.finder.cache.clean;


import com.windbell.shardingkey.autofill.finder.cache.ShardingValueCacheDecorator;
import com.windbell.shardingkey.autofill.finder.cache.ShardingValueCacheFactory;
import com.windbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 分片键值对缓存清理器
 * 注：提供业务使用者自行主动清理分片键值对缓存，
 * （适用于场景：当键值对在业务中发生变化，不再关联时，急切需要及时同步移除，避免在后续SQL流程中自动填充了旧的且无效分片键内容）
 * 注：清理之后，在后续SQL流程中会重新通过分片键查找器进行查找后，并将最新分片键值对内容再置入cache中管理; 所以除了上面场景，其他情况下
 * 大可不必自行清理，以避免额外消耗查找器资源，因为在策略集配置的过期时间达到后，对应cache同样会自动被回收清理
 *
 * @author windbell
 */
public class ShardingValueCleaner extends ShardingValueCacheFactory {

    // 清理
    public static void clear(BusinessKeyStrategy businessKeyStrategy) {
        ShardingValueCacheFactory.getInstance().remove(businessKeyStrategy);
    }

    // 批量清理
    public static void clearBatch(List<BusinessKeyStrategy> businessKeyStrategyList) {
        if (!CollectionUtils.isEmpty(businessKeyStrategyList)) {
            ShardingValueCacheDecorator shardingValueCacheDecorator = ShardingValueCacheFactory.getInstance();
            for (BusinessKeyStrategy businessKeyStrategy : businessKeyStrategyList) {
                shardingValueCacheDecorator.remove(businessKeyStrategy);
            }
        }
    }

}
