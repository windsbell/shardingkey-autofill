package com.windsbell.shardingkey.autofill.finder.cache.clean;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.windsbell.shardingkey.autofill.config.TableShardingStrategyHelper;
import com.windsbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.BusinessStrategy;
import com.windsbell.shardingkey.autofill.strategy.ShardingKeyStrategy;

import java.util.*;

/**
 * 业务分片键字段映射策略建造器，支持链式调用动态构建，可用来清理分片键值对内容所在cache
 *
 * @author windbell
 */
public class BusinessKeyStrategyBuilder {

    private List<BusinessKeyStrategy> businessKeyStrategyList = new ArrayList<>(); // 业务分片键字段映射策略集合

    private List<BusinessStrategy<?>> necessaryBusinessKeys; // 必要业务键列表[条件中必须出现的业务键,通过其中出现的所有业务键可查出分库分表等键值对]

    private List<BusinessStrategy<?>> anyOneBusinessKeys; // 任意业务键列表[条件中出现以下任意一个业务键即可满足可查出分库分表等键值对]

    public static BusinessKeyStrategyBuilder builder() {
        return new BusinessKeyStrategyBuilder();
    }

    private BusinessKeyStrategyBuilder() {
        init();
    }

    private void init() {
        this.necessaryBusinessKeys = new ArrayList<>();
        this.anyOneBusinessKeys = new ArrayList<>();
    }

    /**
     * 设置必要字段和值
     */
    public BusinessKeyStrategyBuilder addNecessaryBusinessKey(String key, Object value) {
        Assert.notEmpty(key, "input necessary business key should not be empty!");
        Assert.notNull(value, "input necessary business value can not be null!");
        List<ShardingKeyStrategy> shardingKeyStrategyList = TableShardingStrategyHelper.getNecessaryTableShardingKeyStrategyMap().get(key);
        Assert.notEmpty(shardingKeyStrategyList, "strategies configs of necessary business key not contain this key:" + key);
        BusinessStrategy<Object> businessStrategy = new BusinessStrategy<>();
        businessStrategy.setKey(key);
        businessStrategy.setValue(value);
        this.necessaryBusinessKeys.add(businessStrategy);
        preNext(shardingKeyStrategyList);
        return this;
    }

    /**
     * 设置任意字段和值
     */
    public BusinessKeyStrategyBuilder addAnyOneBusinessKey(String key, Object value) {
        Assert.notEmpty(key, "input any one business key should not be empty!");
        Assert.notNull(value, "input any one business value should not be null!");
        List<ShardingKeyStrategy> shardingKeyStrategyList = TableShardingStrategyHelper.getAnyOneTableShardingKeyStrategyMap().get(key);
        Assert.notEmpty(shardingKeyStrategyList, "strategies configs of any one business key not contain this key:" + key);
        BusinessStrategy<Object> businessStrategy = new BusinessStrategy<>();
        businessStrategy.setKey(key);
        businessStrategy.setValue(value);
        this.anyOneBusinessKeys.add(businessStrategy);
        preNext(shardingKeyStrategyList);
        return this;
    }

    private void preNext(List<ShardingKeyStrategy> shardingKeyStrategyList) {
        shardingKeyStrategyList.forEach(this::one);
        init();
    }

    private void one(ShardingKeyStrategy shardingKeyStrategy) {
        BusinessKeyStrategy businessKeyStrategy = new BusinessKeyStrategy();
        businessKeyStrategy.setShardingKeyStrategy(shardingKeyStrategy);
        businessKeyStrategy.setAnyOneBusinessKeys(this.anyOneBusinessKeys);
        businessKeyStrategy.setNecessaryBusinessKeys(this.necessaryBusinessKeys);
        this.businessKeyStrategyList.add(businessKeyStrategy);
    }

    /**
     * 策略构建完成后的标记，返回该策略
     */
    public List<BusinessKeyStrategy> build() {
        return this.businessKeyStrategyList;
    }

    /**
     * 清空重置
     */
    public void reset() {
        this.businessKeyStrategyList = new ArrayList<>();
        init();
    }

    /**
     * 清空分片键字段映射策略集对应的 分片键值对内容所在cache
     */
    public void clearShardingValue() {
        ShardingValueCleaner.clearBatch(businessKeyStrategyList);
    }

}
