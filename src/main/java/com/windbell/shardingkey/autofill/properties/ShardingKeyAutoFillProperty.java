package com.windbell.shardingkey.autofill.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 自动注册框架配置
 *
 * @author windbell
 */
@Data
@ConfigurationProperties(prefix = "spring.shardingkey-autofill")
public class ShardingKeyAutoFillProperty {

    private CacheProperty cache; // 缓存配置

    private String type; // 自动填充实现类

    private Boolean logEnabled = true; // 启用日志开关

    private List<TableShardingKeyProperty> strategies; // 分片键配置策略集


}
