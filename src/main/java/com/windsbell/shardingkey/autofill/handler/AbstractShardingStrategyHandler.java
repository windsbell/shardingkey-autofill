package com.windsbell.shardingkey.autofill.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.windsbell.shardingkey.autofill.finder.ShardingValueHandlerFactory;
import com.windsbell.shardingkey.autofill.jsqlparser.CourseExplain;
import com.windsbell.shardingkey.autofill.logger.CustomerLogger;
import com.windsbell.shardingkey.autofill.logger.CustomerLoggerFactory;
import com.windsbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.ShardingKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.ShardingValueStrategy;
import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.springframework.lang.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 核心抽象自动填充分片键策略处理类
 * 注: 具体实现的处理器默认为有效，继承者自行根据是否弃用而进行排除
 *
 * @author windbell
 */
public abstract class AbstractShardingStrategyHandler extends ShardingValueHandlerFactory implements ShardingStrategyHandler {

    private static final CustomerLogger log = CustomerLoggerFactory.getLogger(AbstractShardingStrategyHandler.class);

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
     * 公共方法：填充分片键
     */
    protected CourseExplain doFill(Statement statement, List<TableShardingKeyStrategy> tableShardingKeyStrategyList, List<?> parameterList) {
        return ShardingKeyCooker.builder(statement, tableShardingKeyStrategyList, parameterList)
                .setTableAlias()
                .matchBusinessKey()
                .initShardingKeyMap()
                .findShardingKeyHitFlag()
                .filterBusinessKey()
                .checkBusinessKeyValue()
                .combineShardingKeyMap(this::findShardingKeyValueStrategy, this::checkShardingValueStrategy)
                .fillShardingKey()
                .end();
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
     * 公共方法：检查分片键策略执行查找到的对应分片键值内容  （只做warn级别警告，未查出内容不阻塞流程）
     */
    protected void checkShardingValueStrategy(Statement statement, String table, ShardingKeyStrategy shardingKeyStrategy, ShardingValueStrategy shardingKeyValueStrategy) {
        if (shardingKeyValueStrategy == null || (StringUtils.isBlank(shardingKeyValueStrategy.getDatabaseShardValue()) && StringUtils.isBlank(shardingKeyValueStrategy.getTableShardValue()))) {
            log.warn("\n{} \n: strategy for table: [{}] ,but Finder: [{}] did`t find database sharding key field and table sharding key field！", statement, table, shardingKeyStrategy.getFinderClassName());
        } else if (StringUtils.isBlank(shardingKeyValueStrategy.getDatabaseShardValue())) {
            log.warn("\n{} \n: strategy for table: [{}] ,but Finder: [{}] did`t find database sharding key field！", statement, table, shardingKeyStrategy.getFinderClassName());
        } else if (StringUtils.isBlank(shardingKeyValueStrategy.getTableShardValue())) {
            log.warn("\n{} \n: strategy for table: [{}] ,but Finder: [{}] did`t find table sharding key field！", statement, table, shardingKeyStrategy.getFinderClassName());
        }
    }

}
