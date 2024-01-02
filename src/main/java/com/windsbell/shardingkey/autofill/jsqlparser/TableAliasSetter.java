package com.windsbell.shardingkey.autofill.jsqlparser;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL表别名设置器
 * 检测sql查询中是否有设置表别名，未设置的话，填充设置表别名，同时应用到select、where、order by、group by、having等字段中
 * 注： 最大支持包含1000个表的SQL的别名设置，且只匹配没有join的sql语句，有join的sql语句应该是业务方自行设置别名
 *
 * @author windbell
 */
public class TableAliasSetter extends StatementParser {

    private Integer round = 0; // 解析轮数

    private int counter = 0; // 别名计数器

    protected List<String> alias; // 解析的表别名集合

    public TableAliasSetter(Statement statement) {
        super(statement);
        init();
    }

    private void init() {
        alias = new ArrayList<>();
        statement.accept(this);
        round++;
    }

    public synchronized void setTableAlias() {
        statement.accept(this);
    }

    /**
     * 补充表别名， 以t1，t2形式递增
     */
    protected synchronized String getNextAlias() {
        if (counter < 1000) {
            counter++;
            String nextAlias = "t" + counter;
            if (alias.contains(nextAlias)) {
                return getNextAlias();
            }
            return nextAlias;
        }
        throw new RuntimeException("can`t set table alias more than 1000 tables!");
    }

    @Override
    public void visit(Table tableName) {
        if (round == 0) { // 第一轮收集SQL现有的别名集合
            super.visit(tableName);
            if (tableName.getAlias() != null && !alias.contains(tableName.getAlias().getName())) {
                alias.add(tableName.getAlias().getName());
            }
        }
    }

    @Override
    public void visit(PlainSelect item) {
        super.visit(item);
        if (round > 0) {
            // 第一轮之后，补充SQL中为出现别名的条件
            this.matchBody(item);
        }
    }

    private void matchBody(PlainSelect plainSelect) {
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem != null && plainSelect.getJoins() == null) {
            Alias alias = fromItem.getAlias();
            if (alias == null) {
                alias = new Alias(getNextAlias());
                fromItem.setAlias(alias);
            }
            Table table = new Table(alias.getName());
            this.machSelect(plainSelect.getSelectItems(), table);
            this.matchWhere(plainSelect.getWhere(), table);
            this.matchOrderBy(plainSelect.getOrderByElements(), table);
            this.matchGroupBy(plainSelect.getGroupBy(), table);
            this.matchHaving(plainSelect.getHaving(), table);
        }
    }

    private void machSelect(List<SelectItem> selectItems, Table table) {
        if (selectItems != null) {
            for (SelectItem selectItem : selectItems) {
                if (selectItem instanceof SelectExpressionItem) {
                    SelectExpressionItem selectExpressionItem = (SelectExpressionItem) selectItem;
                    Expression expression = selectExpressionItem.getExpression();
                    if (expression instanceof Column) {
                        this.setTableAlias((Column) expression, table);
                    }
                }
            }
        }
    }

    private void matchWhere(Expression expression, Table table) {
        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            Expression leftExpression = binaryExpression.getLeftExpression();
            Expression rightExpression = binaryExpression.getRightExpression();
            this.matchWhere(leftExpression, rightExpression, table);
        } else if (expression instanceof InExpression) {
            InExpression inExpression = (InExpression) expression;
            Expression leftExpression = inExpression.getLeftExpression();
            Expression rightExpression = inExpression.getRightExpression();
            this.matchWhere(leftExpression, rightExpression, table);
        }
    }

    private void matchWhere(Expression leftExpression, Expression rightExpression, Table table) {
        if (leftExpression instanceof Column) {
            this.setTableAlias((Column) leftExpression, table);
        } else {
            this.matchWhere(leftExpression, table);
        }
        this.matchWhere(rightExpression, table);
    }

    private void matchOrderBy(List<OrderByElement> orderByElements, Table table) {
        if (orderByElements != null) {
            for (OrderByElement orderByElement : orderByElements) {
                Expression expression = orderByElement.getExpression();
                if (expression instanceof Column) {
                    this.setTableAlias((Column) expression, table);
                }
            }
        }
    }

    private void matchGroupBy(GroupByElement groupBy, Table table) {
        if (groupBy != null) {
            for (Expression expression : groupBy.getGroupByExpressionList().getExpressions()) {
                if (expression instanceof Column) {
                    this.setTableAlias((Column) expression, table);
                }
            }
        }
    }

    private void matchHaving(Expression expression, Table table) {
        if (expression != null) {
            this.matchWhere(expression, table);
        }
    }

    private void setTableAlias(Column column, Table table) {
        if (column.getTable() == null) {
            column.setTable(table); // 添加列别名
        }
    }


}

