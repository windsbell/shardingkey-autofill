package com.windsbell.shardingkey.autofill.jsqlparser;

import net.sf.jsqlparser.statement.Statement;

import java.util.List;

/**
 * SQL表名查找器
 *
 * @author windbell
 */
public class TableNameFinder extends StatementParser {

    public TableNameFinder(Statement statement) {
        super(statement);
        statement.accept(this);
    }

    public List<String> getTableList() {
        return super.tables;
    }

}
