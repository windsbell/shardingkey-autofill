package com.windbell.shardingkey.autofill.config;


import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.windbell.shardingkey.autofill.finder.cache.ShardingValueCacheDecorator;
import com.windbell.shardingkey.autofill.finder.cache.ShardingValueCacheFactory;
import com.windbell.shardingkey.autofill.handler.ShardingStrategyHandler;
import com.windbell.shardingkey.autofill.interceptor.ShardingParserInterceptor;
import com.windbell.shardingkey.autofill.logger.CustomerLoggerFactory;
import com.windbell.shardingkey.autofill.properties.CacheProperty;
import com.windbell.shardingkey.autofill.properties.ShardingKeyAutoFillProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;


/**
 * 开启分片键自动填充核心配置
 *
 * @author windbell
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class ShardingKeyAutoFillConfiguration {

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }

    @Bean
    @ConditionalOnBean(value = {MybatisPlusInterceptor.class})
    public ShardingParserInterceptor registerShardingKeyAutoFill(ShardingKeyAutoFillProperty shardingKeyAutoFillProperty
            , MybatisPlusInterceptor mybatisPlusInterceptor, @Autowired(required = false) CacheManager cacheManager
            , @Autowired(required = false) RedisConnectionFactory redisConnectionFactory) {
        // 设置拦截详细日志打印开关
        CustomerLoggerFactory.init(shardingKeyAutoFillProperty.getLogEnabled());
        // 获取分片键策略（若设置自定义分片键策略则注册该实现，否则注册默认分片键策略）
        ShardingStrategyHandler shardingStrategyHandler = ShardingStrategyHandlerFactory.getInstance();
        // 注册策略助手
        ShardingStrategyHandlerFactory.registerStrategyHelper(shardingKeyAutoFillProperty);
        // 初始化分片策略键工厂
        ShardingParserInterceptor shardingParserInterceptor = new ShardingParserInterceptor(shardingStrategyHandler);
        mybatisPlusInterceptor.addInnerInterceptor(shardingParserInterceptor);
        // 创建分片键值对内容加载的缓存
        CacheProperty cacheProperty = shardingKeyAutoFillProperty.getCache();
        Object[] args = {cacheProperty, redisConnectionFactory, cacheManager};
        ShardingValueCacheDecorator shardingValueCache = ShardingValueCacheFactory.newInstance(args);
        // 设置阻断拦截器,防止全表更新与删除
        boolean containBlockAttackInnerInterceptor = mybatisPlusInterceptor.getInterceptors()
                .stream().anyMatch(innerInterceptor -> innerInterceptor instanceof BlockAttackInnerInterceptor);
        if (!containBlockAttackInnerInterceptor)
            mybatisPlusInterceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        log.info("Register sharding key autofill success！ [Log enabled: {}, Key-value cache type: {}, Strategy handler type: {}]"
                , shardingKeyAutoFillProperty.getLogEnabled(), shardingValueCache.getCacheClass().getName()
                , shardingStrategyHandler.getClass().getName());
        return shardingParserInterceptor;
    }

}
