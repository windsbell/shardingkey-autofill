package com.windsbell.shardingkey.autofill.handler;

import com.windsbell.shardingkey.autofill.jsqlparser.StatementParser;
import com.windsbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.BusinessStrategy;
import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import lombok.Getter;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务字段策略执行器：
 * 匹配SQL中，找到必填业务字段和值，找到任意业务字段和值
 */
public class BusinessKeyStrategyHelper extends StatementParser {

    private final Map<String, Map<String, BusinessStrategy<Object>>> matchNecessaryColumnMap = new LinkedHashMap<>(); // 匹配必填业务字段map

    private final Map<String, Map<String, BusinessStrategy<Object>>> matchAnyOneColumnMap = new LinkedHashMap<>(); // 匹配任意业务字段map

    private final List<?> parameterList; // 占位符内容值列表

    @Getter
    private Map<String, TableShardingKeyStrategy> shardingKeyStrategyMap; // 表分片键映射策略map

    @Getter
    private List<BusinessKeyStrategy> businessKeyStrategyList;

    public BusinessKeyStrategyHelper(Statement statement, List<TableShardingKeyStrategy> tableShardingKeyStrategyList, List<?> parameterList) {
        super(statement);
        this.parameterList = parameterList;
        init(tableShardingKeyStrategyList);
    }

    private void init(List<TableShardingKeyStrategy> tableShardingKeyStrategyList) {
        this.shardingKeyStrategyMap = tableShardingKeyStrategyList.stream().collect(Collectors.toMap(TableShardingKeyStrategy::getTable, o -> o, (o1, o2) -> o1));
        this.businessKeyStrategyList = new ArrayList<>(shardingKeyStrategyMap.size());
        for (TableShardingKeyStrategy tableShardingKeyStrategy : tableShardingKeyStrategyList) {
            setColumnMap(matchNecessaryColumnMap, tableShardingKeyStrategy, tableShardingKeyStrategy.getNecessaryBusinessKeys());
            setColumnMap(matchAnyOneColumnMap, tableShardingKeyStrategy, tableShardingKeyStrategy.getAnyOneBusinessKeys());
        }
    }

    private void setColumnMap(Map<String, Map<String, BusinessStrategy<Object>>> matchCloumMap, TableShardingKeyStrategy tableShardingKeyStrategy, List<String> businessKeys) {
        if (!matchCloumMap.containsKey(tableShardingKeyStrategy.getTable())) {
            if (!CollectionUtils.isEmpty(businessKeys)) {
                Map<String, BusinessStrategy<Object>> businessMap = new LinkedHashMap<>();
                for (String necessaryBusinessKey : businessKeys) {
                    BusinessStrategy<Object> businessStrategy = new BusinessStrategy<>();
                    businessStrategy.setKey(necessaryBusinessKey);
                    businessStrategy.setValue(null);
                    businessMap.put(necessaryBusinessKey, businessStrategy);
                }
                matchCloumMap.put(tableShardingKeyStrategy.getTable(), businessMap);
            }
        }
    }

    public void doMatch() {
        statement.accept(this);
        shardingKeyStrategyMap.forEach((k, v) -> {
            BusinessKeyStrategy businessKeyStrategy = new BusinessKeyStrategy();
            businessKeyStrategy.setTable(k);
            businessKeyStrategy.setShardingKeyStrategy(v);
            Map<String, BusinessStrategy<Object>> necessaryMap = matchNecessaryColumnMap.get(k);
            if (necessaryMap != null) {
                businessKeyStrategy.setNecessaryBusinessKeys(new ArrayList<>(necessaryMap.values()));
            }
            Map<String, BusinessStrategy<Object>> anyOneMap = matchAnyOneColumnMap.get(k);
            if (anyOneMap != null) {
                businessKeyStrategy.setNecessaryBusinessKeys(new ArrayList<>(anyOneMap.values()));
            }
            this.businessKeyStrategyList.add(businessKeyStrategy);
        });
    }

    @Override
    public void visit(PlainSelect item) {
        super.visit(item);
        matchBody(item);
    }

    private void matchBody(PlainSelect plainSelect) {
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table) {
            matchTable(plainSelect.getWhere(), (Table) fromItem);
        }

        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                FromItem rightItem = join.getRightItem();
                if (rightItem instanceof Table) {
                    matchTable(plainSelect.getWhere(), (Table) rightItem);
                }
            }
        }
    }

    private void matchTable(Expression expression, Table table) {
        String tableName = table.getName();
        Map<String, BusinessStrategy<Object>> necessaryMap = matchNecessaryColumnMap.get(tableName);
        Map<String, BusinessStrategy<Object>> anyOneMap = matchAnyOneColumnMap.get(tableName);
        matchColumn(expression, necessaryMap, anyOneMap);
    }

    private void matchColumn(Expression expression, Map<String, BusinessStrategy<Object>> necessaryMap, Map<String, BusinessStrategy<Object>> anyOneMap) {
        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            Expression leftExpression = binaryExpression.getLeftExpression();
            Expression rightExpression = binaryExpression.getRightExpression();
            if (leftExpression instanceof Column) {
                Column column = (Column) leftExpression;
                String columnName = column.getColumnName();
                putBusinessMap(rightExpression, columnName, necessaryMap);
                putBusinessMap(rightExpression, columnName, anyOneMap);
            } else {
                matchColumn(leftExpression, necessaryMap, anyOneMap);
            }
            matchColumn(rightExpression, necessaryMap, anyOneMap);
        }
    }

    private void putBusinessMap(Expression rightExpression, String columnName, Map<String, BusinessStrategy<Object>> businessMap) {
        if (businessMap != null && businessMap.containsKey(columnName)) {
            Object columnValue = null;
            if (rightExpression instanceof JdbcParameter) {
                JdbcParameter jdbcParameter = (JdbcParameter) rightExpression;
                Integer index = jdbcParameter.getIndex();
                if (parameterList.size() > index) {
                    // 提取占位符对应的字段值 (param index从1开始)
                    columnValue = parameterList.get(index - 1);
                }
            } else {
                // String 、 Time 、Long 、等值
                columnValue = rightExpression.toString();
            }
            BusinessStrategy<Object> businessStrategy = businessMap.get(columnName);
            if (businessStrategy.getValue() == null) {
                businessStrategy.setValue(columnValue); // 补充业务键内容
            }
        }
    }
}
