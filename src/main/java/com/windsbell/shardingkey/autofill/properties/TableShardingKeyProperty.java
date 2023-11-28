package com.windsbell.shardingkey.autofill.properties;

import com.windsbell.shardingkey.autofill.strategy.ShardingKeyStrategy;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


/**
 * 表分片策略配置
 *
 * @author windbell
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TableShardingKeyProperty extends ShardingKeyStrategy {

    private List<String> suitableTables; // 适配对应分片策略的表集合

    private List<String> necessaryBusinessKeys; // 必要业务键列表[条件中必须出现的业务键,通过其中所有业务键可查出分库分表等键值对]

    private List<String> anyOneBusinessKeys; // 任意业务键列表[条件中出现以下任意一个业务键即可满足可查出分库分表等键值对]

}
