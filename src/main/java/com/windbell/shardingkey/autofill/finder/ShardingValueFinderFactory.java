package com.windbell.shardingkey.autofill.finder;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * 可拓展实现查询分片键值查找器SPI实例化工厂
 *
 * @author windbell
 */
@Slf4j
public class ShardingValueFinderFactory {

    public static ShardingValueFinder getInstance() {
        // 工厂返回从项目中取的SPI第一条实现者
        ServiceLoader<ShardingValueFinder> services = ServiceLoader.load(ShardingValueFinder.class);
        Iterator<ShardingValueFinder> iterator = services.iterator();
        if (iterator.hasNext()) {
            ShardingValueFinder shardingValueFinder = iterator.next();
            log.info("load shardingValueFinder: {}", shardingValueFinder.getClass().getName());
            return shardingValueFinder;
        }
        throw new IllegalStateException("未找到分片键值查找器的实现，请通过SPI加载实现ShardingValueFinder!");
    }

}
