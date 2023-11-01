package com.windbell.shardingkey.autofill.finder.cache;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * key过期时间信息记录
 *
 * @author windbell
 */
@Data
@AllArgsConstructor
class ExpireKey {

    /**
     * 关键字key
     */
    private String key;

    /**
     * 过期日期（时间戳）
     */
    private Long expire;

}

