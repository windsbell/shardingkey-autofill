-- left join 、order by、设置了别名
SELECT t1.user_id,
       t1.user_name AS fullName,
       t1.org_name,
       t2.*
FROM user_info t1
         LEFT JOIN order_info t2 ON t1.org_id = t2.org_id
    AND t1.user_id = t2.user_id
WHERE t1.account_id = '12345'
  AND t2.mobile = '133'
ORDER BY t2.order_time DESC LIMIT 1,4

