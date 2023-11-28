package com.windsbell.shardingkey.autofill.strategy;

import com.windsbell.shardingkey.autofill.finder.ShardingValueFinder;
import lombok.Data;

import java.util.List;

/**
 * 表分片键映射策略
 *
 * @author windbell
 */
@Data
public class TableShardingKeyStrategy extends ShardingKeyStrategy {

    private String table; // 表名

    private ShardingValueFinder shardingValueFinder; // 表对应的分片键查找器Class

    private List<String> necessaryBusinessKeys; // 必要业务键列表[条件中必须出现的业务键,通过其中出现的所有业务键可查出分库分表等键值对]

    private List<String> anyOneBusinessKeys; // 任意业务键列表[条件中出现以下任意一个业务键即可满足可查出分库分表等键值对]

    private String errorNotHasNecessaryBusinessKeys; // 检测提示未设置必要业务键列表

    private String errorNotHasAnyOneBusinessKeys; // 检测提示未设置任意业务键列表

    private String errorNotHaseDatabaseShardKey; // 检测提示未设置分库键

    private String errorNotHaseTableShardKey; // 检测提示未设置分表键


}


