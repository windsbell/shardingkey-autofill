package com.windsbell.shardingkey.autofill.finder.cache.clean;

import com.windsbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.ShardingKeyStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务分片键字段映射策略批量建造器，支持链式调用动态构建，可用来批量清理分片键值对内容所在cache
 *
 * @author windbell
 */
public class BusinessKeyStrategyBatchBuilder extends BusinessKeyStrategyBuilder {

    private List<BusinessKeyStrategy> businessKeyStrategyList = new ArrayList<>(); // 业务分片键字段映射策略集合

    public static BusinessKeyStrategyBatchBuilder builder() {
        return new BusinessKeyStrategyBatchBuilder();
    }

    private BusinessKeyStrategyBatchBuilder() {
        init();
    }

    /**
     * 一轮策略设置完成后的标记, 再次进入下一轮设置
     */
    public BusinessKeyStrategyBatchBuilder one(String databaseShardKey, String tableShardKey) {
        ShardingKeyStrategy shardingKeyStrategy = new ShardingKeyStrategy();
        shardingKeyStrategy.setDatabaseShardKey(databaseShardKey);
        shardingKeyStrategy.setTableShardKey(tableShardKey);
        businessKeyStrategyList.add(businessKeyStrategy);
        init();
        return this;
    }

    /**
     * 所有策略批量构建完成后的标记，返回所有策略集
     */
    public List<BusinessKeyStrategy> buildBatch() {
        return this.businessKeyStrategyList;
    }

    /**
     * 清空重置
     */
    @Override
    public void reset() {
        this.businessKeyStrategyList = new ArrayList<>();
    }

    /**
     * 清空分片键字段映射策略集对应的 分片键值对内容所在cache
     */
    @Override
    public void clearShardingValue() {
        ShardingValueCleaner.clearBatch(this.businessKeyStrategyList);
    }


}
