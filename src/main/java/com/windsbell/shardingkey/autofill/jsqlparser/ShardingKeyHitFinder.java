package com.windsbell.shardingkey.autofill.jsqlparser;

import lombok.Getter;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 核心API SQL分片键是否设置查找器
 *
 * @author windbell
 */
@Getter
public class ShardingKeyHitFinder extends StatementParser {

    private Map<String, String> aliasTablesMap; // 【别名 表名】

    private Map<String, Map<String, String>> shardingKeyValueMap; // 【表名  分片键字段, 分片键内容】

    private Map<String, Map<String, Boolean>> shardingKeyHitFlagMap; // 【表名  分片键，是否设置 】

    public ShardingKeyHitFinder(Statement statement, Map<String, Map<String, String>> shardingKeyValueMap) {
        super(statement);
        init(shardingKeyValueMap);
    }

    private void init(Map<String, Map<String, String>> shardingKeyValueMap) {
        this.aliasTablesMap = new LinkedHashMap<>();
        this.shardingKeyValueMap = shardingKeyValueMap;
        this.shardingKeyHitFlagMap = initFillShardingKeyFlagMap(shardingKeyValueMap);
    }

    public Map<String, Map<String, Boolean>> doFind() {
        statement.accept(this);
        return shardingKeyHitFlagMap;
    }

    // 初始化是否命中分片键Map 默认都为未命中
    private Map<String, Map<String, Boolean>> initFillShardingKeyFlagMap(Map<String, Map<String, String>> fillShardingKeyMap) {
        Map<String, Map<String, Boolean>> fillShardingKeyFlagMap = new LinkedHashMap<>();
        fillShardingKeyMap.forEach((k, v) -> {
            Map<String, Boolean> innerMap = new LinkedHashMap<>();
            v.forEach((m, n) -> innerMap.put(m, false));
            fillShardingKeyFlagMap.put(k, innerMap);
        });
        return fillShardingKeyFlagMap;
    }

    @Override
    public void visit(Table tableName) {
        super.visit(tableName);
        String tableWholeName = extractTableName(tableName);
        if (!aliasTablesMap.containsKey(tableWholeName)) {
            String alias = tableName.getAlias() != null ? tableName.getAlias().getName() : tableWholeName;
            aliasTablesMap.put(alias, tableWholeName);
        }
    }

    @Override
    public void visit(InExpression inExpression) {
        // 只有等于、或者IN条件满足匹配到对应分片键
        Expression leftExpression = inExpression.getLeftExpression();
        if (leftExpression instanceof Column) {
            this.parse((Column) leftExpression);
        }
        super.visit(inExpression);
    }

    @Override
    public void visitBinaryExpression(BinaryExpression binaryExpression) {
        // 只有等于、或者IN条件满足匹配到对应分片键
        this.parse(binaryExpression);
        binaryExpression.getLeftExpression().accept(this);
        binaryExpression.getRightExpression().accept(this);
    }

    // 扫描解析，标记哪些字段命中分片键，哪些没有命中
    private void parse(BinaryExpression binaryExpression) {
        Expression rightExpression = binaryExpression.getRightExpression();
        if (rightExpression instanceof EqualsTo) {
            EqualsTo equalsTo = (EqualsTo) rightExpression;
            Expression leftExpression = equalsTo.getLeftExpression();
            if (leftExpression instanceof Column
                    && (equalsTo.getRightExpression() instanceof StringValue || equalsTo.getRightExpression() instanceof JdbcParameter)) {
                this.parse((Column) leftExpression);
            }
        }
    }

    // 若字段命中了，标记命中
    private void parse(Column column) {
        Table table = column.getTable();
        if (table != null) {
            String alias = table.getName();
            String tableName = aliasTablesMap.get(alias);
            if (tableName != null) {
                Map<String, String> tableInnerMap = shardingKeyValueMap.get(tableName);
                if (tableInnerMap != null) {
                    String columnName = column.getColumnName();
                    if (tableInnerMap.containsKey(columnName)) {
                        shardingKeyHitFlagMap.get(tableName).put(columnName, true);  // 命中分片键字段
                    }
                }
            }
        }
    }

}
