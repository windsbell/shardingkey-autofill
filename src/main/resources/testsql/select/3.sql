-- join、order by、设置了别名
SELECT t1.user_id,
       t1.user_name AS userId,
       t2.org_id,
       t2.org_name
FROM user_info t1,
     order_info t2
WHERE t1.org_id = t2.org_id
  AND t1.user_id = t2.user_id
  AND account_id = '12345'
  AND mobile = '133'
ORDER BY t2.order_time DESC
LIMIT 1,10