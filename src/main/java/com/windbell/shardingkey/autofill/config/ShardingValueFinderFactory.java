package com.windbell.shardingkey.autofill.config;

import com.windbell.shardingkey.autofill.finder.ShardingValueFinder;
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
            throw new ClassCastException(String.format("分片键查找器实现类：%s 必须实现%s！", finderClassName, ShardingValueFinder.class.getName()));
        }
        try {
            return (ShardingValueFinder) applicationContext.getBean(loadedClass);
        } catch (NoSuchBeanDefinitionException e) {
            try {
                return (ShardingValueFinder) loadedClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

}
