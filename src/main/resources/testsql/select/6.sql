-- join、group by、having、设置了别名
-- SELECT user_id,
--        user_name   AS userId,
--        org_name,
--        COUNT(1) productCounts
-- FROM user_info
-- WHERE  org_id = '12345'
-- GROUP BY user_id, area_id
-- HAVING area_id IN ('123','456')

-- SELECT user_id,
--        user_name AS userId,
--        org_name,
--        COUNT(1)     productCounts
-- FROM user_info
-- WHERE org_id = '12345'
-- GROUP BY user_id, area_id
-- HAVING area_id > 0

SELECT t1.user_id,
       t1.user_name AS userId,
       t1.org_name,
       COUNT(1)        productCounts
FROM user_info
WHERE t1.org_id = '12345'
GROUP BY t1.user_id, t1.area_id
HAVING t1.area_id > 0