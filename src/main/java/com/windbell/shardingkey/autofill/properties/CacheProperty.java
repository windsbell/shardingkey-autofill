package com.windbell.shardingkey.autofill.properties;

import lombok.Data;

/**
 * 缓存配置
 *
 * @author windbell
 */
@Data
public class CacheProperty {

    // 缓存类型
    private String type;

    // 缓存过期时间（秒）
    private Long expire;


}
