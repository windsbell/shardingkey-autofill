## Shardingkey-Autofill

### 介绍

Shardingkey-Autofill 是一个针对**分库分表**的项目进行**分片键（分库、分表键字段）自动填充**的增强框架

### 前言

    1. 面对正要落地实施分库分表的项目，有大面积的原始业务SQL语句，需要手动改造：确保查询条件中有分片键（分库、分表字段），目的是让对应框架能够正确路由到对应的库和表）
    2. 对于每次新的业务开发，SQL书写时都得冗余手动补充分片键，如果遗漏任何一处没有补充，可能导致等运行后才发现：路由到错误的库或表、甚至跨库查询并合并，而导致查询到错误的结果
    3. 当原始业务SQL本身条件中含有一些业务字段，而通过其关联查询可以查到对应的分片键时，为此在需要改造目前业务代码，提取业务字段进行查询匹配到的分片键字段，再补充到最终SQL条件中，这种“动作”显得重复而又多余

### 特性

    1. 基于springboot和mybatis-plus的自动填充分片键框架：将上述直面场景提炼出来，通过一些简单的配置，让具备现有查询条件能够关联查询到分库、分表等分片键字段场景的SQL，可以自动拦截并将分片键填充到至里面，无需手动操作
    2. 实现功能：针对目前流行使用的mybatis-plus框架，执行Service单表SQL逻辑层面，可以自动进行拦截填充分片键；而对于mapper.xml层面，目前实现了是否有出现分片键的检查，自动填充分片键有待实现（持续跟进连表条件适配中...）

### 直面场景

假设项目有业务场景如下（以下为演示样例，主要描述改造原始业务的过程）：

-
背景：每个用户只属于单个机构，每个用户在每次购买服务时会使用其名下账户之一进行下单；这里业务实现上，有用户表（user_info）、用户账户表（user_account_info）、机构表（org_info）、订单表（order_info）

-
升级原因：现在用户下单频率增长，订单表数据体量较大，考虑对其进行分库分表（引入相关分库分表框架），规则为：通过用户id进行分表（方便某个用户的所有订单信息都在同一张表中），机构id进行分库（方便某个机构的所有用户订单信息都在同一数据库中）

- 原始业务：查询某个账户下的某个订单信息

    ```sql
     SELECT * FROM order_info WHERE account_id = '123' AND order_id = '001'
    ```

- 业务查询改造步骤：

    1. 对原始业务调用查询前，提取用户账户id

    2. 找到分表键，前往用户账户表查询用户id

    ```sql
    SELECT user_id FROM user_account_info WHERE account_id = '123'
    ```

    3. 找到分库键，前往用户表查询机构id ：

    ```sql
    SELECT org_id FROM user_info WHERE user_id = '456'
    ```

    4. 拿到上述分片键，填充原始业务查询：

    ```sql
    SELECT * FROM order_info WHERE account_id = '123' AND order_id =  '001' AND user_id = '456' AND org_id = '789'
    ```

-
总结：假设订单表有很多和上面类似的查询，条件各不相同，为此需要对每条SQL进行手动改造，不过基本步骤与上面类似：即在总体上保证最后条件都要有分库键字段org_id和分表键字段user_id；所以我们发现此类基本动作大致相同，为此开发了自动填充分片键框架，帮助提炼上述改造步骤

### 快速开始

1. 引入pom.xml依赖（当前最新版号为：**1.1.3**）

   ````xml
   <dependency>
    <groupId>com.windbell</groupId>
    <artifactId>shardingkey-autofill</artifactId>
    <version>最新版号</version>
   </dependency>

2. springboot启动类，添加开启使用分片键自动填充注解（**@EnableShardingKeyAutoFill**）
   ```java
   @EnableShardingKeyAutoFill
   @SpringBootApplication
   public class ExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
   }
   ```

3. 配置application.yml, 设置自动填充分片键策略（以下配置为演示样例，主要阐述数据表对应分片键和对应业务字段的关联映射）

   ```yaml
   spring:
   ## 自动填充分片键策略插件配置
     shardingkey-autofill:
       # 分片键值对内容缓存[首次执行查找器得到分片键值对并缓存，之后则在有效期内从缓存提取进行填充]（不填写则默认本地缓存、过期1小时）
       cache:
         # 类型[default（本地缓存）、redis（redis缓存）、spring（spring cache缓存）]
         type: default
         # 过期时间 [单位：秒 ，不填写则默认一小时]
         expire: 3600
       # 启用拦截日志开关[true:开启 false：不开启，不填写则默认开启]
       logEnabled: false
       # 策略集 [分片键都相同的一组数据表作为一个策略集进行配置]
       strategies:
       - # 策略一
         # 适配策略的表集合[多个表则对应都所使用的分片键都相同]
         suitableTables: [order_info]
         # 分表键
         tableShardKey: user_id   
         # 分库键
         databaseShardKey: org_id
         # 查找器类名 [继承ShardingValueFinder自定义实现目标业务键查询到分片键内容逻辑]
         finderClassName: com.exmpale.finder.CustomerShardingValueFinder
         #（选填）必要业务键列表[条件中必须出现的业务键,通过其中出现的所有业务键可查出分库分表等键值对]
         necessaryBusinessKeys:
         - account_id
         #（选填）任意业务键列表[条件中出现以下任意一个业务键即可满足查出分库分表等键值对]
         anyOneBusinessKeys:
         - mobile   
       - # 策略二
         # 多个策略依次如上面策略一的配置一样，自定义设置选择必要业务键或者任意业务键
         ****
       - # 策略三
         # 多个策略依次如上面策略一的配置一样，自定义设置选择必要业务键或者任意业务键
         ****
   ```

4. 业务书写实现上面每个策略集中的分片键查找器：实现com.windbell.shardingkey.autofill.finder.ShardingValueFinder，添加通过业务键查询到分片键内容逻辑，用来提供给框架调用）

   ```java
   public class CustomerShardingValueFinder implements ShardingValueFinder {
   
       @Override
       public ShardingValueStrategy apply(BusinessKeyStrategy businessKeyStrategy) {
           ShardingValueStrategy shardingValueStrategy = new ShardingValueStrategy();
           String userId = null; // 分表键
           String orgId = null; // 分库键
           // 通过必要业务字段查出分片键
           List<BusinessStrategy> necessaryBusinessKeys = businessKeyStrategy.getNecessaryBusinessKeys();
           for (BusinessStrategy businessStrategy : necessaryBusinessKeys) {
               String key = businessStrategy.getKey(); //  "account_id"
               String accountId = businessStrategy.getValue(); // "123"
               if ("account_id".equals(key)) {
                    userId = findUserIdByAccountId(accountId);
                    orgId = findOrgIdByUserId(userId);
                    break;
               }
           }
           // 若设有非必要业务字段，也支持通过其查出分片键
           if (StringUtils.isBlank(userId) && StringUtils.isBlank(userId)) {
               List<BusinessStrategy> anyOneBusinessKeys = businessKeyStrategy.getAnyOneBusinessKeys();
               for (BusinessStrategy anyOneBusinessKey : anyOneBusinessKeys) {
                   String key = anyOneBusinessKey.getKey(); //  "mobile"
                   String mobile = anyOneBusinessKey.getValue(); // "130"
                   if ("mobile".equals(key)) {
                       userId = findUserIdByMobile(mobile);
                       orgId = findOrgIdByUserId(userId);
                       break;
                   }
               }
           }
           shardingValueStrategy.setTableShardValue(userId);
           shardingValueStrategy.setDatabaseShardValue(orgId);
           return shardingValueStrategy;
       }
   
       private String findUserIdByAccountId(String accountId) {
           return null; // 补充通过用户账户id，查找用户id逻辑
       }
   
       private String findUserIdByMobile(String mobile) {
           return null; // 补充通过电话号码，查找用户id逻辑
       }
   
       private String findOrgIdByUserId(String userId) {
           return null; // 补充通过用户id，查找机构id逻辑
       }
       
   }
   ```

5. 备注：

    - 拦截日志：如果打开spring.shardingkeyaAutofill.logEnabled = true，在执行原始业务时，可以观察到具体哪些SQL片段的拦截以及哪些分片键字段被自动填充的过程
    - 分片键值对内容缓存：设置spring.shardingkeyaAutofill.cache，目前支持本地缓存（不设置则为默认缓存方式）、redis（自动读取spring
      redis starter配置）、spring cache ，业务查询在同样条件下，首次执行查找器找到分片键值内容会进行缓存，之后则在缓存有效期内直接自动从缓存提取并设置到条件当中
    -
    分片键值对内容缓存重置：如果在缓存有效期内，分片键值对关联关系发生变化（业务变更了），这时需要在关系变更后，及时清理键值对内容缓存，避免框架执行时拿取旧的关系，而影响查询结果；可以使用ShardingValueCleaner实现类辅助缓存清理，之后业务查询时会重新执行查找器重新进行新的键值对内容缓存构建
    -
    分片键自动填充核心处理类：目前对于mapper.xml层面，实现了是否有出现分片键的检查，自动填充分片键还未实现，主要在于连表相关join时条件解析和适配相对复杂，不过笔者有预留支持SPI方式的拓展，使用者可以通过继承AbstractShardingStrategyHandler来diy

### 说明

这是笔者在日常工作中，对落地分库分表框架(sharding-sphere)
之后，发现上述直面场景是不得不遇到的，很多SQL都是需要改造做这种冗余动作，为此写了自动填充分片键框架工具，由工具自动提炼并设置分片键，让开发专注于业务SQL。欢迎留言和star使用！

