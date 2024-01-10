package com.windsbell.shardingkey.autofill.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.windsbell.shardingkey.autofill.finder.ShardingValueHandlerFactory;
import com.windsbell.shardingkey.autofill.jsqlparser.CourseExplain;
import com.windsbell.shardingkey.autofill.jsqlparser.ShardingKeyFillerAgent;
import com.windsbell.shardingkey.autofill.jsqlparser.TableAliasSetter;
import com.windsbell.shardingkey.autofill.strategy.*;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 核心抽象自动填充分片键策略处理类
 * 注: 具体实现的处理器默认为有效，继承者自行根据是否弃用而进行排除
 *
 * @author windbell
 */
@Slf4j
public abstract class AbstractShardingStrategyHandler extends ShardingValueHandlerFactory implements ShardingStrategyHandler {

    // SQL类型：查询
    protected final static String SQL_COMMAND_SELECT = "select";

    // SQL类型：修改
    protected final static String SQL_COMMAND_UPDATE = "update";

    // SQL类型：删除
    protected final static String SQL_COMMAND_DELETE = "delete";

    /**
     * 对应策略要执行的解析动作
     * statement:预处理语句
     * parameterObject: 替换参数对象
     * tableShardingKeyStrategyList: 表分片键映射策略 （单表一条，多表多条）
     */
    public abstract void parse(Statement statement, Object parameterObject, List<TableShardingKeyStrategy> tableShardingKeyStrategyList);

    /**
     * 尝试获取到Wrapper,若拿不到则是mapper
     */
    @Nullable
    protected AbstractWrapper<?, ?, ?> tryAndGetWrapper(Object methodObj) {
        AbstractWrapper<?, ?, ?> wrapper = null;
        if (methodObj instanceof MapperMethod.ParamMap) {
            MapperMethod.ParamMap<?> paramMap = (MapperMethod.ParamMap<?>) methodObj;
            if (paramMap.containsKey("ew")) {
                Object obj = paramMap.get("ew");
                if (obj instanceof AbstractWrapper) {
                    wrapper = (AbstractWrapper<?, ?, ?>) obj;
                }
            }
        }
        return wrapper;
    }

    /**
     * 公共方法：提取占位符字段参数内容列表
     */
    protected static List<?> getParameterList(Map<String, ?> paramMap, String parameterPrefix) {
        return paramMap.keySet()
                .stream().filter(k -> k.contains(parameterPrefix))
                .sorted(Comparator.comparing(k -> Long.valueOf(k.replace(parameterPrefix, StringUtils.EMPTY))))
                .map(paramMap::get).collect(Collectors.toList());
    }

    /**
     * 公共方法：通过业务键策略，使用分片键查找器查到对应分片键值内容，可选配是否置入cache，如果置入cache，则后面同样语句类型直接从cache中拿取
     */
    protected ShardingValueStrategy findShardingKeyValueStrategy(BusinessKeyStrategy businessKeyStrategy
            , TableShardingKeyStrategy tableShardingKeyStrategy) {
        return ShardingValueHandlerFactory.getHandler()
                .doFind(businessKeyStrategy, tableShardingKeyStrategy.getShardingValueFinder());
    }

    /**
     * 公共方法：填充分片键
     */
    protected CourseExplain doFill(Statement statement, List<TableShardingKeyStrategy> tableShardingKeyStrategyList, List<?> parameterList) {
        // 检查businessKeyStrategyList中 是否出现必要字段，以及任意字段,  对于值为占位符 ? 的, 从参数列表取】
        BusinessKeyStrategyHelper businessKeyStrategyHelper = new BusinessKeyStrategyHelper(statement, tableShardingKeyStrategyList, parameterList);
        List<BusinessKeyStrategy> businessKeyStrategyList = businessKeyStrategyHelper.match();
        Map<String, TableShardingKeyStrategy> shardingKeyStrategyMap = businessKeyStrategyHelper.getShardingKeyStrategyMap();
        checkBusinessKeyStrategy(statement, shardingKeyStrategyMap, businessKeyStrategyList);
        Map<String, Map<String, String>> fillShardingKeyMap = combineShardingKeyMap(statement, shardingKeyStrategyMap, businessKeyStrategyList);
        TableAliasSetter tableAliasSetter = new TableAliasSetter(statement);
        tableAliasSetter.setTableAlias();
        ShardingKeyFillerAgent shardingKeyFillerAgent = new ShardingKeyFillerAgent(fillShardingKeyMap, statement);
        return shardingKeyFillerAgent.doFill();
    }

    private void checkBusinessKeyStrategy(Statement statement, Map<String, TableShardingKeyStrategy> shardingKeyStrategyMap, List<BusinessKeyStrategy> businessKeyStrategyList) {
        for (BusinessKeyStrategy businessKeyStrategy : businessKeyStrategyList) {
            TableShardingKeyStrategy tableShardingKeyStrategy = shardingKeyStrategyMap.get(businessKeyStrategy.getTable());
            checkBusinessStrategies(statement, tableShardingKeyStrategy, businessKeyStrategy.getNecessaryBusinessKeys(), businessKeyStrategy.getAnyOneBusinessKeys());
        }
    }

    /*
     *      1.如果配置有必填字段列表，SQL必须满足有搭配所有必填字段，才能为后续提供自动查询出分库分表键进行填充
     *      2.如果配置有任意字段列表，SQL须满足出现任意字段的条件，才能为后续提供自动查询出分库分表键进行填充
     *      3.如果两者都配置，则SQL需要同时满足上述场景1以及场景2
     */
    protected void checkBusinessStrategies(Statement statement, TableShardingKeyStrategy tableShardingStrategy
            , List<BusinessStrategy<?>> necessaryBusinessStrategies, List<BusinessStrategy<?>> anyOneBusinessStrategies) {
        Assert.isTrue(!(CollectionUtils.isNotEmpty(tableShardingStrategy.getNecessaryBusinessKeys())
                && CollectionUtils.isEmpty(necessaryBusinessStrategies)), tableShardingStrategy.getErrorNotHasNecessaryBusinessKeys());
        if (CollectionUtils.isNotEmpty(tableShardingStrategy.getNecessaryBusinessKeys()) && CollectionUtils.isNotEmpty(necessaryBusinessStrategies)) {
            boolean notHasNecessaryBusinessKey = necessaryBusinessStrategies.stream().anyMatch(businessStrategy -> businessStrategy.getValue() == null);
            Assert.isTrue(!notHasNecessaryBusinessKey, "\n" + statement + "\n: " + tableShardingStrategy.getErrorNotHasNecessaryBusinessKeys());
        }
        Assert.isTrue(!(CollectionUtils.isNotEmpty(tableShardingStrategy.getAnyOneBusinessKeys())
                && CollectionUtils.isEmpty(anyOneBusinessStrategies)), statement + "\n: " + tableShardingStrategy.getErrorNotHasAnyOneBusinessKeys());
    }

    private Map<String, Map<String, String>> combineShardingKeyMap(Statement statement, Map<String, TableShardingKeyStrategy> shardingKeyStrategyMap, List<BusinessKeyStrategy> businessKeyStrategyList) {
        Map<String, Map<String, String>> fillShardingKeyMap = new LinkedHashMap<>(businessKeyStrategyList.size());
        for (BusinessKeyStrategy businessKeyStrategy : businessKeyStrategyList) {
            TableShardingKeyStrategy tableShardingKeyStrategy = shardingKeyStrategyMap.get(businessKeyStrategy.getTable());
            ShardingValueStrategy shardingKeyValueStrategy = this.findShardingKeyValueStrategy(businessKeyStrategy, tableShardingKeyStrategy);
            checkShardingValueStrategy(statement, businessKeyStrategy.getTable(), businessKeyStrategy.getShardingKeyStrategy(), shardingKeyValueStrategy);
            Map<String, String> fillShardingKeyInnerMap = new LinkedHashMap<>(2);
            fillShardingKeyInnerMap.put(businessKeyStrategy.getShardingKeyStrategy().getDatabaseShardKey(), shardingKeyValueStrategy.getDatabaseShardValue());
            fillShardingKeyInnerMap.put(businessKeyStrategy.getShardingKeyStrategy().getTableShardKey(), shardingKeyValueStrategy.getTableShardValue());
            fillShardingKeyMap.put(businessKeyStrategy.getTable(), fillShardingKeyInnerMap);
        }
        return fillShardingKeyMap;
    }

    private void checkShardingValueStrategy(Statement statement, String table, ShardingKeyStrategy shardingKeyStrategy, ShardingValueStrategy shardingKeyValueStrategy) {
        if (shardingKeyValueStrategy == null || (StringUtils.isBlank(shardingKeyValueStrategy.getDatabaseShardValue()) && StringUtils.isBlank(shardingKeyValueStrategy.getTableShardValue()))) {
            log.warn("\n{} \n: strategy for table: [{}] ,but Finder: [{}] did`t find database sharding key field and table sharding key field！", statement, table, shardingKeyStrategy.getFinderClassName());
        } else if (StringUtils.isBlank(shardingKeyValueStrategy.getDatabaseShardValue())) {
            log.warn("\n{} \n: strategy for table: [{}] ,but Finder: [{}] did`t find database sharding key field！", statement, table, shardingKeyStrategy.getFinderClassName());
        } else if (StringUtils.isBlank(shardingKeyValueStrategy.getTableShardValue())) {
            log.warn("\n{} \n: strategy for table: [{}] ,but Finder: [{}] did`t find table sharding key field！", statement, table, shardingKeyStrategy.getFinderClassName());
        }
    }

}
