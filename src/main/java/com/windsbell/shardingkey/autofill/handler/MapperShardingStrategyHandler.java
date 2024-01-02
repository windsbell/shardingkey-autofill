package com.windsbell.shardingkey.autofill.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.windsbell.shardingkey.autofill.logger.CustomerLogger;
import com.windsbell.shardingkey.autofill.logger.CustomerLoggerFactory;
import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import lombok.Getter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.util.Assert;

import java.util.List;


/**
 * mapper类型SQL自动填充分片键策略处理类
 * 此处理器废弃：请移步新的Mapper2ShardingStrategyHandler
 *
 * @author windbell
 */
@Deprecated
public class MapperShardingStrategyHandler extends AbstractShardingStrategyHandler implements ShardingStrategyHandler {

    @Getter
    protected boolean isEffective = false;

    private static final CustomerLogger log = CustomerLoggerFactory.getLogger(MapperShardingStrategyHandler.class);

    /**
     * statement:预处理语句
     * parameterObject: 替换参数对象
     * tableShardingKeyStrategyList: 表分片键映射策略 （单表一条，多表多条）
     */
    @Override
    public void parse(Statement statement, Object parameterObject, List<TableShardingKeyStrategy> tableShardingKeyStrategyList) {
        AbstractWrapper<?, ?, ?> wrapper = super.tryAndGetWrapper(parameterObject);
        if (wrapper == null) {
            if (statement instanceof Select) {
                Select select = (Select) statement;
                if (select.getSelectBody() instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
                    this.parseMapper(SQL_COMMAND_SELECT, statement, plainSelect.getWhere(), tableShardingKeyStrategyList);
                }
            } else if (statement instanceof Update) {
                Update update = (Update) statement;
                this.parseMapper(SQL_COMMAND_UPDATE, statement, update.getWhere(), tableShardingKeyStrategyList);
            } else if (statement instanceof Delete) {
                Delete delete = (Delete) statement;
                this.parseMapper(SQL_COMMAND_DELETE, statement, delete.getWhere(), tableShardingKeyStrategyList);
            }
        }
    }

    private void parseMapper(String sqlCommand, Statement statement, Expression where, List<TableShardingKeyStrategy> tableShardingKeyStrategyList) {
        if (CollectionUtils.isNotEmpty(tableShardingKeyStrategyList)) {
            for (TableShardingKeyStrategy tableShardingStrategy : tableShardingKeyStrategyList) {
                // 对于非wrapper的mapper操作，检查关键分片键是否存在
                boolean matchTableShardKey = this.fullMatch(where, tableShardingStrategy.getTableShardKey());
                boolean matchDatabaseShardKey = this.fullMatch(where, tableShardingStrategy.getDatabaseShardKey());
                if (!matchTableShardKey && !matchDatabaseShardKey) {
                    log.warn("[{} mapper] missing table sharding key field and database sharding key field in sql: {}", sqlCommand, statement);
                } else if (!matchTableShardKey) {
                    log.warn("[{} mapper] missing table sharding key field in sql: {}", sqlCommand, statement);
                } else if (!matchDatabaseShardKey) {
                    log.warn("[{} mapper] missing database sharding key field in sql: {}", sqlCommand, statement);
                }
                Assert.isTrue(matchTableShardKey, tableShardingStrategy.getErrorNotHaseTableShardKey());
                Assert.isTrue(matchDatabaseShardKey, tableShardingStrategy.getErrorNotHaseDatabaseShardKey());
            }
        }
    }

    private boolean fullMatch(Expression where, String logicField) {
        if (where instanceof EqualsTo) {
            EqualsTo equalsTo = (EqualsTo) where;
            if (equalsTo.getLeftExpression().toString().contains(logicField)) {
                return true;
            }
        }
        if (where instanceof InExpression) {
            InExpression in = (InExpression) where;
            if (in.getLeftExpression().toString().contains(logicField)) {
                return true;
            }
        }
        if (where instanceof AndExpression) {
            AndExpression andExpression = (AndExpression) where;
            return fullMatch(andExpression.getLeftExpression(), logicField) || fullMatch(andExpression.getRightExpression(), logicField);
        } else if (where instanceof Parenthesis) {
            Parenthesis parenthesis = (Parenthesis) where;
            return fullMatch(parenthesis.getExpression(), logicField);
        }
        return false;
    }


}
