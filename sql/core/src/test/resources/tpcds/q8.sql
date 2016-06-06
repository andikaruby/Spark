SELECT
  s_store_name,
  sum(ss_net_profit)
FROM store_sales, date_dim, store,
  (SELECT ca_zip
  FROM (
         (SELECT substr(ca_zip, 1, 5) ca_zip
         FROM customer_address
         WHERE substr(ca_zip, 1, 5) IN (
               '24128','76232','65084','87816','83926','77556','20548',
               '26231','43848','15126','91137','61265','98294','25782',
               '17920','18426','98235','40081','84093','28577','55565',
               '17183','54601','67897','22752','86284','18376','38607',
               '45200','21756','29741','96765','23932','89360','29839',
               '25989','28898','91068','72550','10390','18845','47770',
               '82636','41367','76638','86198','81312','37126','39192',
               '88424','72175','81426','53672','10445','42666','66864',
               '66708','41248','48583','82276','18842','78890','49448',
               '14089','38122','34425','79077','19849','43285','39861',
               '66162','77610','13695','99543','83444','83041','12305',
               '57665','68341','25003','57834','62878','49130','81096',
               '18840','27700','23470','50412','21195','16021','76107',
               '71954','68309','18119','98359','64544','10336','86379',
               '27068','39736','98569','28915','24206','56529','57647',
               '54917','42961','91110','63981','14922','36420','23006',
               '67467','32754','30903','20260','31671','51798','72325',
               '85816','68621','13955','36446','41766','68806','16725',
               '15146','22744','35850','88086','51649','18270','52867',
               '39972','96976','63792','11376','94898','13595','10516',
               '90225','58943','39371','94945','28587','96576','57855',
               '28488','26105','83933','25858','34322','44438','73171',
               '30122','34102','22685','71256','78451','54364','13354',
               '45375','40558','56458','28286','45266','47305','69399',
               '83921','26233','11101','15371','69913','35942','15882',
               '25631','24610','44165','99076','33786','70738','26653',
               '14328','72305','62496','22152','10144','64147','48425',
               '14663','21076','18799','30450','63089','81019','68893',
               '24996','51200','51211','45692','92712','70466','79994',
               '22437','25280','38935','71791','73134','56571','14060',
               '19505','72425','56575','74351','68786','51650','20004',
               '18383','76614','11634','18906','15765','41368','73241',
               '76698','78567','97189','28545','76231','75691','22246',
               '51061','90578','56691','68014','51103','94167','57047',
               '14867','73520','15734','63435','25733','35474','24676',
               '94627','53535','17879','15559','53268','59166','11928',
               '59402','33282','45721','43933','68101','33515','36634',
               '71286','19736','58058','55253','67473','41918','19515',
               '36495','19430','22351','77191','91393','49156','50298',
               '87501','18652','53179','18767','63193','23968','65164',
               '68880','21286','72823','58470','67301','13394','31016',
               '70372','67030','40604','24317','45748','39127','26065',
               '77721','31029','31880','60576','24671','45549','13376',
               '50016','33123','19769','22927','97789','46081','72151',
               '15723','46136','51949','68100','96888','64528','14171',
               '79777','28709','11489','25103','32213','78668','22245',
               '15798','27156','37930','62971','21337','51622','67853',
               '10567','38415','15455','58263','42029','60279','37125',
               '56240','88190','50308','26859','64457','89091','82136',
               '62377','36233','63837','58078','17043','30010','60099',
               '28810','98025','29178','87343','73273','30469','64034',
               '39516','86057','21309','90257','67875','40162','11356',
               '73650','61810','72013','30431','22461','19512','13375',
               '55307','30625','83849','68908','26689','96451','38193',
               '46820','88885','84935','69035','83144','47537','56616',
               '94983','48033','69952','25486','61547','27385','61860',
               '58048','56910','16807','17871','35258','31387','35458',
               '35576'))
         INTERSECT
         (SELECT ca_zip
         FROM
           (SELECT
             substr(ca_zip, 1, 5) ca_zip,
             count(*) cnt
           FROM customer_address, customer
           WHERE ca_address_sk = c_current_addr_sk AND
             c_preferred_cust_flag = 'Y'
           GROUP BY ca_zip
           HAVING count(*) > 10) A1)
       ) A2
  ) V1
WHERE ss_store_sk = s_store_sk
  AND ss_sold_date_sk = d_date_sk
  AND d_qoy = 2 AND d_year = 1998
  AND (substr(s_zip, 1, 2) = substr(V1.ca_zip, 1, 2))
GROUP BY s_store_name
ORDER BY s_store_name
LIMIT 100
