package com.windsbell.shardingkey.autofill.finder.cache.clean;

import com.windsbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.BusinessStrategy;
import com.windsbell.shardingkey.autofill.strategy.ShardingKeyStrategy;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务分片键字段映射策略建造器，支持链式调用动态构建，可用来清理分片键值对内容所在cache
 *
 * @author windbell
 */
public class BusinessKeyStrategyBuilder {

    protected BusinessKeyStrategy businessKeyStrategy; // 业务分片键字段映射策略

    protected ShardingKeyStrategy shardingKeyStrategy; // 分片键字段映射策略[分表键、分库键]

    protected List<BusinessStrategy<?>> necessaryBusinessKeys; // 必要业务键列表[条件中必须出现的业务键,通过其中出现的所有业务键可查出分库分表等键值对]

    protected List<BusinessStrategy<?>> anyOneBusinessKeys; // 任意业务键列表[条件中出现以下任意一个业务键即可满足可查出分库分表等键值对]

    public static BusinessKeyStrategyBuilder builder() {
        return new BusinessKeyStrategyBuilder();
    }

    protected BusinessKeyStrategyBuilder() {
        init();
    }

    protected void init() {
        this.businessKeyStrategy = new BusinessKeyStrategy();
        this.shardingKeyStrategy = new ShardingKeyStrategy();
        this.necessaryBusinessKeys = new ArrayList<>();
        this.anyOneBusinessKeys = new ArrayList<>();
        businessKeyStrategy.setShardingKeyStrategy(shardingKeyStrategy);
        businessKeyStrategy.setNecessaryBusinessKeys(necessaryBusinessKeys);
        businessKeyStrategy.setAnyOneBusinessKeys(anyOneBusinessKeys);
    }

    /**
     * 设置分片键
     */
    public BusinessKeyStrategyBuilder setShardingKeyStrategy(String databaseShardKey, String tableShardKey) {
        if (StringUtils.isNotBlank(databaseShardKey) && StringUtils.isNotBlank(tableShardKey)) {
            shardingKeyStrategy.setDatabaseShardKey(databaseShardKey);
            shardingKeyStrategy.setTableShardKey(tableShardKey);
        }
        return this;
    }

    /**
     * 设置必要字段和值
     */
    public BusinessKeyStrategyBuilder setNecessaryBusinessKey(String key, Object value) {
        if (StringUtils.isNotBlank(key) && ObjectUtils.isNotEmpty(value)) {
            BusinessStrategy<Object> businessStrategy = new BusinessStrategy<>();
            businessStrategy.setKey(key);
            businessStrategy.setValue(value);
            this.necessaryBusinessKeys.add(businessStrategy);
        }
        return this;
    }

    /**
     * 设置任意字段和值
     */
    public BusinessKeyStrategyBuilder setAnyOneBusinessKey(String key, Object value) {
        if (StringUtils.isNotBlank(key) && ObjectUtils.isNotEmpty(value)) {
            BusinessStrategy<Object> businessStrategy = new BusinessStrategy<>();
            businessStrategy.setKey(key);
            businessStrategy.setValue(value);
            this.necessaryBusinessKeys.add(businessStrategy);
            this.anyOneBusinessKeys.add(businessStrategy);
        }
        return this;
    }

    /**
     * 策略构建完成后的标记，返回该策略
     */
    public BusinessKeyStrategy build() {
        return this.businessKeyStrategy;
    }

    /**
     * 清空重置
     */
    public void reset() {
        this.businessKeyStrategy = new BusinessKeyStrategy();
    }

    /**
     * 清空分片键字段映射策略集对应的 分片键值对内容所在cache
     */
    public void clearShardingValue() {
        ShardingValueCleaner.clear(businessKeyStrategy);
    }

}
