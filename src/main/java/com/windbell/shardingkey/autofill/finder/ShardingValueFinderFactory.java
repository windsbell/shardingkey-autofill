package com.windbell.shardingkey.autofill.finder;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 * 可拓展实现查询分片键值查找器实例化工厂
 *
 * @author windbell
 */
@Slf4j
public class ShardingValueFinderFactory {

    public static ShardingValueFinder getInstance(String finderClassName) {
        try {
            Class<?> loadedClass = Class.forName(finderClassName);
            if (!ShardingValueFinder.class.isAssignableFrom(loadedClass)) {
                throw new ClassCastException(String.format("分片键查找器类名：%s 必须实现%s！", finderClassName, ShardingValueFinder.class.getName()));
            }
            return (ShardingValueFinder) loadedClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
