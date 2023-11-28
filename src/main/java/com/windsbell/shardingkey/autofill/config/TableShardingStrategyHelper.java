package com.windsbell.shardingkey.autofill.config;

import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.lang.Nullable;

import java.util.concurrent.ConcurrentHashMap;


/**
 * 表分片键映射策略助手
 *
 * @author windbell
 */
public class TableShardingStrategyHelper {

    /**
     * 表分片键策略缓存 key：表名 value：表策略
     */
    private final static ConcurrentHashMap<String, TableShardingKeyStrategy> TABLE_STRATEGY_CACHE = new ConcurrentHashMap<>();

    /**
     * statement:通过预处理语句，获取对应表的分片键策略
     */
    @Nullable
    public static TableShardingKeyStrategy find(Statement statement) {
        String tableName = null;
        if (statement instanceof Select) {
            Select select = (Select) statement;
            if (select.getSelectBody() instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
                Table table = (Table) plainSelect.getFromItem();
                tableName = table.getName();
            }
        } else if (statement instanceof Update) {
            Update update = (Update) statement;
            tableName = update.getTable().getName();
        } else if (statement instanceof Delete) {
            Delete delete = (Delete) statement;
            tableName = delete.getTable().getName();
        }
        return TABLE_STRATEGY_CACHE.get(tableName);
    }

    protected static void put(String suitableTable, TableShardingKeyStrategy tableShardingStrategy) {
        TABLE_STRATEGY_CACHE.put(suitableTable, tableShardingStrategy);
    }

    protected static ConcurrentHashMap<String, TableShardingKeyStrategy> showALl() {
        return new ConcurrentHashMap<>(TABLE_STRATEGY_CACHE);
    }

}
