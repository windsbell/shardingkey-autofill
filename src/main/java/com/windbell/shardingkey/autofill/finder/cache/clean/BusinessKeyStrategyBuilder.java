package com.windbell.shardingkey.autofill.finder.cache.clean;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.windbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windbell.shardingkey.autofill.strategy.BusinessStrategy;
import com.windbell.shardingkey.autofill.strategy.ShardingKeyStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务分片键字段映射策略建造器，支持链式调用动态构建，可用来清理分片键值对内容所在cache
 *
 * @author windbell
 */
public class BusinessKeyStrategyBuilder {

    private final BusinessKeyStrategy businessKeyStrategy; // 业务分片键字段映射策略

    private final ShardingKeyStrategy shardingKeyStrategy; // 分片键字段映射策略[分表键、分库键]

    private final List<BusinessStrategy> necessaryBusinessKeys; // 必要业务键列表[条件中必须出现的业务键,通过其中出现的所有业务键可查出分库分表等键值对]

    private final List<BusinessStrategy> anyOneBusinessKeys; // 任意业务键列表[条件中出现以下任意一个业务键即可满足可查出分库分表等键值对]

    public BusinessKeyStrategyBuilder() {
        this.businessKeyStrategy = new BusinessKeyStrategy();
        this.shardingKeyStrategy = new ShardingKeyStrategy();
        this.necessaryBusinessKeys = new ArrayList<>();
        this.anyOneBusinessKeys = new ArrayList<>();
        businessKeyStrategy.setShardingKeyStrategy(shardingKeyStrategy);
        businessKeyStrategy.setNecessaryBusinessKeys(necessaryBusinessKeys);
        businessKeyStrategy.setAnyOneBusinessKeys(anyOneBusinessKeys);
    }

    public BusinessKeyStrategyBuilder setShardingKeyStrategy(String databaseShardKey, String tableShardKey) {
        if (StringUtils.isNotBlank(databaseShardKey) && StringUtils.isNotBlank(tableShardKey)) {
            shardingKeyStrategy.setDatabaseShardKey(databaseShardKey);
            shardingKeyStrategy.setTableShardKey(tableShardKey);
        }
        return this;
    }

    public BusinessKeyStrategyBuilder setNecessaryBusinessKey(String key, String value) {
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
            BusinessStrategy businessStrategy = new BusinessStrategy();
            businessStrategy.setKey(key);
            businessStrategy.setValue(value);
            this.necessaryBusinessKeys.add(businessStrategy);
        }
        return this;
    }

    public BusinessKeyStrategyBuilder setAnyOneBusinessKey(String key, String value) {
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
            BusinessStrategy businessStrategy = new BusinessStrategy();
            businessStrategy.setKey(key);
            businessStrategy.setValue(value);
            this.necessaryBusinessKeys.add(businessStrategy);
            this.anyOneBusinessKeys.add(businessStrategy);
        }
        return this;
    }

    public BusinessKeyStrategy build() {
        return this.businessKeyStrategy;
    }

    public void clearShardingValue() {
        ShardingValueCleaner.clear(businessKeyStrategy);
    }

}
