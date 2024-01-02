package com.windsbell.shardingkey.autofill.strategy;

import lombok.Data;

import java.util.List;

/**
 * 业务分片键字段映射策略
 *
 * @author windbell
 */
@Data
public class BusinessKeyStrategy {

    private String table; // 表名

    ShardingKeyStrategy shardingKeyStrategy; // 分片键字段映射策略[分表键、分库键]

    private List<BusinessStrategy<?>> necessaryBusinessKeys;  // 必要业务键列表[条件中必须出现的业务键,通过其中出现的所有业务键可查出分库分表等键值对]

    private List<BusinessStrategy<?>> anyOneBusinessKeys; // 任意业务键列表[条件中出现以下任意一个业务键即可满足可查出分库分表等键值对]

}
