package com.windbell.shardingkey.autofill.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启自动填充分片键策略开关
 *
 * @author windbell
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ShardingKeyAutoFillConfiguration.class})
public @interface EnableShardingKeyAutoFill {
}
