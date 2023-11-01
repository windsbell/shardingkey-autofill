package com.windbell.shardingkey.autofill.properties;

import lombok.Data;

import java.util.List;

@Data
public class BusinessKeyProperty {

    private List<String> necessary; // 必要业务字段，条件中必须有

    private List<String> anyOne; // 任意业务字段，条件中需要出现其中之一
}
