package com.windsbell.shardingkey.autofill.properties;

import lombok.Data;

/**
 * 缓存配置
 *
 * @author windbell
 */
@Data
public class ShardingValueCacheProperty {

    // 是否启用缓存开关 true:开启 false：不开启，推荐开启提升体验，不填写默认不开启
    private Boolean enabled = false;

    // 缓存类型 default（本地缓存）、redis（redis缓存）、spring（spring cache缓存）
    private String type;

    // 缓存过期时间（秒）
    private Long expire;

}
