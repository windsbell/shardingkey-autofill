package com.windsbell.shardingkey.autofill.jsqlparser;

import lombok.Getter;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 核心API SQL分片键填充
 *
 * @author windbell
 */
@Getter
public class ShardingKeyFiller extends StatementParser {

    private Map<String, String> aliasTablesMap; // 【别名 表名】

    private Map<String, String> tablesAliasMap; // 【表名 别名】

    private Map<String, Map<String, String>> shardingKeyValueMap; // 【表名  分片键字段, 分片键内容】

    private Map<String, Map<String, Boolean>> shardingKeyHitFlagMap; // 【表名  分片键，是否设置 】

    private Map<String, Map<String, Boolean>> firstShardingKeyHitFlagMap; // 【表名  分片键，是否设置 】原始检测收集的结果

    public ShardingKeyFiller(Statement statement, Map<String, Map<String, String>> shardingKeyValueMap, Map<String, Map<String, Boolean>> firstShardingKeyHitFlagMap) {
        super(statement);
        init(shardingKeyValueMap, firstShardingKeyHitFlagMap);
    }

    private void init(Map<String, Map<String, String>> shardingKeyValueMap, Map<String, Map<String, Boolean>> firstShardingKeyHitFlagMap) {
        this.aliasTablesMap = new LinkedHashMap<>();
        this.tablesAliasMap = new LinkedHashMap<>();
        this.shardingKeyValueMap = shardingKeyValueMap;
        this.shardingKeyHitFlagMap = initFillShardingKeyFlagMap(shardingKeyValueMap);
        this.firstShardingKeyHitFlagMap = firstShardingKeyHitFlagMap;
    }

    // 开始填充分片键
    public void doFill() {
        statement.accept(this);
        checkHasHitSharingKey(); // 在填充分片键完成后，再次检测是否填充成功，未成功则抛出异常
    }

    // 复制是否命中分片键Map
    private Map<String, Map<String, Boolean>> copyShardingKeyHitFlagMap(Map<String, Map<String, Boolean>> fillShardingKeyFlagMap) {
        Map<String, Map<String, Boolean>> copyFillShardingKeyFlagMap = new LinkedHashMap<>();
        fillShardingKeyFlagMap.forEach((k, v) -> {
            Map<String, Boolean> innerMap = new LinkedHashMap<>(v);
            copyFillShardingKeyFlagMap.put(k, innerMap);
        });
        return copyFillShardingKeyFlagMap;
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
                    stringBuilder.append("table:").append(k).append(",sharding key:").append(notHitList).append(" dit`t find! \n");
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
        // 每一轮新的select，重置为最开始的检测分片键是否设置收集结果
        this.shardingKeyHitFlagMap = copyShardingKeyHitFlagMap(firstShardingKeyHitFlagMap);
        super.visit(item);
    }

    @Override
    public void visitBinaryExpression(BinaryExpression binaryExpression) {
        // 只有等于、或者IN条件满足匹配到对应分片键
        this.fill(binaryExpression);
        binaryExpression.getLeftExpression().accept(this);
        binaryExpression.getRightExpression().accept(this);
    }

    // 对未命中分片键的，填充分片键
    private void fill(BinaryExpression binaryExpression) {
        shardingKeyHitFlagMap.forEach((k, v) -> {
            Map<String, String> shardingKeyValueInnerMap = shardingKeyValueMap.get(k);
            String alias = tablesAliasMap.get(k);
            if (alias != null) {
                Table table = new Table(alias);
                v.forEach((m, n) -> {
                    if (!n) {
                        String shardingKeyValue = shardingKeyValueInnerMap.get(m);
                        if (StringUtils.isNotBlank(shardingKeyValue)) {
                            EqualsTo append = combineEqualsTo(table, m, shardingKeyValue);
                            AndExpression andExpression = new AndExpression(binaryExpression.getRightExpression(), append);
                            binaryExpression.setRightExpression(andExpression);
                            v.put(m, true);
                        }
                    }
                });
            }
        });
    }

    private EqualsTo combineEqualsTo(Table table, String shardKey, String value) {
        EqualsTo equalsTo = new EqualsTo();
        Column column = new Column(table, shardKey);
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(new StringValue(value));
        return equalsTo;
    }

}
