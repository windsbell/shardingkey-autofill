package com.windsbell.shardingkey.autofill.handler;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.windsbell.shardingkey.autofill.jsqlparser.CourseExplain;
import com.windsbell.shardingkey.autofill.jsqlparser.ShardingKeyFillerAgent;
import com.windsbell.shardingkey.autofill.jsqlparser.ShardingKeyHitFinder;
import com.windsbell.shardingkey.autofill.jsqlparser.TableAliasSetter;
import com.windsbell.shardingkey.autofill.strategy.*;
import net.sf.jsqlparser.statement.Statement;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * SQL分片键锅炉房（核心调用链）
 *
 * @author windbell
 */
public class ShardingKeyCooker {

    private final Long start; // 开始时间

    private final Statement statement; // sql

    private final List<?> parameterList; // 占位符内容值列表

    private final List<TableShardingKeyStrategy> tableShardingKeyStrategyList; // 表分片键映射策略列表

    private final Map<String, Map<String, String>> fillShardingKeyMap; // 【表名  分片键，分片键值】目标要填充的片键值对内容

    private List<BusinessKeyStrategy> businessKeyStrategyList; // 业务分片键字段映射策略列表

    private Map<String, TableShardingKeyStrategy> shardingKeyStrategyMap; // 【表名  ，表分片键映射策略】 业务分片键字段映射策略列表

    private Map<String, Map<String, Boolean>> firstShardingKeyHitFlagMap; // 【表名  分片键，是否设置 】原始检测收集的结果

    private CourseExplain courseExplain; // 分片键填充过程说明

    public static ShardingKeyCooker builder(Statement statement, List<TableShardingKeyStrategy> tableShardingKeyStrategyList, List<?> parameterList) {
        return new ShardingKeyCooker(statement, tableShardingKeyStrategyList, parameterList);
    }

    private ShardingKeyCooker(Statement statement, List<TableShardingKeyStrategy> tableShardingKeyStrategyList, List<?> parameterList) {
        this.start = System.currentTimeMillis();
        this.statement = statement;
        this.tableShardingKeyStrategyList = tableShardingKeyStrategyList;
        this.parameterList = parameterList;
        this.fillShardingKeyMap = new LinkedHashMap<>();
    }

    // 设置表字段别名
    public ShardingKeyCooker setTableAlias() {
        TableAliasSetter tableAliasSetter = new TableAliasSetter(statement);
        tableAliasSetter.doSet();
        return this;
    }

    // 匹配业务键
    public ShardingKeyCooker matchBusinessKey() {
        BusinessKeyStrategyHelper businessKeyStrategyHelper = new BusinessKeyStrategyHelper(statement, tableShardingKeyStrategyList, parameterList);
        businessKeyStrategyHelper.doMatch();
        businessKeyStrategyList = businessKeyStrategyHelper.getBusinessKeyStrategyList();
        shardingKeyStrategyMap = businessKeyStrategyHelper.getShardingKeyStrategyMap();
        return this;
    }

    // 初始化目标要填充的片键值对内容
    public ShardingKeyCooker initShardingKeyMap() {
        for (BusinessKeyStrategy businessKeyStrategy : businessKeyStrategyList) {
            Map<String, String> fillShardingKeyInnerMap = new LinkedHashMap<>(2);
            fillShardingKeyInnerMap.put(businessKeyStrategy.getShardingKeyStrategy().getDatabaseShardKey(), null);
            fillShardingKeyInnerMap.put(businessKeyStrategy.getShardingKeyStrategy().getTableShardKey(), null);
            fillShardingKeyMap.put(businessKeyStrategy.getTable(), fillShardingKeyInnerMap);
        }
        return this;
    }

    // 查找分片键是否设置
    public ShardingKeyCooker findShardingKeyHitFlag() {
        ShardingKeyHitFinder shardingKeyHitFinder = new ShardingKeyHitFinder(statement, fillShardingKeyMap);
        firstShardingKeyHitFlagMap = shardingKeyHitFinder.doFind();
        return this;
    }

    // 过滤业务键  如果条件中匹配已经有了分片键，则过滤后面需要通过业务键策略查找分片键再填充分片的流程
    public ShardingKeyCooker filterBusinessKey() {
        businessKeyStrategyList = businessKeyStrategyList.stream()
                .filter(businessKeyStrategy -> !hasMatchShardingKey(businessKeyStrategy.getTable())
                ).collect(Collectors.toList());
        Iterator<Map.Entry<String, Map<String, String>>> iterator = fillShardingKeyMap.entrySet().iterator();
        while (iterator.hasNext()) {
            String table = iterator.next().getKey();
            if (hasMatchShardingKey(table)) iterator.remove();
        }
        return this;
    }

    // 检查所有分片键是否设置，全部设置了则过滤业务键、未设置全时则不过滤业务键
    private boolean hasMatchShardingKey(String table) {
        return firstShardingKeyHitFlagMap.get(table).values().stream().allMatch(Boolean::booleanValue);
    }

    // 检查是否包含业务键内容
    public ShardingKeyCooker checkBusinessKeyValue() {
        /*   在检查到原SQL未包含分片键时，则匹配检查业务键：
         *      1.如果配置有必填字段列表，SQL必须满足有搭配所有必填字段，才能为后续提供自动查询出分库分表键进行填充
         *      2.如果配置有任意字段列表，SQL须满足出现任意字段的条件，才能为后续提供自动查询出分库分表键进行填充
         *      3.如果两者都配置，则SQL需要同时满足上述场景1以及场景2
         */

        for (BusinessKeyStrategy businessKeyStrategy : businessKeyStrategyList) {
            TableShardingKeyStrategy tableShardingKeyStrategy = shardingKeyStrategyMap.get(businessKeyStrategy.getTable());
            List<String> necessaryBusinessKeys = tableShardingKeyStrategy.getNecessaryBusinessKeys();
            List<BusinessStrategy<?>> necessaryBusinessStrategies = businessKeyStrategy.getNecessaryBusinessKeys();
            Assert.isTrue(!(CollectionUtils.isNotEmpty(necessaryBusinessKeys)
                    && CollectionUtils.isEmpty(necessaryBusinessStrategies)), tableShardingKeyStrategy.getErrorNotHasNecessaryBusinessKeys());
            if (CollectionUtils.isNotEmpty(necessaryBusinessKeys) && CollectionUtils.isNotEmpty(necessaryBusinessStrategies)) {
                boolean notHasNecessaryBusinessKey = necessaryBusinessStrategies.stream().anyMatch(businessStrategy -> businessStrategy.getValue() == null);
                Assert.isTrue(!notHasNecessaryBusinessKey, "\n" + statement + "\n: " + tableShardingKeyStrategy.getErrorNotHasNecessaryBusinessKeys());
            }
            List<String> anyOneBusinessKeys = tableShardingKeyStrategy.getAnyOneBusinessKeys();
            List<BusinessStrategy<?>> anyOneBusinessStrategies = businessKeyStrategy.getAnyOneBusinessKeys();
            Assert.isTrue(!(CollectionUtils.isNotEmpty(anyOneBusinessKeys)
                    && CollectionUtils.isEmpty(anyOneBusinessStrategies)), statement + "\n: " + tableShardingKeyStrategy.getErrorNotHasAnyOneBusinessKeys());

        }
        return this;
    }

    // 组装目标要填充的片键值对内容
    public ShardingKeyCooker combineShardingKeyMap(BiFunction<BusinessKeyStrategy, TableShardingKeyStrategy, ShardingValueStrategy> findShardingKeyValueStrategyFunction
            , QuadConsumer<Statement, String, ShardingKeyStrategy, ShardingValueStrategy> checkShardingValueStrategyConsumer) {
        for (BusinessKeyStrategy businessKeyStrategy : businessKeyStrategyList) {
            String table = businessKeyStrategy.getTable();
            businessKeyStrategy.setStatement(this.statement);
            ShardingKeyStrategy shardingKeyStrategy = businessKeyStrategy.getShardingKeyStrategy();
            ShardingValueStrategy shardingKeyValueStrategy = findShardingKeyValueStrategyFunction.apply(businessKeyStrategy, shardingKeyStrategyMap.get(table));
            checkShardingValueStrategyConsumer.accept(statement, table, shardingKeyStrategy, shardingKeyValueStrategy);
            Map<String, String> fillShardingKeyInnerMap = fillShardingKeyMap.get(table);
            fillShardingKeyInnerMap.put(shardingKeyStrategy.getDatabaseShardKey(), shardingKeyValueStrategy.getDatabaseShardValue());
            fillShardingKeyInnerMap.put(shardingKeyStrategy.getTableShardKey(), shardingKeyValueStrategy.getTableShardValue());
        }
        return this;
    }

    // 开始填充分片键
    public ShardingKeyCooker fillShardingKey() {
        ShardingKeyFillerAgent shardingKeyFillerAgent = new ShardingKeyFillerAgent(statement, fillShardingKeyMap, firstShardingKeyHitFlagMap);
        courseExplain = shardingKeyFillerAgent.doFill(this.start);
        return this;
    }

    // 填充结束，拿到执行过程
    public CourseExplain end() {
        return this.courseExplain;
    }

    @FunctionalInterface
    public interface QuadConsumer<T, U, V, W> {
        void accept(T t, U u, V v, W w);
    }


}
