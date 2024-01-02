-- join、in 、group by、having、设置了别名
SELECT t1.user_id,
       t1.user_name AS userId,
       t1.org_name,
       COUNT(1)        productCounts
FROM user_info
WHERE t1.org_id in ('12345', '5678')
GROUP BY t1.user_id, t1.area_id
HAVING t1.area_id > 0