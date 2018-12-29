/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.msgframe.sql.parser

import wvlet.airframe.AirframeSpec
import wvlet.msgframe.sql.SQLBenchmark

/**
  *
  */
class SQLParserTest extends AirframeSpec {

  def parse(sql: String): Unit = {
    trace(sql)
    val m = SQLParser.parse(sql)
  }

  "SQLParser" should {
    "parse SQL" in {
      parse("select * from a") // Query(Seq(AllColumns(None)), false, Some(Table(QName("a"))))
      parse("select * from a where time > 10")
      parse("select * from a where time < 10")
      parse("select * from a where time <= 10")
      parse("select * from a where id = 'xxxx'")
      parse("select * from a where time >= 10 and time < 20")
      parse("select * from a where id is null")
      parse("select * from a where id is not null")
      parse("select * from a where flag = true")
      parse("select * from a where flag = false")
      parse("select x, y from a")
      parse("select x from a where val > 0.5")
      parse("select `x` from a")
      parse("""select "x" from a""")
      parse("select category, count(*) from a group by 1")
      parse("select category, count(*) from a group by category")
      parse("select * from a order by 1")
      parse("select * from a order by 1 desc")
      parse("select * from a order by 1 asc")
      parse("select * from a order by 1 nulls first")
      parse("select * from a order by 1 nulls last")
      parse("select * from a limit 100")
    }

    "parse joins" taggedAs ("join") in {
      parse("select * from a, b")
      parse("select * from a join b on a.id = b.id")
      parse("select * from a join b using (id)")
      parse("select * from a left join b on a.id = b.id")
    }

    "parse expressions" taggedAs working in {
      parse("select 1")
      parse("select 1 + 2")
      parse("select true")
      parse("select true or false")

      parse("select NULL")
      parse("select ARRAY[1, 2]")
      parse("select interval '1' year")
      parse("select interval '1' month")
      parse("select interval '1' day")
      parse("select interval - '1' month")
      parse("select interval '1' hour")
      parse("select interval '1' minute")
      parse("select interval '1' second")
      parse("select data '2012-08-08' + interval '2' day")
      parse("select case a when 1 then 'one' end")
      parse("select case a when 1 then 'one' when 2 then 'two' else 'many' end")
      parse("select case when a=1 then 'one' when a=2 then 'two' else 'many' end")

      parse("select cast(1 as double)")
      parse("select try_cast(1 as double)")
      parse("select count(*)")
      parse("select count(distinct a)")

      parse("select time '01:00'")
      parse("select timestamp '2012-08-08 01:00'")
      parse("select date '2018-08-08'")
      parse("select decimal '1000000000001'")
      parse("select char 'a'")
      parse("select binary '00'")

      parse("select ?")

      parse("select 'a'")
      parse("select `a`")
      parse("select \"a\"")

      parse("select rank() over (partition by a order by b desc range between unbounded preceding  and current row)")
      parse("select rank() over (partition by a order by b desc range between current row and unbounded following)")

      parse("select rank() over (partition by a order by b desc rows between unbounded preceding  and current row)")
      parse("select rank() over (partition by a order by b desc rows between current row and unbounded following)")
      parse("select rank() over (partition by a order by b desc rows between current row and 1 following)")
      parse("select rank() over (partition by a order by b desc rows between current row and 1 preceding)")
      parse("select rank() over (partition by a order by b desc)")
      parse("""select * from (select * from t) as t(a, "b", `c`)""")
      parse("""with t(a, "b", `c`) as (select 1, 2, 3) select * from t""")

      parse("select * from (select 1 limit 1) as a")
      parse("select * from (a right join b on a.id = b.id) as c")

      parse("""(
          |select c_last_name,c_first_name,sum(cs_quantity*cs_list_price) sales
          |        from catalog_sales, customer, date_dim
          |        where d_year = 2000
          |         and d_moy = 2
          |         and cs_sold_date_sk = d_date_sk
          |         and cs_item_sk in (select item_sk from frequent_ss_items)
          |         and cs_bill_customer_sk in (select c_customer_sk from best_ss_customer)
          |         and cs_bill_customer_sk = c_customer_sk
          |       group by c_last_name,c_first_name)
        """.stripMargin)

      parse("""
          |(select c_last_name,c_first_name,sum(cs_quantity*cs_list_price) sales
          |        from catalog_sales, customer, date_dim
          |        where d_year = 2000
          |         and d_moy = 2
          |         and cs_sold_date_sk = d_date_sk
          |         and cs_item_sk in (select item_sk from frequent_ss_items)
          |         and cs_bill_customer_sk in (select c_customer_sk from best_ss_customer)
          |         and cs_bill_customer_sk = c_customer_sk
          |       group by c_last_name,c_first_name)
          |      union all
          |      (select c_last_name,c_first_name,sum(ws_quantity*ws_list_price) sales
          |       from web_sales, customer, date_dim
          |       where d_year = 2000
          |         and d_moy = 2
          |         and ws_sold_date_sk = d_date_sk
          |         and ws_item_sk in (select item_sk from frequent_ss_items)
          |         and ws_bill_customer_sk in (select c_customer_sk from best_ss_customer)
          |         and ws_bill_customer_sk = c_customer_sk
          |       group by c_last_name,c_first_name)
        """.stripMargin)
    }

    "parse tpc-h queries" taggedAs ("tpc-h") in {
      SQLBenchmark.tpcH.foreach { sql =>
        parse(sql)
      }
    }

    "parse tpc-ds queries" taggedAs ("tpc-ds") in {
      SQLBenchmark.tpcDS.foreach { sql =>
        parse(sql)
      }
    }
  }
}
