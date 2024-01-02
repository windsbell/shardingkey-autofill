-- 单表、设置了别名
update user_info t1
SET t1.user_name = '123',
    t1.mobile    = (select mobile from order_info where user_name = '123' limit 1)
WHERE
    t1.user_id = '9999'
--   AND t1.org_id = '12345'

