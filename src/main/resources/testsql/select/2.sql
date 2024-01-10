-- 单表 、order by、未设置别名
SELECT user_name AS fullName,
       order_id
FROM order_info
WHERE account_id = '12345'
  AND status = 1
ORDER BY create_time DESC
LIMIT 1,10