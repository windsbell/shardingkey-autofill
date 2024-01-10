-- join、group by、设置了别名  、union all
SELECT t1.user_id,
       t1.user_name AS userId,
       t1.org_name,
       COUNT(1)        productCounts
FROM user_info t1,
     order_info t2
WHERE t1.org_id = t2.org_id
  AND t1.user_id = t2.user_id
  AND t1.account_id = '12345'
GROUP BY t2.product_id, t2.create_time

UNION ALL
SELECT t1.user_id,
       t1.user_name AS userId,
       t1.org_name,
       COUNT(1)        productCounts
FROM user_info t1,
     order_info t2
WHERE t1.org_id = t2.org_id
  AND t1.user_id = t2.user_id
  AND t1.org_id = '12345'
GROUP BY t2.product_id, t2.create_time
