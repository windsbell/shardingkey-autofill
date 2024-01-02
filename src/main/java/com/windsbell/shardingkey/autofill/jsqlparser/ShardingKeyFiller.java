package com.windsbell.shardingkey.autofill.jsqlparser;

import lombok.Getter;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 核心API SQL分片键填充
 *
 * @author windbell
 */
@Getter
public class ShardingKeyFiller extends StatementParser {

    private Integer round; // 解析轮数

    private Map<String, String> aliasTablesMap; // 【别名 表名】

    private Map<String, String> tablesAliasMap; // 【表名 别名】

    private Map<String, Map<String, String>> shardingKeyValueMap; // 【表名  分片键字段, 分片键内容】

    private Map<String, Map<String, Boolean>> shardingKeyHitFlagMap; // 【表名  分片键，是否设置 】

    private Map<String, Map<String, Boolean>> firstRoundShardingKeyHitFlagMap; // 【表名  分片键，是否设置 】第一轮检测收集的结果

    public ShardingKeyFiller(Map<String, Map<String, String>> shardingKeyValueMap, Statement statement) {
        super(statement);
        init(shardingKeyValueMap);
    }

    private void init(Map<String, Map<String, String>> shardingKeyValueMap) {
        round = 0; // 解析轮数
        this.aliasTablesMap = new LinkedHashMap<>();
        this.tablesAliasMap = new LinkedHashMap<>();
        this.shardingKeyValueMap = shardingKeyValueMap;
        this.shardingKeyHitFlagMap = initFillShardingKeyFlagMap(shardingKeyValueMap);
        statement.accept(this); // 第一轮收集SQL条件中是否命中相关分片键
        this.firstRoundShardingKeyHitFlagMap = copyShardingKeyHitFlagMap(shardingKeyHitFlagMap); // 保存第一轮收集的结果
        round++; // 轮数
    }

    // 开始填充分片键
    public void doFill() {
        statement.accept(this); // 第二轮对未命中相关分片键的条件，补充设置分片键
        checkHasHitSharingKey(); // 在填充分片键完成后，再次检测是否填充成功，未成功则抛出异常
    }

    // 复制是否命中分片键Map
    private Map<String, Map<String, Boolean>> copyShardingKeyHitFlagMap(Map<String, Map<String, Boolean>> fillShardingKeyFlagMap) {
        Map<String, Map<String, Boolean>> copyFillShardingKeyFlagMap = new LinkedHashMap<>();
        fillShardingKeyFlagMap.forEach((k, v) -> {
            Map<String, Boolean> innerMap = new HashMap<>(v);
            copyFillShardingKeyFlagMap.put(k, innerMap);
        });
        return copyFillShardingKeyFlagMap;
    }

    // 初始化是否命中分片键Map 默认都为未命中
    private Map<String, Map<String, Boolean>> initFillShardingKeyFlagMap(Map<String, Map<String, String>> fillShardingKeyMap) {
        Map<String, Map<String, Boolean>> fillShardingKeyFlagMap = new LinkedHashMap<>();
        fillShardingKeyMap.forEach((k, v) -> {
            Map<String, Boolean> innerMap = new HashMap<>();
            v.forEach((m, n) -> innerMap.put(m, false));
            fillShardingKeyFlagMap.put(k, innerMap);
        });
        return fillShardingKeyFlagMap;
    }

    private void checkHasHitSharingKey() {
        StringBuilder stringBuilder = new StringBuilder();
        shardingKeyHitFlagMap.forEach((k, v) -> {
            if (aliasTablesMap.containsValue(k)) {
                Map<String, Boolean> innerMap = shardingKeyHitFlagMap.get(k);
                List<String> notHitList = new ArrayList<>();
                innerMap.forEach((m, n) -> {
                    if (!n) {
                        notHitList.add(m);
                    }
                });
                if (!notHitList.isEmpty()) {
                    stringBuilder.append("table:").append(k).append(",sharding key:").append(notHitList).append(" dit not find! \n");
                }
            }
        });
        Assert.isTrue(stringBuilder.length() == 0, "parse SQL error: \n" + statement + " \n" + stringBuilder);
    }

    @Override
    public void visit(Table tableName) {
        super.visit(tableName);
        String tableWholeName = extractTableName(tableName);
        if (!aliasTablesMap.containsKey(tableWholeName)) {
            String alias = tableName.getAlias() != null ? tableName.getAlias().getName() : tableWholeName;
            aliasTablesMap.put(alias, tableWholeName);
            tablesAliasMap.put(tableWholeName, alias);
        }
    }


    @Override
    public void visit(PlainSelect item) {
        if (round > 0) {
            // 第一轮解析后,每到新的select时，重置为第一轮的检测收集结果
            this.shardingKeyHitFlagMap = copyShardingKeyHitFlagMap(firstRoundShardingKeyHitFlagMap);
        }
        super.visit(item);
    }

    @Override
    public void visit(InExpression inExpression) {
        if (round == 0) {
            // 只有等于、或者IN条件满足匹配到对应分片键
            Expression leftExpression = inExpression.getLeftExpression();
            if (leftExpression instanceof Column) {
                this.parse((Column) leftExpression);
            }
        }
        super.visit(inExpression);
    }

    @Override
    public void visitBinaryExpression(BinaryExpression binaryExpression) {
        // 只有等于、或者IN条件满足匹配到对应分片键
        this.parse(binaryExpression);
        this.fill(binaryExpression);
        binaryExpression.getLeftExpression().accept(this);
        binaryExpression.getRightExpression().accept(this);
    }

    // 扫描解析，标记哪些字段命中分片键，哪些没有命中
    private void parse(BinaryExpression binaryExpression) {
        if (round == 0) {
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
    }

    // 第一轮解析后,对未命中分片键的，填充分片键
    private void fill(BinaryExpression binaryExpression) {
        if (round > 0) {
            shardingKeyHitFlagMap.forEach((k, v) -> {
                Map<String, String> shardingKeyValueInnerMap = shardingKeyValueMap.get(k);
                String alias = tablesAliasMap.get(k);
                if (alias != null) {
                    Table table = new Table(alias);
                    v.forEach((m, n) -> {
                        if (!n) {
                            String shardingKeyValue = shardingKeyValueInnerMap.get(m);
                            EqualsTo append = combineEqualsTo(table, m, shardingKeyValue);
                            AndExpression andExpression = new AndExpression(binaryExpression.getRightExpression(), append);
                            binaryExpression.setRightExpression(andExpression);
                            v.put(m, true);
                        }
                    });
                }
            });
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

    private EqualsTo combineEqualsTo(Table table, String shardKey, String value) {
        EqualsTo equalsTo = new EqualsTo();
        Column column = new Column(table, shardKey);
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(new StringValue(value));
        return equalsTo;
    }

}
