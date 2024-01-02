package com.windsbell.shardingkey.autofill.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.windsbell.shardingkey.autofill.logger.CustomerLogger;
import com.windsbell.shardingkey.autofill.logger.CustomerLoggerFactory;
import com.windsbell.shardingkey.autofill.strategy.*;
import lombok.Getter;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * wrapper类型SQL自动填充分片键策略处理类
 * 此处理器废弃：请移步新的Wrapper2ShardingStrategyHandler
 *
 * @author windbell
 */
@Deprecated
public class WrapperShardingStrategyHandler extends AbstractShardingStrategyHandler implements ShardingStrategyHandler {

    @Getter
    protected boolean isEffective = false;

    private static final CustomerLogger log = CustomerLoggerFactory.getLogger(WrapperShardingStrategyHandler.class);

    /**
     * statement:预处理语句
     * parameterObject: 替换参数对象
     * tableShardingKeyStrategyList: 表分片键映射策略 （单表一条，多表多条）
     */
    @Override
    public void parse(Statement statement, Object parameterObject, List<TableShardingKeyStrategy> tableShardingKeyStrategyList) {
        AbstractWrapper<?, ?, ?> wrapper = super.tryAndGetWrapper(parameterObject);
        if (wrapper != null) {
            if (statement instanceof Select) {
                Select select = (Select) statement;
                if (select.getSelectBody() instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
                    for (TableShardingKeyStrategy tableShardingKeyStrategy : tableShardingKeyStrategyList) {
                        this.parseWrapper(SQL_COMMAND_SELECT, statement, (Parenthesis) plainSelect.getWhere(), wrapper, tableShardingKeyStrategy);
                    }
                }
            } else if (statement instanceof Update) {
                Update update = (Update) statement;
                for (TableShardingKeyStrategy tableShardingKeyStrategy : tableShardingKeyStrategyList) {
                    this.parseWrapper(SQL_COMMAND_SELECT, statement, (Parenthesis) update.getWhere(), wrapper, tableShardingKeyStrategy);
                }
            } else if (statement instanceof Delete) {
                Delete delete = (Delete) statement;
                for (TableShardingKeyStrategy tableShardingKeyStrategy : tableShardingKeyStrategyList) {
                    this.parseWrapper(SQL_COMMAND_DELETE, statement, (Parenthesis) delete.getWhere(), wrapper, tableShardingKeyStrategy);
                }

            }
        }
    }

    private static List<BusinessStrategy<?>> selectBusinessStrategies(List<String> tableShardingStrategy, String sqlSegment, AbstractWrapper<?, ?, ?> wrapper) {
        List<BusinessStrategy<?>> businessStrategies = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tableShardingStrategy)) {
            for (String businessKey : tableShardingStrategy) {
                String businessKeyEqual = businessKey + " =";
                if (sqlSegment.contains(businessKeyEqual)) {
                    String pKey = sqlSegment.split(businessKey + " =")[1].split("paramNameValuePairs.")[1].split("}")[0];
                    Object valueObj = wrapper.getParamNameValuePairs().get(pKey);
                    if (valueObj != null && StringUtils.isNotBlank(valueObj.toString())) {
                        BusinessStrategy<String> businessStrategy = new BusinessStrategy<>();
                        businessStrategy.setKey(businessKey);
                        businessStrategy.setValue(valueObj.toString());
                        businessStrategies.add(businessStrategy);
                    }
                }
            }
        }
        return businessStrategies;
    }

    private void parseWrapper(String sqlCommand, Statement statement, Parenthesis where, AbstractWrapper<?, ?, ?> wrapper, TableShardingKeyStrategy tableShardingKeyStrategy) {
        if (tableShardingKeyStrategy != null) {
            // 对于wrapper操作，自动填充分片键和值
            List<EqualsTo> appendShardKeyValList = new ArrayList<>();
            String sqlSegment = wrapper.getExpression().getSqlSegment();
            // 只有等于、或者IN条件满足匹配到对应分片键
            boolean hasTableShardKey = sqlSegment.contains(tableShardingKeyStrategy.getTableShardKey() + StringPool.SPACE + SqlKeyword.EQ.getSqlSegment())
                    || sqlSegment.contains(tableShardingKeyStrategy.getTableShardKey() + StringPool.SPACE + SqlKeyword.IN.getSqlSegment());
            boolean hasDataBaseShardKey = sqlSegment.contains(tableShardingKeyStrategy.getDatabaseShardKey() + StringPool.SPACE + SqlKeyword.EQ.getSqlSegment())
                    || sqlSegment.contains(tableShardingKeyStrategy.getDatabaseShardKey() + StringPool.SPACE + SqlKeyword.IN.getSqlSegment());
            if (!hasTableShardKey || !hasDataBaseShardKey) {
                BusinessKeyStrategy businessKeyStrategy = this.selectBusinessKeyStrategy(statement, sqlSegment, wrapper, tableShardingKeyStrategy);
                if (!hasTableShardKey) {
                    ShardingValueStrategy shardingKeyValueStrategy = super.findShardingKeyValueStrategy(businessKeyStrategy, tableShardingKeyStrategy);
                    if (shardingKeyValueStrategy != null && StringUtils.isNotBlank(shardingKeyValueStrategy.getTableShardValue())) {
                        EqualsTo equalsTo = combineEqualsTo(tableShardingKeyStrategy.getTableShardKey()
                                , shardingKeyValueStrategy.getTableShardValue());
                        appendShardKeyValList.add(equalsTo);
                    } else {
                        log.warn("[{} wrapper] missing table sharding key field in sql: {}", sqlCommand, statement);
                    }
                }

                if (!hasDataBaseShardKey) {
                    ShardingValueStrategy shardingKeyValueStrategy = super.findShardingKeyValueStrategy(businessKeyStrategy, tableShardingKeyStrategy);
                    if (shardingKeyValueStrategy != null && StringUtils.isNotBlank(shardingKeyValueStrategy.getDatabaseShardValue())) {
                        EqualsTo equalsTo = combineEqualsTo(tableShardingKeyStrategy.getDatabaseShardKey()
                                , shardingKeyValueStrategy.getDatabaseShardValue());
                        appendShardKeyValList.add(equalsTo);
                    } else {
                        log.warn("[{} wrapper] missing database sharding key field in sql: {}", sqlCommand, statement);
                    }
                }

                if (CollectionUtils.isNotEmpty(appendShardKeyValList)) {
                    log.info("[{} wrapper] pre sql: {}", sqlCommand, statement);
                    appendShardKeyValList.forEach(equalsTo -> {
                        AndExpression andExpression = new AndExpression(where.getExpression(), equalsTo);
                        where.setExpression(andExpression);
                    });
                    log.info("[{} wrapper] auto fill sharding key field to sql: {}", sqlCommand, statement);
                }
            }
        }
    }


    /**
     * 从必填业务字段列表、任意业务字段列表解析设置相关策略对应的业务键值，后续可通过其内容，使用分片键值查找器找到分库分表键值对，进行自动填充
     */
    private BusinessKeyStrategy selectBusinessKeyStrategy(Statement statement, String sqlSegment, AbstractWrapper<?, ?, ?> wrapper
            , TableShardingKeyStrategy tableShardingStrategy) {
        BusinessKeyStrategy businessKeyStrategy = new BusinessKeyStrategy();
        // 分片建列表
        ShardingKeyStrategy shardingKeyStrategy = new ShardingKeyStrategy();
        shardingKeyStrategy.setTableShardKey(tableShardingStrategy.getTableShardKey());
        shardingKeyStrategy.setDatabaseShardKey(tableShardingStrategy.getDatabaseShardKey());
        businessKeyStrategy.setShardingKeyStrategy(shardingKeyStrategy);
        // 必填字段列表
        List<BusinessStrategy<?>> necessaryBusinessKeys = selectBusinessStrategies(tableShardingStrategy.getNecessaryBusinessKeys(), sqlSegment, wrapper);
        // 任意字段列表
        List<BusinessStrategy<?>> anyOneBusinessKeys = selectBusinessStrategies(tableShardingStrategy.getAnyOneBusinessKeys(), sqlSegment, wrapper);

        /*
         *      1.如果配置有必填字段列表，SQL必须满足有搭配所有必填字段，才能为后续提供自动查询出分库分表键进行填充
         *      2.如果配置有任意字段列表，SQL须满足出现任意字段的条件，才能为后续提供自动查询出分库分表键进行填充
         *      3.如果两者都配置，则SQL需要同时满足上述场景1以及场景2
         */
        checkBusinessStrategies(statement, tableShardingStrategy, necessaryBusinessKeys, anyOneBusinessKeys);
        businessKeyStrategy.setNecessaryBusinessKeys(necessaryBusinessKeys);
        businessKeyStrategy.setAnyOneBusinessKeys(anyOneBusinessKeys);
        return businessKeyStrategy;
    }


    private EqualsTo combineEqualsTo(String shardKey, String value) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(shardKey));
        equalsTo.setRightExpression(new StringValue(value));
        return equalsTo;
    }


}
