package com.windsbell.shardingkey.autofill.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.windsbell.shardingkey.autofill.jsqlparser.CourseExplain;
import com.windsbell.shardingkey.autofill.logger.CustomerLogger;
import com.windsbell.shardingkey.autofill.logger.CustomerLoggerFactory;
import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import net.sf.jsqlparser.statement.Statement;

import java.util.List;
import java.util.Map;

/**
 * wrapper类型SQL自动填充分片键策略处理类
 *
 * @author windbell
 */
public class Wrapper2ShardingStrategyHandler extends AbstractShardingStrategyHandler implements ShardingStrategyHandler {

    private static final CustomerLogger log = CustomerLoggerFactory.getLogger(Wrapper2ShardingStrategyHandler.class);

    /**
     * statement:预处理语句
     * parameterObject: 替换参数对象
     * tableShardingKeyStrategyList: 表分片键映射策略 （单表一条，多表多条）
     */
    @Override
    public void parse(Statement statement, Object parameterObject, List<TableShardingKeyStrategy> tableShardingKeyStrategyList) {
        AbstractWrapper<?, ?, ?> wrapper = super.tryAndGetWrapper(parameterObject);
        if (wrapper != null) {
            Map<String, Object> paramNameValuePairs = wrapper.getParamNameValuePairs();
            List<?> parameterList = getParameterList(paramNameValuePairs, Constants.WRAPPER_PARAM);
            CourseExplain courseExplain = super.doFill(statement, tableShardingKeyStrategyList, parameterList);
            log.info("[wrapper: {}]", courseExplain);
        }
    }


}
