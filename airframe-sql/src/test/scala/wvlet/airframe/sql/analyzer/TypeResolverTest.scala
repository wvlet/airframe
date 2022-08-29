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
package wvlet.airframe.sql.analyzer

import wvlet.airframe.sql.{SQLError, SQLErrorCode}
import wvlet.airframe.sql.SQLErrorCode.SyntaxError
import wvlet.airframe.sql.analyzer.SQLAnalyzer.PlanRewriter
import wvlet.airframe.sql.catalog.Catalog._
import wvlet.airframe.sql.catalog.{Catalog, DataType, InMemoryCatalog}
import wvlet.airframe.sql.model.Expression.{And, Eq, GroupingKey, LongLiteral, SingleColumn}
import wvlet.airframe.sql.model.LogicalPlan.{Aggregate, Filter, Project}
import wvlet.airframe.sql.model.{Expression, LogicalPlan, LogicalPlanPrinter, ResolvedAttribute}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.airspec.AirSpec

class TypeResolverTest extends AirSpec {

  private val tableA = Catalog.newTable(
    "default",
    "A",
    Catalog.newSchema
      .addColumn("id", DataType.LongType, properties = Map("tag" -> Seq("personal_identifier")))
      .addColumn("name", DataType.StringType, properties = Map("tag" -> Seq("private")))
  )
  private val tableB = Catalog.newTable(
    "default",
    "B",
    Catalog.newSchema
      .addColumn("id", DataType.LongType, properties = Map("tag" -> Seq("personal_identifier")))
      .addColumn("name", DataType.StringType, properties = Map("tag" -> Seq("private")))
  )

  private def demoCatalog: Catalog = {
    val catalog = new InMemoryCatalog(
      "global",
      None,
      Seq.empty
    )

    catalog.createDatabase(Catalog.Database("default"), CreateMode.CREATE_IF_NOT_EXISTS)
    catalog.createTable(tableA, CreateMode.CREATE_IF_NOT_EXISTS)
    catalog.createTable(tableB, CreateMode.CREATE_IF_NOT_EXISTS)
    catalog
  }

  private def analyzeSingle(sql: String, rule: AnalyzerContext => PlanRewriter): LogicalPlan = {
    analyze(sql, List(rule))
  }

  private def analyze(
      sql: String,
      rules: List[AnalyzerContext => PlanRewriter] = TypeResolver.typerRules
  ): LogicalPlan = {
    val plan = SQLParser.parse(sql)
    debug(s"original plan:\n${plan.pp}")
    val analyzerContext = AnalyzerContext("default", demoCatalog).withAttributes(plan.outputAttributes)

    val resolvedPlan = rules.foldLeft(plan) { (targetPlan, rule) =>
      val rewriter: PlanRewriter = rule(analyzerContext)
      val newPlan                = targetPlan.transform(rewriter)
      newPlan
    }

    debug(s"new plan:\n${resolvedPlan.pp}")
    resolvedPlan
  }

  test("resolveTableRef") {
    test("resolve all columns") {
      val p = analyzeSingle("select * from A", TypeResolver.resolveTableRef)
      p.inputAttributes shouldBe Seq(
        ResolvedAttribute(Some(tableA), "id", DataType.LongType),
        ResolvedAttribute(Some(tableA), "name", DataType.StringType)
      )
    }

    test("resolve the right table") {
      val p = analyzeSingle("select * from B", TypeResolver.resolveTableRef)
      p.inputAttributes shouldBe Seq(
        ResolvedAttribute(Some(tableB), "id", DataType.LongType),
        ResolvedAttribute(Some(tableB), "name", DataType.StringType)
      )
    }
  }

  test("resolveRelation") {

    test("resolve a filter") {
      val p = analyze(s"select * from A where id = 1")
      p.inputAttributes shouldBe Seq(
        ResolvedAttribute(Some(tableA), "id", DataType.LongType),
        ResolvedAttribute(Some(tableA), "name", DataType.StringType)
      )
      p.children.headOption shouldBe defined
      p.children.head.expressions shouldBe List(
        Expression.Eq(
          ResolvedAttribute(Some(tableA), "id", DataType.LongType),
          Expression.LongLiteral(1)
        )
      )
    }

    test("resolve a filter condition for multiple tables") {
      val p = analyze(s"select A.id id_a, B.id id_b from A, B where A.id = 1 and B.id = 2")
      p match {
        case Project(Filter(_, And(Eq(a, LongLiteral(1)), Eq(b, LongLiteral(2)))), _) =>
          a shouldBe ResolvedAttribute(Some(tableA), "id", DataType.LongType)
          b shouldBe ResolvedAttribute(Some(tableB), "id", DataType.LongType)
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("detect ambiguous column references") {
      val ex = intercept[SQLError] {
        analyze(s"select * from A, B where id = 1")
      }
      ex.errorCode shouldBe SQLErrorCode.SyntaxError
    }

    test("resolve union") {
      val p = analyze("select * from A union all select * from B")
      // TODO merging same column names from different tables
      pending("merge UNION columns")
    }
  }

  test("resolve aggregation queries") {
    test("group by column name") {
      val p = analyze("select id, count(*) from A group by id")
      p match {
        case Aggregate(
              _,
              _,
              List(GroupingKey(ResolvedAttribute(Some(tbl), "id", DataType.LongType))),
              _
            ) if tbl == tableA =>

        case _ =>
          fail(s"unexpected plan: ${p}")
      }
    }

    test("group by index") {
      val p = analyze("select id, count(*) from A group by 1")
      p match {
        case Aggregate(
              _,
              _,
              List(GroupingKey(ResolvedAttribute(Some(tbl), "id", DataType.LongType))),
              _
            ) if tbl == tableA =>
        case _ =>
          fail(s"unexpected plan: ${p}")
      }
    }

    test("group by multiple keys") {
      val p = analyze("select id, name, count(*) from A group by 1, 2")
      p match {
        case Aggregate(
              _,
              _,
              List(
                GroupingKey(ResolvedAttribute(Some(tbl1), "id", DataType.LongType)),
                GroupingKey(ResolvedAttribute(Some(tbl2), "name", DataType.StringType))
              ),
              _
            ) if tbl1 == tableA && tbl2 == tableA =>
        case _ =>
          fail(s"unexpected plan: ${p}")
      }
    }
  }
}
