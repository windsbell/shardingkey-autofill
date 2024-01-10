package com.windsbell.shardingkey.autofill.config;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.windsbell.shardingkey.autofill.finder.ShardingValueFinder;
import com.windsbell.shardingkey.autofill.handler.AbstractShardingStrategyHandler;
import com.windsbell.shardingkey.autofill.handler.ShardingStrategyHandler;
import com.windsbell.shardingkey.autofill.properties.ShardingKeyAutoFillProperty;
import com.windsbell.shardingkey.autofill.properties.TableShardingKeyProperty;
import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import com.windsbell.shardingkey.autofill.utils.PackageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.*;
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

    static {
        String factoryPackageName = ShardingStrategyHandlerFactory.class.getPackage().getName();
        SHARDING_STRATEGY_HANDLER_PACKAGE_NAME = factoryPackageName.substring(0, factoryPackageName.lastIndexOf('.')) + ".handler";
        DEFAULT_SHARDING_STRATEGY_HANDLER_TYPE = SHARDING_STRATEGY_HANDLER_PACKAGE_NAME + ".DefaultShardingStrategyHandler";
        initInstances();
    }

    public static List<AbstractShardingStrategyHandler> getAllInstances() {
        ArrayList<AbstractShardingStrategyHandler> allInstances = new ArrayList<>(SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE.values());
        allInstances.remove(getDefaultInstance());
        return allInstances;
    }

    private static AbstractShardingStrategyHandler getDefaultInstance() {
        return SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE.get(DEFAULT_SHARDING_STRATEGY_HANDLER_TYPE);
    }

    static AbstractShardingStrategyHandler getInstance() {
        AbstractShardingStrategyHandler shardingStrategyHandler = getFromServices();
        if (shardingStrategyHandler == null) {
            return ShardingStrategyHandlerFactory.getDefaultInstance();
        }
        SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE.putIfAbsent(shardingStrategyHandler.getClass().getName(), shardingStrategyHandler);
        return shardingStrategyHandler;
    }

    private static AbstractShardingStrategyHandler getFromServices() {
        ServiceLoader<AbstractShardingStrategyHandler> services = ServiceLoader.load(AbstractShardingStrategyHandler.class);
        Iterator<AbstractShardingStrategyHandler> iterator = services.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    private static void initInstances() {
        // 默认加载handler包下所有的实现ShardingStrategyHandler（自动填充分片键策略接口）的策略处理类收集到工厂
        List<Class<?>> classList = PackageUtil.getClassList(SHARDING_STRATEGY_HANDLER_PACKAGE_NAME, false
                , clazz -> clazz.getSuperclass() != null && ShardingStrategyHandler.class.isAssignableFrom(clazz)
                        && AbstractShardingStrategyHandler.class.isAssignableFrom(clazz.getSuperclass()));
        for (Class<?> clazz : classList) {
            try {
                AbstractShardingStrategyHandler shardingStrategyHandler = (AbstractShardingStrategyHandler) clazz.newInstance();
                SHARDING_STRATEGY_HANDLER_INSTANCE_CACHE.putIfAbsent(shardingStrategyHandler.getClass().getName(), shardingStrategyHandler);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static void registerStrategyHelper(ShardingKeyAutoFillProperty shardingKeyAutoFillProperty, ShardingValueFinderFactory shardingValueFinderFactory) {
        checkShardProperties(shardingKeyAutoFillProperty);
        List<TableShardingKeyProperty> strategies = shardingKeyAutoFillProperty.getStrategies();
        for (TableShardingKeyProperty strategy : strategies) {
            String tableShardKey = strategy.getTableShardKey().trim();
            String databaseShardKey = strategy.getDatabaseShardKey().trim();
            String finderClassName = strategy.getFinderClassName().trim();
            ShardingValueFinder shardingValueFinder = shardingValueFinderFactory.getInstance(finderClassName);
            // 保证列表不能出现重复内容
            List<String> necessaryBusinessKeys = CollectionUtils.isEmpty(strategy.getNecessaryBusinessKeys())
                    ? strategy.getNecessaryBusinessKeys() : strategy.getNecessaryBusinessKeys().stream()
                    .map(String::trim).distinct().collect(Collectors.toList());
            List<String> anyOneBusinessKeys = CollectionUtils.isEmpty(strategy.getAnyOneBusinessKeys())
                    ? strategy.getAnyOneBusinessKeys() : strategy.getAnyOneBusinessKeys().stream()
                    .map(String::trim).distinct().collect(Collectors.toList());
            List<String> suitableTables = strategy.getSuitableTables();
            for (String suitableTable : suitableTables) {
                TableShardingKeyStrategy tableShardingStrategy = new TableShardingKeyStrategy();
                if (CollectionUtils.isNotEmpty(necessaryBusinessKeys)) {
                    tableShardingStrategy.setNecessaryBusinessKeys(necessaryBusinessKeys);
                    String errorNotHasNecessaryBusinessKeys = String.format("strategy for table: %s, condition must contain all necessary field:%s！", suitableTable, necessaryBusinessKeys);
                    tableShardingStrategy.setErrorNotHasNecessaryBusinessKeys(errorNotHasNecessaryBusinessKeys);
                }
                if (CollectionUtils.isNotEmpty(anyOneBusinessKeys)) {
                    tableShardingStrategy.setAnyOneBusinessKeys(anyOneBusinessKeys);
                    String errorNotHasAnyOneBusinessKeys = String.format("strategy for table: %s, condition should contain any one required field:%s！", suitableTable, anyOneBusinessKeys);
                    tableShardingStrategy.setErrorNotHasAnyOneBusinessKeys(errorNotHasAnyOneBusinessKeys);
                }
                String errorNotHaseDatabaseShardKey = String.format("strategy for table: %s, condition must contain database sharding key field:%s！", suitableTable, strategy.getDatabaseShardKey());
                String errorNotHaseTableShardKey = String.format("strategy for table: %s, condition must contain table sharding key field:%s！", suitableTable, strategy.getTableShardKey());
                tableShardingStrategy.setTable(suitableTable.trim());
                tableShardingStrategy.setTableShardKey(tableShardKey);
                tableShardingStrategy.setDatabaseShardKey(databaseShardKey);
                tableShardingStrategy.setShardingValueFinder(shardingValueFinder);
                tableShardingStrategy.setFinderClassName(finderClassName);
                tableShardingStrategy.setErrorNotHaseTableShardKey(errorNotHaseTableShardKey);
                tableShardingStrategy.setErrorNotHaseDatabaseShardKey(errorNotHaseDatabaseShardKey);
                TableShardingStrategyHelper.put(suitableTable, tableShardingStrategy);
            }
        }

    }

    private static void checkShardProperties(ShardingKeyAutoFillProperty shardingKeyAutoFillProperty) {
        List<TableShardingKeyProperty> strategies = shardingKeyAutoFillProperty.getStrategies();
        Assert.notEmpty(strategies, "please configure strategies [spring.shardingkeyAutofill.strategies]！");
        for (TableShardingKeyProperty table : strategies) {
            Assert.notEmpty(table.getSuitableTables(), "please configure strategies for suitableTables [spring.shardingkeyAutofill.strategies.suitableTables]！");
            Assert.notEmpty(new String[]{table.getTableShardKey()}, "please configure table shard key [spring.shardingkeyAutofill.strategies.tableShardKey]！");
            Assert.notEmpty(new String[]{table.getDatabaseShardKey()}, "please configure database shard key [spring.shardingkeyAutofill.strategies.databaseShardKey]！");
            Assert.notEmpty(new String[]{table.getFinderClassName()}, "please configure finder class name [spring.shardingkeyAutofill.strategies.finderClassName]！");
            Assert.isTrue(!(CollectionUtils.isEmpty(table.getNecessaryBusinessKeys()) && CollectionUtils.isEmpty(table.getAnyOneBusinessKeys()))
                    , "please configure necessary business keys or any one business keys [spring.shardingkeyAutofill.strategies.necessaryBusinessKeys(or anyOneBusinessKeys)]！");
        }
    }

}
