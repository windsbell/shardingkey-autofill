package com.windsbell.shardingkey.autofill.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.windsbell.shardingkey.autofill.finder.cache.ShardingValueCacheFactory;
import com.windsbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windsbell.shardingkey.autofill.strategy.ShardingValueStrategy;
import com.windsbell.shardingkey.autofill.strategy.TableShardingKeyStrategy;
import net.sf.jsqlparser.statement.Statement;
import org.apache.ibatis.binding.MapperMethod;
import org.springframework.lang.Nullable;

/**
 * 核心抽象自动填充分片键策略处理类
 *
 * @author windbell
 */
public abstract class AbstractShardingStrategyHandler extends ShardingValueCacheFactory implements ShardingStrategyHandler {

    // SQL类型：查询
    protected final static String SQL_COMMAND_SELECT = "select";

    // SQL类型：修改
    protected final static String SQL_COMMAND_UPDATE = "update";

    // SQL类型：删除
    protected final static String SQL_COMMAND_DELETE = "delete";

    /**
     * 对应策略要执行的解析动作
     * statement:预处理语句
     * parameterObject: 替换参数对象
     * tableShardingKeyStrategy: 表分片键映射策略
     */
    public abstract void parse(Statement statement, Object parameterObject, TableShardingKeyStrategy tableShardingKeyStrategy);

    /**
     * 尝试获取到Wrapper,若拿不到则是mapper
     */
    @Nullable
    protected AbstractWrapper<?, ?, ?> tryAndGetWrapper(Object methodObj) {
        AbstractWrapper<?, ?, ?> wrapper = null;
        if (methodObj instanceof MapperMethod.ParamMap) {
            MapperMethod.ParamMap<?> paramMap = (MapperMethod.ParamMap<?>) methodObj;
            if (paramMap.containsKey("ew")) {
                Object obj = paramMap.get("ew");
                if (obj instanceof AbstractWrapper) {
                    wrapper = (AbstractWrapper<?, ?, ?>) obj;
                }
            }
        }
        return wrapper;
    }

    /**
     * 公共方法：通过业务键策略，使用分片键查找器查到对应分片键值内容，再置入cache，后面同样语句类型直接从cache中拿取
     */
    protected ShardingValueStrategy findShardingKeyValueStrategy(BusinessKeyStrategy businessKeyStrategy, TableShardingKeyStrategy tableShardingKeyStrategy) {
        return ShardingValueCacheFactory.getInstance().get(businessKeyStrategy, tableShardingKeyStrategy.getShardingValueFinder());
    }


}
