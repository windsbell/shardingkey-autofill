package com.windsbell.shardingkey.autofill.jsqlparser.test;

import com.windsbell.shardingkey.autofill.jsqlparser.CourseExplain;
import com.windsbell.shardingkey.autofill.jsqlparser.ShardingKeyFillerAgent;
import com.windsbell.shardingkey.autofill.jsqlparser.TableAliasSetter;
import com.windsbell.shardingkey.autofill.utils.ResourceUtil;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ShardingKeyTest {

    public static void main(String[] args) {

        Map<String, Map<String, String>> fillShardingKeyMap = new HashMap<>();
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put("org_id", "o222");
        fillShardingKeyMap.put("user_info", innerMap);
        Map<String, String> inner1Map = new HashMap<>();
        inner1Map.put("user_id", "u111");
        inner1Map.put("org_id", "o111");
        fillShardingKeyMap.put("order_info", inner1Map);
        Map<String, String> sqlFileContents = ResourceUtil.getResourceFilesContent("testsql/select");
        filterSqlFile("6.sql", sqlFileContents);
        filterSqlFile(null, sqlFileContents);

        sqlFileContents.forEach((fileName, sql) -> {
            Statement statement;
            try {
                statement = CCJSqlParserUtil.parse(sql);
            } catch (JSQLParserException e) {
                throw new RuntimeException(e);
            }

            System.out.println("===============start================");
            System.out.printf("1.start parse File: %s \n%s\n", fileName, sql);
            TableAliasSetter tableAliasSetter = new TableAliasSetter(statement);
            tableAliasSetter.setTableAlias();
            System.out.printf("2.set table alias sql:\n%s", statement);
            CourseExplain courseExplain = new ShardingKeyFillerAgent(fillShardingKeyMap, statement).doFill();
            List<String> targetTables = courseExplain.getTargetTables();
            System.out.printf("3.get tables:%s\n", targetTables);
            System.out.printf("4.success parse File: %s  final sql:\n%s \n", fileName, courseExplain.getFinalSQL());
            System.out.println(courseExplain);
            System.out.println("===============end================\n");
        });
    }

    private static void filterSqlFile(String fileName, Map<String, String> sqlFileContents) {
        if (fileName != null) {
            Iterator<Map.Entry<String, String>> iterator = sqlFileContents.entrySet().iterator();
            while (iterator.hasNext()) {
                if (!iterator.next().getKey().equals(fileName)) {
                    iterator.remove();
                }
            }
        }
    }

}
