-- 单表 、order by、未设置别名
SELECT user_name AS fullName,
       order_id
FROM order_info
WHERE org_id = '12345'
  AND statu = 1
ORDER BY occur_time
        DESC LIMIT 1,4