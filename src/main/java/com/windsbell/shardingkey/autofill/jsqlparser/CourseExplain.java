package com.windsbell.shardingkey.autofill.jsqlparser;

import lombok.Data;
import net.sf.jsqlparser.statement.Statement;

import java.util.List;
import java.util.Map;

/**
 * SQL分片键填充过程说明
 *
 * @author windbell
 */
@Data
public class CourseExplain {

    private Long cost; // 消耗时间

    private String initialSQL; // 初始的SQL

    private Statement finalSQL; // 填充分片键的SQL

    private Boolean hasFilled;  // 是否有进行填充

    private List<String> targetTables; // 目标SQL中出现的表集合

    private Map<String, Map<String, String>> targetShardingKeyValueMap; // 目标SQL中，需要用到的分片键

    private Map<String, Map<String, String>> finalFilledShardingKeyMap; // 目标SQL中，最终填充了的分片键

    @Override
    public String toString() {
        return "auto fill sharding key course explain:"
                + "\ninitialSQL:\n      " + initialSQL
                + "\nfinalSQL:\n      " + finalSQL
                + "\ntargetTables:  " + targetTables
                + "\nhasFilled: " + hasFilled
                + "\ntargetShardingKeyValueMap: " + targetShardingKeyValueMap
                + "\nfinalFilledShardingKeyMap: " + finalFilledShardingKeyMap
                + "\ncost: " + cost + "ms";
    }


}
