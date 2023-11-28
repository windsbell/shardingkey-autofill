package com.windsbell.shardingkey.autofill.properties;

import lombok.Data;

import java.util.List;

@Data
public class BusinessKeyProperty {

    private List<String> necessary; // （选填）必要业务键列表[条件中必须出现的业务键,通过其中出现的业务键可查出分库分表等键值对]

    private List<String> anyOne; // （选填）任意业务键列表[条件中出现以下任意一个业务键即可满足查出分库分表等键值对]

}
