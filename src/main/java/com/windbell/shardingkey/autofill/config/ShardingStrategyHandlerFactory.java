package com.windbell.shardingkey.autofill.config;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.windbell.shardingkey.autofill.finder.ShardingValueFinder;
import com.windbell.shardingkey.autofill.finder.ShardingValueFinderFactory;
import com.windbell.shardingkey.autofill.handler.AbstractShardingStrategyHandler;
import com.windbell.shardingkey.autofill.handler.ShardingStrategyHandler;
import com.windbell.shardingkey.autofill.properties.ShardingKeyAutoFillProperty;
import com.windbell.shardingkey.autofill.properties.TableShardingKeyProperty;
import com.windbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import com.windbell.shardingkey.autofill.utils.PackageUtil;
import lombok.extern.slf4j.Slf4j;

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

    static ShardingStrategyHandler getInstance() {
        ShardingStrategyHandler shardingStrategyHandler = getFromServices();
        if (shardingStrategyHandler == null) {
            return ShardingStrategyHandlerFactory.getDefaultInstance();
        }
        if (!(AbstractShardingStrategyHandler.class.isAssignableFrom(shardingStrategyHandler.getClass()))) {
            throw new ClassCastException(String.format("自定义分片键策略：%s 必须继承自AbstractShardingStrategyHandler！", shardingStrategyHandler.getClass()));
        }
        return shardingStrategyHandler;
    }

    private static ShardingStrategyHandler getFromServices() {
        // 工厂返回从项目中取的SPI第一条实现者
        ServiceLoader<ShardingStrategyHandler> services = ServiceLoader.load(ShardingStrategyHandler.class);
        Iterator<ShardingStrategyHandler> iterator = services.iterator();
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

    static void registerStrategyHelper(ShardingKeyAutoFillProperty shardingKeyAutoFillProperty) {
        checkShardProperties(shardingKeyAutoFillProperty);
        List<TableShardingKeyProperty> strategies = shardingKeyAutoFillProperty.getStrategies();
        for (TableShardingKeyProperty strategy : strategies) {
            String tableShardKey = strategy.getTableShardKey();
            String databaseShardKey = strategy.getDatabaseShardKey();
            String finderClassName = strategy.getFinderClassName();
            ShardingValueFinder shardingValueFinder = ShardingValueFinderFactory.getInstance(finderClassName);
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
                    String errorNotHasNecessaryBusinessKeys = String.format("condition must contain all necessary field:%s！", necessaryBusinessKeys);
                    tableShardingStrategy.setErrorNotHasNecessaryBusinessKeys(errorNotHasNecessaryBusinessKeys);
                }
                if (CollectionUtils.isNotEmpty(anyOneBusinessKeys)) {
                    tableShardingStrategy.setAnyOneBusinessKeys(anyOneBusinessKeys);
                    String errorNotHasAnyOneBusinessKeys = String.format("condition should contain any one required field:%s！", anyOneBusinessKeys);
                    tableShardingStrategy.setErrorNotHasAnyOneBusinessKeys(errorNotHasAnyOneBusinessKeys);
                }
                String errorNotHaseDatabaseShardKey = String.format("condition must contain database sharding key field:%s！", strategy.getDatabaseShardKey());
                String errorNotHaseTableShardKey = String.format("condition must contain table sharding key field:%s！", strategy.getTableShardKey());
                tableShardingStrategy.setTable(suitableTable);
                tableShardingStrategy.setTableShardKey(tableShardKey);
                tableShardingStrategy.setDatabaseShardKey(databaseShardKey);
                tableShardingStrategy.setShardingValueFinder(shardingValueFinder);
                tableShardingStrategy.setErrorNotHaseTableShardKey(errorNotHaseTableShardKey);
                tableShardingStrategy.setErrorNotHaseDatabaseShardKey(errorNotHaseDatabaseShardKey);
                TableShardingStrategyHelper.put(suitableTable, tableShardingStrategy);
            }
        }

    }

    private static void checkShardProperties(ShardingKeyAutoFillProperty shardingKeyAutoFillProperty) {
        List<TableShardingKeyProperty> strategies = shardingKeyAutoFillProperty.getStrategies();
        Assert.notEmpty(strategies, "未配置策略集[spring.shardingkeyAutofill.strategies]！");
        for (TableShardingKeyProperty table : strategies) {
            Assert.notEmpty(table.getSuitableTables(), "未配置适配的表集合[spring.shardingkeyAutofill.strategies.suitableTables]！");
            Assert.notEmpty(table.getTableShardKey(), "未配置分表键[spring.shardingkeyAutofill.strategies.tableShardKey]！");
            Assert.notEmpty(table.getDatabaseShardKey(), "未配置分库键[spring.shardingkeyAutofill.strategies.databaseShardKey]！");
            Assert.notEmpty(table.getFinderClassName(), "未配置分库键查找器Class[spring.shardingkeyAutofill.strategies.finderClassName]！");
            Assert.isFalse(CollectionUtils.isEmpty(table.getNecessaryBusinessKeys()) && CollectionUtils.isEmpty(table.getAnyOneBusinessKeys())
                    , "请配置关键业务字段列表或者任务业务字段列表之一[spring.shardingkeyAutofill.strategies.necessaryBusinessKeys(or anyOneBusinessKeys)]！");
        }
    }

}
