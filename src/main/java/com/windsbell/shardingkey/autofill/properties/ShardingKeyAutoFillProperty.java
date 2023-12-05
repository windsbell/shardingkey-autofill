package com.windsbell.shardingkey.autofill.properties;

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

    private ShardingValueCacheProperty cache; // 缓存配置 [开启的情况下可使用：default（未填写则是默认本地缓存）、redis（redis缓存）、spring（spring cache缓存）]

    private Boolean logEnabled = true; //  启用拦截日志开关[建议线上环境关闭、测试环境开启]

    private List<TableShardingKeyProperty> strategies; //  策略集 [分片键都相同的一组数据表作为一个策略集进行配置]

}
