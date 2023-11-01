package com.windbell.shardingkey.autofill.config;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.windbell.shardingkey.autofill.handler.AbstractShardingStrategyHandler;
import com.windbell.shardingkey.autofill.handler.ShardingStrategyHandler;
import com.windbell.shardingkey.autofill.properties.ShardingKeyAutoFillProperty;
import com.windbell.shardingkey.autofill.properties.TableShardingKeyProperty;
import com.windbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import com.windbell.shardingkey.autofill.utils.PackageUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 自动填充分片键策实例化工厂
 *
 * @author windbell
 */
@Slf4j
public class ShardingStrategyHandlerFactory {

    private final static String SHARDING_STRATEGY_HANDLER_PACKAGE_NAME;

    private final static String DEFAULT_SHARDING_STRATEGY_HANDLER_TYPE;

    private final static Map<String, AbstractShardingStrategyHandler> SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE = new ConcurrentHashMap<>();

    public static AbstractShardingStrategyHandler getDefaultInstance() {
        return SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE.get(DEFAULT_SHARDING_STRATEGY_HANDLER_TYPE);
    }

    public static List<AbstractShardingStrategyHandler> getAllInstances() {
        ArrayList<AbstractShardingStrategyHandler> allInstances = new ArrayList<>(SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE.values());
        allInstances.remove(getDefaultInstance());
        return allInstances;
    }

    static AbstractShardingStrategyHandler getInstance(String type, Object[] args) {
        AbstractShardingStrategyHandler shardingStrategyHandler = SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE.get(type);
        if (shardingStrategyHandler == null) {
            try {
                Class<?> clazz = Class.forName(type);
                if (!(AbstractShardingStrategyHandler.class.isAssignableFrom(clazz))) {
                    throw new ClassCastException(String.format("自动填充分片键策略：%s 必须继承自AbstractShardingStrategyHandler！", type));
                }
                shardingStrategyHandler = reflect(args, clazz);
                SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE.put(type, shardingStrategyHandler);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                     InvocationTargetException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return shardingStrategyHandler;
    }

    static {
        String factoryPackageName = ShardingStrategyHandlerFactory.class.getPackage().getName();
        SHARDING_STRATEGY_HANDLER_PACKAGE_NAME = factoryPackageName.substring(0, factoryPackageName.lastIndexOf('.')) + ".handler";
        DEFAULT_SHARDING_STRATEGY_HANDLER_TYPE = SHARDING_STRATEGY_HANDLER_PACKAGE_NAME + ".DefaultShardingStrategyHandler";
    }

    static void init(Object[] args) {
        // 默认加载handler包下所有的实现ShardingStrategyHandler（自动填充分片键策略接口）的策略处理类收集到工厂
        List<Class<?>> classList = PackageUtil.getClassList(SHARDING_STRATEGY_HANDLER_PACKAGE_NAME, false
                , clazz -> clazz.getSuperclass() != null && ShardingStrategyHandler.class.isAssignableFrom(clazz)
                        && AbstractShardingStrategyHandler.class.isAssignableFrom(clazz.getSuperclass()));
        for (Class<?> clazz : classList) {
            try {
                AbstractShardingStrategyHandler shardingStrategyHandler = reflect(args, clazz);
                SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE.putIfAbsent(shardingStrategyHandler.getClass().getName(), shardingStrategyHandler);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 初始化时反射注入属性
     */
    static AbstractShardingStrategyHandler reflect(Object[] args, Class<?> clazz)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        init((ShardingKeyAutoFillProperty) args[0]);
        return (AbstractShardingStrategyHandler) clazz.newInstance();
    }

    private static void init(ShardingKeyAutoFillProperty shardingKeyAutoFillProperty) {
        checkShardProperties(shardingKeyAutoFillProperty);
        List<TableShardingKeyProperty> strategies = shardingKeyAutoFillProperty.getStrategies();
        for (TableShardingKeyProperty strategy : strategies) {
            String tableShardKey = strategy.getTableShardKey();
            String databaseShardKey = strategy.getDatabaseShardKey();
            // 保证列表不能出现重复内容
            List<String> necessaryBusinessKeys = CollectionUtils.isEmpty(strategy.getNecessaryBusinessKeys())
                    ? strategy.getNecessaryBusinessKeys() : strategy.getNecessaryBusinessKeys().stream().distinct().collect(Collectors.toList());
            List<String> anyOneBusinessKeys = CollectionUtils.isEmpty(strategy.getAnyOneBusinessKeys())
                    ? strategy.getAnyOneBusinessKeys() : strategy.getAnyOneBusinessKeys().stream().distinct().collect(Collectors.toList());
            List<String> suitableTables = strategy.getSuitableTables();
            for (String suitableTable : suitableTables) {
                TableShardingKeyStrategy tableShardingStrategy = new TableShardingKeyStrategy();
                if (CollectionUtils.isNotEmpty(necessaryBusinessKeys)) {
                    tableShardingStrategy.setNecessaryBusinessKeys(necessaryBusinessKeys);
                    String errorNotHasNecessaryBusinessKeys = String.format("条件未设置必须的业务字段:%s！", necessaryBusinessKeys);
                    tableShardingStrategy.setErrorNotHasNecessaryBusinessKeys(errorNotHasNecessaryBusinessKeys);
                }
                if (CollectionUtils.isNotEmpty(anyOneBusinessKeys)) {
                    tableShardingStrategy.setAnyOneBusinessKeys(anyOneBusinessKeys);
                    String errorNotHasAnyOneBusinessKeys = String.format("条件未设置任意业务字段之一:%s！", anyOneBusinessKeys);
                    tableShardingStrategy.setErrorNotHasAnyOneBusinessKeys(errorNotHasAnyOneBusinessKeys);
                }
                String errorNotHaseDatabaseShardKey = String.format("条件未设置分片键:%s！", strategy.getDatabaseShardKey());
                String errorNotHaseTableShardKey = String.format("条件未设置分片建:%s！", strategy.getTableShardKey());
                tableShardingStrategy.setTable(suitableTable);
                tableShardingStrategy.setTableShardKey(tableShardKey);
                tableShardingStrategy.setDatabaseShardKey(databaseShardKey);
                tableShardingStrategy.setErrorNotHaseTableShardKey(errorNotHaseTableShardKey);
                tableShardingStrategy.setErrorNotHaseDatabaseShardKey(errorNotHaseDatabaseShardKey);
                TableShardingStrategyHelper.put(suitableTable, tableShardingStrategy);
            }
        }

    }

    private static void checkShardProperties(ShardingKeyAutoFillProperty shardingKeyAutoFillProperty) {
        List<TableShardingKeyProperty> strategies = shardingKeyAutoFillProperty.getStrategies();
        Assert.notEmpty(strategies, "未配置策略集[spring.shardingkey-autofill.strategies]！");
        for (TableShardingKeyProperty table : strategies) {
            Assert.notEmpty(table.getSuitableTables(), "未配置适配的表集合[spring.shardingkey-autofill.strategies.suitableTables]！");
            Assert.notEmpty(table.getTableShardKey(), "未配置分表键[spring.shardingkey-autofill.strategies.tableShardKey]！");
            Assert.notEmpty(table.getDatabaseShardKey(), "未配置分库键[spring.shardingkey-autofill.strategies.databaseShardKey]！");
            Assert.isFalse(CollectionUtils.isEmpty(table.getNecessaryBusinessKeys()) && CollectionUtils.isEmpty(table.getAnyOneBusinessKeys())
                    , "请配置关键业务字段列表或者任务业务字段列表之一[spring.shardingkey-autofill.strategies.necessaryBusinessKeys(or anyOneBusinessKeys)]！");
        }
    }

}
