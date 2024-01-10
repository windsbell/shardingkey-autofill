-- join、group by、设置了别名
SELECT t1.user_id,
       t1.user_name AS userId,
       COUNT(1)        orderCounts
FROM user_info t1,
     order_info t2
WHERE t1.org_id = t2.org_id
  AND t1.user_id = t2.user_id
  AND t1.mobile = '12345'
  AND t2.account_id = '678'
GROUP BY t1.id

