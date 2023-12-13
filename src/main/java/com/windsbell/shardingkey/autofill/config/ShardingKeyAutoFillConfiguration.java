package com.windsbell.shardingkey.autofill.config;


import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.windsbell.shardingkey.autofill.finder.ShardingValueHandler;
import com.windsbell.shardingkey.autofill.finder.ShardingValueHandlerFactory;
import com.windsbell.shardingkey.autofill.finder.cache.ShardingValueCachedHandler;
import com.windsbell.shardingkey.autofill.handler.AbstractShardingStrategyHandler;
import com.windsbell.shardingkey.autofill.interceptor.ShardingParserInterceptor;
import com.windsbell.shardingkey.autofill.logger.CustomerLoggerFactory;
import com.windsbell.shardingkey.autofill.properties.ShardingKeyAutoFillProperty;
import com.windsbell.shardingkey.autofill.properties.ShardingValueCacheProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;


/**
 * 开启分片键自动填充核心配置
 *
 * @author windbell
 */
@Slf4j
public class ShardingKeyAutoFillConfiguration {

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }

    @Bean
    @ConditionalOnBean(value = {MybatisPlusInterceptor.class})
    public ShardingParserInterceptor registerShardingKeyAutoFill(ShardingKeyAutoFillProperty shardingKeyAutoFillProperty
            , MybatisPlusInterceptor mybatisPlusInterceptor, ShardingValueFinderFactory shardingValueFinderFactory
            , @Autowired(required = false) RedisConnectionFactory redisConnectionFactory
            , @Autowired(required = false) CacheManager cacheManager) {
        // 设置拦截详细日志打印开关
        CustomerLoggerFactory.init(shardingKeyAutoFillProperty.getLogEnabled());
        // 获取分片键策略（若设置自定义分片键策略则注册该实现，否则注册默认分片键策略）
        AbstractShardingStrategyHandler shardingStrategyHandler = ShardingStrategyHandlerFactory.getInstance();
        // 注册策略助手
        ShardingStrategyHandlerFactory.registerStrategyHelper(shardingKeyAutoFillProperty, shardingValueFinderFactory);
        // 初始化分片策略键工厂
        ShardingParserInterceptor shardingParserInterceptor = new ShardingParserInterceptor(shardingStrategyHandler);
        mybatisPlusInterceptor.addInnerInterceptor(shardingParserInterceptor);
        // 创建分片键值对内容加载处理器
        ShardingValueCacheProperty shardingValueCacheProperty = shardingKeyAutoFillProperty.getCache();
        Object[] args = {shardingValueCacheProperty, redisConnectionFactory, cacheManager};
        ShardingValueHandler shardingValueHandler = ShardingValueHandlerFactory.initInstance(args);
        // 设置阻断拦截器,防止全表更新与删除
        boolean noneMatchBlockAttackInnerInterceptor = mybatisPlusInterceptor.getInterceptors()
                .stream().noneMatch(innerInterceptor -> innerInterceptor instanceof BlockAttackInnerInterceptor);
        if (noneMatchBlockAttackInnerInterceptor)
            mybatisPlusInterceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        String cacheType = Strings.EMPTY;
        if (shardingValueHandler instanceof ShardingValueCachedHandler)
            cacheType = "type:" + ((ShardingValueCachedHandler) shardingValueHandler).getCacheClass().getName();
        log.info("Register sharding key autofill success！ [Log enabled: {}, Key-value cache enabled:{} {}, Strategy handler type:{}]"
                , shardingKeyAutoFillProperty.getLogEnabled(), shardingValueCacheProperty.getEnabled(), cacheType, shardingStrategyHandler.getClass().getName());
        return shardingParserInterceptor;
    }

}
