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

    private final Long start;

    private final CourseExplain courseExplain;

    private final ShardingKeyFiller shardingKeyFiller;

    public ShardingKeyFillerAgent(Map<String, Map<String, String>> shardingKeyValueMap, Statement statement) {
        this.start = System.currentTimeMillis();
        this.courseExplain = new CourseExplain();
        courseExplain.setHasFilled(false);
        courseExplain.setFinalFilledShardingKeyMap(new LinkedHashMap<>());
        courseExplain.setInitialSQL(statement.toString());
        this.shardingKeyFiller = new ShardingKeyFiller(shardingKeyValueMap, statement);
    }

    public CourseExplain doFill() {
        shardingKeyFiller.doFill();
        combineExplain();
        return courseExplain;
    }

    private void combineExplain() {
        courseExplain.setFinalSQL(shardingKeyFiller.getStatement());
        courseExplain.setTargetTables(new ArrayList<>(shardingKeyFiller.getTablesAliasMap().keySet()));
        Map<String, Map<String, String>> shardingKeyValueMap = shardingKeyFiller.getShardingKeyValueMap();
        courseExplain.setTargetShardingKeyValueMap(shardingKeyValueMap);
        Map<String, Map<String, Boolean>> firstRoundShardingKeyHitFlagMap = shardingKeyFiller.getFirstRoundShardingKeyHitFlagMap();
        Map<String, Map<String, Boolean>> shardingKeyHitFlagMap = shardingKeyFiller.getShardingKeyHitFlagMap();
        firstRoundShardingKeyHitFlagMap.forEach((k, v) -> {
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
