package com.windsbell.shardingkey.autofill.strategy;

import lombok.Data;

/**
 * 业务字段映射策略
 *
 * @author windbell
 */
@Data
public class BusinessStrategy<V> {

    private String Key;   // 业务唯一键字段

    private V value; // 业务唯一键字段值
}
