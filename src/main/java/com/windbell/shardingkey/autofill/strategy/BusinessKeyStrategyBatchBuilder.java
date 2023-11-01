package com.windbell.shardingkey.autofill.strategy;

import com.windbell.shardingkey.autofill.finder.ShardingValueCleaner;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务分片键字段映射策略批量建造器，支持链式调用动态构建
 *
 * @author windbell
 */
public class BusinessKeyStrategyBatchBuilder {

    private BusinessKeyStrategy businessKeyStrategy;  // 业务分片键字段映射策略

    private ShardingKeyStrategy shardingKeyStrategy;  // 分片键字段映射策略[分表键、分库键]

    private List<BusinessStrategy> necessaryBusinessKeys;  // 必要业务键列表[条件中必须出现的业务键,通过其中出现的所有业务键可查出分库分表等键值对]

    private List<BusinessStrategy> anyOneBusinessKeys; // 任意业务键列表[条件中出现以下任意一个业务键即可满足可查出分库分表等键值对]

    private List<BusinessKeyStrategy> businessKeyStrategyList = new ArrayList<>(); // 业务分片键字段映射策略集合

    public BusinessKeyStrategyBatchBuilder() {
        init();
    }

    private void init() {
        this.businessKeyStrategy = new BusinessKeyStrategy();
        this.shardingKeyStrategy = new ShardingKeyStrategy();
        this.necessaryBusinessKeys = new ArrayList<>();
        this.anyOneBusinessKeys = new ArrayList<>();
        businessKeyStrategy.setShardingKeyStrategy(shardingKeyStrategy);
        businessKeyStrategy.setNecessaryBusinessKeys(necessaryBusinessKeys);
        businessKeyStrategy.setAnyOneBusinessKeys(anyOneBusinessKeys);
    }

    public BusinessKeyStrategyBatchBuilder setShardingKeyStrategy(String databaseShardKey, String tableShardKey) {
        if (!StringUtils.isEmpty(databaseShardKey) && !StringUtils.isEmpty(tableShardKey)) {
            shardingKeyStrategy.setDatabaseShardKey(databaseShardKey);
            shardingKeyStrategy.setTableShardKey(tableShardKey);
        }
        return this;
    }

    public BusinessKeyStrategyBatchBuilder setNecessaryBusinessKey(String key, String value) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(value)) {
            BusinessStrategy businessStrategy = new BusinessStrategy();
            businessStrategy.setKey(key);
            businessStrategy.setValue(value);
            this.necessaryBusinessKeys.add(businessStrategy);
        }
        return this;
    }

    public BusinessKeyStrategyBatchBuilder setAnyOneBusinessKey(String key, String value) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(value)) {
            BusinessStrategy businessStrategy = new BusinessStrategy();
            businessStrategy.setKey(key);
            businessStrategy.setValue(value);
            this.anyOneBusinessKeys.add(businessStrategy);
        }
        return this;
    }

    public BusinessKeyStrategyBatchBuilder one() {
        businessKeyStrategyList.add(businessKeyStrategy);
        init();
        return this;
    }

    public List<BusinessKeyStrategy> build() {
        return this.businessKeyStrategyList;
    }

    /**
     * 清空重置
     */
    public void clear() {
        this.businessKeyStrategyList = new ArrayList<>();
    }

    /**
     * 清空分片键字段映射策略集对应的 分片键值对内容所在cache
     */
    public void clearShardingValue() {
        ShardingValueCleaner.clearBatch(this.businessKeyStrategyList);
    }


}
