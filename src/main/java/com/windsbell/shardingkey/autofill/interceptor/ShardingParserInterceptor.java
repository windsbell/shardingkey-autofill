package com.windsbell.shardingkey.autofill.interceptor;


import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.windsbell.shardingkey.autofill.config.TableShardingStrategyHelper;
import com.windsbell.shardingkey.autofill.handler.ShardingStrategyHandler;
import com.windsbell.shardingkey.autofill.logger.CustomerLogger;
import com.windsbell.shardingkey.autofill.logger.CustomerLoggerFactory;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statements;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.util.stream.Collectors;

/**
 * 分片解析拦截器：针对wrapper拦截,补充分片键 ,同时对mapper检测是否有分片键
 *
 * @author windbell
 */
public class ShardingParserInterceptor extends JsqlParserSupport implements InnerInterceptor {

    private static final CustomerLogger log = CustomerLoggerFactory.getLogger(ShardingParserInterceptor.class);

    ShardingStrategyHandler shardingStrategyHandler;

    public ShardingParserInterceptor(ShardingStrategyHandler shardingStrategyHandler) {
        this.shardingStrategyHandler = shardingStrategyHandler;
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler handler = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = handler.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();
        if (sct == SqlCommandType.SELECT || sct == SqlCommandType.DELETE || sct == SqlCommandType.UPDATE) {
            if (InterceptorIgnoreHelper.willIgnoreBlockAttack(ms.getId())) return;
            BoundSql boundSql = handler.boundSql();
            Object parameterObject = handler.parameterHandler().getParameterObject();
            String parse = parserMulti(boundSql.getSql(), parameterObject);
            PluginUtils.mpBoundSql(boundSql).sql(parse);
        }
    }

    @Override
    public String parserMulti(String sql, Object obj) {
        log.debug("original SQL: " + sql);
        Statements statements;
        try {
            statements = CCJSqlParserUtil.parseStatements(sql);
        } catch (JSQLParserException e) {
            log.warn("Failed to process, Error SQL: " + sql, e);
            return sql;
        }
        return statements.getStatements().stream()
                .map(statement -> {
                    shardingStrategyHandler.parse(statement, obj, TableShardingStrategyHelper.find(statement));
                    return statement.toString();
                }).collect(Collectors.joining(StringPool.SEMICOLON))
                + StringPool.SEMICOLON;
    }

}
