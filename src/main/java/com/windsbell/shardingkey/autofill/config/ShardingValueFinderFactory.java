package com.windsbell.shardingkey.autofill.config;

import com.windsbell.shardingkey.autofill.finder.ShardingValueFinder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;

/**
 * 可拓展实现查询分片键值查找器实例化工厂
 *
 * @author windbell
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@AllArgsConstructor
public class ShardingValueFinderFactory {

    private final ApplicationContext applicationContext;

    ShardingValueFinder getInstance(String finderClassName) {
        Class<?> loadedClass;
        try {
            loadedClass = Class.forName(finderClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (!ShardingValueFinder.class.isAssignableFrom(loadedClass)) {
            throw new ClassCastException(String.format("sharding key finder:%s needs to implement:%s！", finderClassName, ShardingValueFinder.class.getName()));
        }
        try {
            return (ShardingValueFinder) applicationContext.getBean(loadedClass); // 优先从spring容器加载
        } catch (NoSuchBeanDefinitionException e) {
            try {
                return (ShardingValueFinder) loadedClass.getDeclaredConstructor().newInstance(); // 其次再进行反射创建
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

}
