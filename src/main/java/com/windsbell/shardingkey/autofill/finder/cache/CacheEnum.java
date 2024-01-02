package com.windsbell.shardingkey.autofill.finder.cache;


import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 分片键值对内容缓存枚举
 *
 * @author windbell
 */
public enum CacheEnum {

    DEFAULT("default", "本地缓存(默认)"),
    REDIS("redis", "redis缓存"),
    SPRING("spring", "spring cache缓存"),
    ;

    CacheEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    @Getter
    final String type;

    @Getter
    final String desc;


    public static CacheEnum getCache(String type) {
        if (StringUtils.isBlank(type)) return CacheEnum.DEFAULT;
        Optional<CacheEnum> optional = Arrays.stream(CacheEnum.values()).filter(cacheEnum -> type.equals(cacheEnum.getType())).findFirst();
        Assert.isTrue(optional.isPresent(), String.format("暂不支持缓存类型：%s，请从列表%s中选择一个配置！"
                , type, Arrays.stream(CacheEnum.values()).map(CacheEnum::getType).collect(Collectors.toList())));
        return optional.get();
    }


}
