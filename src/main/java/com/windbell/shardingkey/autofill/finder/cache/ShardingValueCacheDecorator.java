package com.windbell.shardingkey.autofill.finder.cache;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.windbell.shardingkey.autofill.finder.ShardingValueFinder;
import com.windbell.shardingkey.autofill.logger.CustomerLogger;
import com.windbell.shardingkey.autofill.logger.CustomerLoggerFactory;
import com.windbell.shardingkey.autofill.strategy.BusinessKeyStrategy;
import com.windbell.shardingkey.autofill.strategy.ShardingValueStrategy;
import lombok.Getter;
import net.sf.jsqlparser.expression.NullValue;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 分片键值对缓存装饰器
 *
 * @author windbell
 */
public class ShardingValueCacheDecorator implements ShardingValueCache {

    private static final CustomerLogger log = CustomerLoggerFactory.getLogger(ShardingValueCacheDecorator.class);

    /**
     * 分片键值对缓存
     */
    @Getter
    private final ShardingValueCache cache;

    /**
     * 分片键值对缓存器实现类class
     */
    @Getter
    private final Class<?> cacheClass;

    /**
     * 修改锁，采用ConcurrentHashMap的分段锁，在put、remove时，通过key设置一个监视器对象来进行细化粒度锁
     */
    private static final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    /**
     * 有正在put、remove内容时，get方需要等待的最大时间（ms）
     */
    private static final Long waitingTimeWhilePutting = 100L;

    private static final String DATA_BASE_SHARD_KEY = "databaseShardKey:";

    private static final String TABLE_SHARD_KEY = "tableShardKey:";

    private static final String KEY_SEPARATOR = ";";

    private static final String VALUE_SEPARATOR = ":";

    public ShardingValueCacheDecorator(ShardingValueCache shardingValueCache, Class<?> cacheClass) {
        this.cache = shardingValueCache;
        this.cacheClass = cacheClass;
    }

    @Override
    public ShardingValueStrategy get(String key) {
        return cache.get(key);
    }

    @Override
    public void put(String key, ShardingValueStrategy ShardingValueStrategy) {
        cache.put(key, ShardingValueStrategy);
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * 公共方法：通过业务键策略，使用分片键查找器查到对应分片键值内容，再置入cache，后面同样语句类型直接从cache中拿取
     */
    public ShardingValueStrategy get(BusinessKeyStrategy businessKeyStrategy, ShardingValueFinder shardingValueFinder) {
        String shardingValueCacheKey = getShardingValueCacheKey(businessKeyStrategy);
        waitWhilePutting(shardingValueCacheKey);
        // 查询缓存
        ShardingValueStrategy shardingValueStrategy = get(shardingValueCacheKey);
        if (shardingValueStrategy != null) {
            log.info("findShardingKeyValueStrategy hit cache: key-->'{}' value-->'{}'"
                    , shardingValueCacheKey, shardingValueStrategy);
            return shardingValueStrategy;
        }
        shardingValueStrategy = put(businessKeyStrategy, shardingValueCacheKey, shardingValueFinder);
        return shardingValueStrategy;
    }

    /**
     * 私有方法：get时没有则调取查找器，进行put
     */
    private ShardingValueStrategy put(BusinessKeyStrategy businessKeyStrategy, String shardingValueCacheKey, ShardingValueFinder shardingValueFinder) {
        ShardingValueStrategy shardingValueStrategy;
        try {
            synchronized (lockMap.computeIfAbsent(shardingValueCacheKey, k -> new Object())) {
                // 未命中则使用分片键值对查找器执行查找（查找器通过SPI用户程序自行定制实现）
                shardingValueStrategy = shardingValueFinder.apply(businessKeyStrategy);
                // 查找器匹配到任意一条分片键时方可置入cache中
                if (shardingValueStrategy != null &&
                        (StringUtils.isNotBlank(shardingValueStrategy.getTableShardValue()) ||
                                StringUtils.isNotBlank(shardingValueStrategy.getDatabaseShardValue()))) {
                    // 将分片键值内容置入缓存
                    put(shardingValueCacheKey, shardingValueStrategy);
                    log.info("findShardingKeyValueStrategy put cache: key-->'{}' value-->'{}'"
                            , shardingValueCacheKey, shardingValueStrategy);
                }
                // 唤醒阻塞等待的get方
                lockMap.get(shardingValueCacheKey).notifyAll();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lockMap.remove(shardingValueCacheKey);
        }
        return shardingValueStrategy;
    }

    /**
     * 公共方法：通过业务键策略将其关联的分片键值内容，从cache中移除
     */
    public void remove(BusinessKeyStrategy businessKeyStrategy) {
        String shardingValueCacheKey = getShardingValueCacheKey(businessKeyStrategy);
        ShardingValueStrategy shardingValueStrategy = this.get(shardingValueCacheKey);
        // 查询缓存
        if (shardingValueStrategy != null) {
            try {
                synchronized (lockMap.computeIfAbsent(shardingValueCacheKey, k -> new Object())) {
                    // 将分片键值内容置入缓存
                    remove(shardingValueCacheKey);
                    log.info("findShardingKeyValueStrategy remove cache: key-->'{}' value-->'{}'"
                            , shardingValueCacheKey, shardingValueStrategy);
                    // 唤醒阻塞等待的get方
                    lockMap.get(shardingValueCacheKey).notifyAll();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lockMap.remove(shardingValueCacheKey);
            }
        } else {
            log.warn("can't remove shardingValueCacheKey：{} because of it doesn't exist！", shardingValueCacheKey);
        }
    }

    private static void waitWhilePutting(String shardingValueCacheKey) {
        // 当有线程正在put时，其他获取内容的线程阻塞等待
        if (lockMap.containsKey(shardingValueCacheKey)) {
            long start = System.currentTimeMillis();
            try {
                // 阻塞等待put方唤醒
                Object lock = lockMap.get(shardingValueCacheKey);
                if (lock != null) {
                    lock.wait(waitingTimeWhilePutting);
                    log.info("waiting {}ms get key-->'{}'", System.currentTimeMillis() - start, shardingValueCacheKey);
                }
            } catch (Exception e) {
                log.warn("waiting warning shardingValueCacheKey: {}", shardingValueCacheKey);
            }
        }
    }

    // 将查询目标分片键值对内容的缓存key
    private static String getShardingValueCacheKey(BusinessKeyStrategy businessKeyStrategy) {
        // like this ----> databaseShardKey:org_no;tableShardKey:paper_id;exam_id:********
        String keyPrefix = DATA_BASE_SHARD_KEY + businessKeyStrategy.getShardingKeyStrategy().getDatabaseShardKey() + KEY_SEPARATOR
                + TABLE_SHARD_KEY + businessKeyStrategy.getShardingKeyStrategy().getTableShardKey();
        String necessary = Strings.EMPTY;
        if (!CollectionUtils.isEmpty(businessKeyStrategy.getNecessaryBusinessKeys())) {
            necessary = businessKeyStrategy.getNecessaryBusinessKeys().stream()
                    .map(necessaryBusinessStrategy -> necessaryBusinessStrategy.getKey() + VALUE_SEPARATOR
                            + necessaryBusinessStrategy.getValue())
                    .collect(Collectors.joining(","));
        }
        String anyOne = Strings.EMPTY;
        if (!CollectionUtils.isEmpty(businessKeyStrategy.getAnyOneBusinessKeys())) {
            anyOne = businessKeyStrategy.getAnyOneBusinessKeys().stream()
                    .map(anyOneBusinessStrategy -> anyOneBusinessStrategy.getKey() + VALUE_SEPARATOR
                            + anyOneBusinessStrategy.getValue())
                    .collect(Collectors.joining(","));
        }
        if (StringUtils.isNotBlank(necessary) && StringUtils.isNotBlank(anyOne)) {
            return keyPrefix + KEY_SEPARATOR + necessary + KEY_SEPARATOR + anyOne;
        }
        if (StringUtils.isNotBlank(necessary)) {
            return keyPrefix + KEY_SEPARATOR + necessary;
        }
        if (StringUtils.isNotBlank(anyOne)) {
            return keyPrefix + KEY_SEPARATOR + anyOne;
        }
        return new NullValue().toString();
    }

}
