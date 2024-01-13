package com.windsbell.shardingkey.autofill.jsqlparser;

import net.sf.jsqlparser.statement.Statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQL分片键填充代理
 *
 * @author windbell
 */
public class ShardingKeyFillerAgent {

    private final CourseExplain courseExplain;  // 分片键填充过程说明

    private final ShardingKeyFiller shardingKeyFiller; // 分片键填充

    public ShardingKeyFillerAgent(Statement statement, Map<String, Map<String, String>> shardingKeyValueMap, Map<String, Map<String, Boolean>> firstShardingKeyHitFlagMap) {
        this.courseExplain = new CourseExplain();
        courseExplain.setHasFilled(false);
        courseExplain.setFinalFilledShardingKeyMap(new LinkedHashMap<>());
        courseExplain.setInitialSQL(statement.toString());
        this.shardingKeyFiller = new ShardingKeyFiller(statement, shardingKeyValueMap, firstShardingKeyHitFlagMap);
    }

    public CourseExplain doFill(long start) {
        shardingKeyFiller.doFill();
        combineExplain(start);
        return courseExplain;
    }

    private void combineExplain(long start) {
        courseExplain.setFinalSQL(shardingKeyFiller.getStatement());
        courseExplain.setTargetTables(new ArrayList<>(shardingKeyFiller.getTablesAliasMap().keySet()));
        Map<String, Map<String, String>> shardingKeyValueMap = shardingKeyFiller.getShardingKeyValueMap();
        courseExplain.setTargetShardingKeyValueMap(shardingKeyValueMap);
        Map<String, Map<String, Boolean>> firstShardingKeyHitFlagMap = shardingKeyFiller.getFirstShardingKeyHitFlagMap();
        Map<String, Map<String, Boolean>> shardingKeyHitFlagMap = shardingKeyFiller.getShardingKeyHitFlagMap();
        firstShardingKeyHitFlagMap.forEach((k, v) -> {
            Map<String, Boolean> shardingKeyHitFlagInnerMap = shardingKeyHitFlagMap.get(k);
            Map<String, String> shardingKeyValueInnerMap = shardingKeyValueMap.get(k);
            v.forEach((m, n) -> {
                Boolean isHit = shardingKeyHitFlagInnerMap.get(m);
                if (!n && isHit) { // 分片键字段未出现在SQL，并最终填充成功的
                    courseExplain.setHasFilled(true);
                    Map<String, Map<String, String>> finalFilledShardingKeyMap = courseExplain.getFinalFilledShardingKeyMap();
                    Map<String, String> finalFilledShardingKeyInnerMap = finalFilledShardingKeyMap.computeIfAbsent(k, o -> new LinkedHashMap<>());
                    finalFilledShardingKeyInnerMap.put(m, shardingKeyValueInnerMap.get(m));
                }
            });
        });
        courseExplain.setCost(System.currentTimeMillis() - start);
    }

}
