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
import wvlet.airframe.sql.analyzer.SQLAnalyzer.PlanRewriter
import wvlet.airframe.sql.catalog.Catalog._
import wvlet.airframe.sql.catalog.{Catalog, DataType, InMemoryCatalog}
import wvlet.airframe.sql.model.Expression.{And, Cast, Eq, FunctionCall, GroupingKey, LongLiteral, SingleColumn}
import wvlet.airframe.sql.model.LogicalPlan.{Aggregate, Filter, Project}
import wvlet.airframe.sql.model.{Expression, LogicalPlan, LogicalPlanPrinter, ResolvedAttribute}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.airspec.AirSpec

class TypeResolverTest extends AirSpec {

  private val a1 = TableColumn("id", DataType.LongType, properties = Map("tag" -> Seq("personal_identifier")))
  private val a2 = TableColumn("name", DataType.StringType, properties = Map("tag" -> Seq("private")))

  private val tableA = Catalog.newTable(
    "default",
    "A",
    Catalog.newSchema
      .addColumn(a1)
      .addColumn(a2)
  )

  private val b1 = TableColumn("id", DataType.LongType, properties = Map("tag" -> Seq("personal_identifier")))
  private val b2 = TableColumn("name", DataType.StringType, properties = Map("tag" -> Seq("private")))

  private val tableB = Catalog.newTable(
    "default",
    "B",
    Catalog.newSchema
      .addColumn(b1)
      .addColumn(b2)
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
        ResolvedAttribute("id", DataType.LongType, Some(tableA), Some(a1)),
        ResolvedAttribute("name", DataType.StringType, Some(tableA), Some(a2))
      )
    }

    test("resolve the right table") {
      val p = analyzeSingle("select * from B", TypeResolver.resolveTableRef)
      p.inputAttributes shouldBe Seq(
        ResolvedAttribute("id", DataType.LongType, Some(tableB), Some(b1)),
        ResolvedAttribute("name", DataType.StringType, Some(tableB), Some(b2))
      )
    }

    test("resolve database.table name format") {
      val p = analyzeSingle("select * from default.A", TypeResolver.resolveTableRef)
      p.inputAttributes shouldBe Seq(
        ResolvedAttribute("id", DataType.LongType, Some(tableA), Some(a1)),
        ResolvedAttribute("name", DataType.StringType, Some(tableA), Some(a2))
      )
    }
  }

  test("resolveRelation") {

    test("resolve a filter") {
      val p = analyze(s"select * from A where id = 1")
      p.inputAttributes shouldBe Seq(
        ResolvedAttribute("id", DataType.LongType, Some(tableA), Some(a1)),
        ResolvedAttribute("name", DataType.StringType, Some(tableA), Some(a2))
      )
      p.children.headOption shouldBe defined
      p.children.head.expressions shouldBe List(
        Expression.Eq(
          ResolvedAttribute("id", DataType.LongType, Some(tableA), Some(a1)),
          Expression.LongLiteral(1)
        )
      )
    }

    test("resolve a filter condition for multiple tables") {
      val p = analyze(s"select A.id id_a, B.id id_b from A, B where A.id = 1 and B.id = 2")
      p match {
        case Project(Filter(_, And(Eq(a, LongLiteral(1)), Eq(b, LongLiteral(2)))), _) =>
          a shouldBe ResolvedAttribute("id", DataType.LongType, Some(tableA), Some(a1))
          b shouldBe ResolvedAttribute("id", DataType.LongType, Some(tableB), Some(b1))
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
              List(GroupingKey(ResolvedAttribute("id", DataType.LongType, Some(`tableA`), Some(`a1`)))),
              _
            ) =>

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
              List(GroupingKey(ResolvedAttribute("id", DataType.LongType, Some(`tableA`), Some(`a1`)))),
              _
            ) =>
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
                GroupingKey(ResolvedAttribute("id", DataType.LongType, Some(`tableA`), Some(`a1`))),
                GroupingKey(ResolvedAttribute("name", DataType.StringType, Some(`tableA`), Some(`a2`)))
              ),
              _
            ) =>
        case _ =>
          fail(s"unexpected plan: ${p}")
      }
    }

    test("group by with renamed keys") {
      val p = analyze("select xxx, count(*) from (select id as xxx from A) group by 1")
      p match {
        case Aggregate(
              _,
              _,
              List(
                GroupingKey(
                  ResolvedAttribute("xxx", DataType.LongType, Some(`tableA`), Some(`a1`))
                )
              ),
              _
            ) =>
        case _ =>
          fail(s"unexpected plan: ${p}")
      }
    }
  }

  test("resolve CTE (WITH statement) queries") {
    test("parse WITH statement") {
      val p = analyze("with q1 as (select id from A) select id from q1")
      p.outputAttributes.toList shouldBe List(ResolvedAttribute("id", DataType.LongType, Some(tableA), Some(a1)))
    }

    test("parse multiple WITH sub queries") {
      val p = analyze("with q1 as (select id, name from A), q2 as (select name from q1) select * from q2")
      p.outputAttributes.toList shouldBe List(ResolvedAttribute("name", DataType.StringType, Some(tableA), Some(a2)))
    }

    test("parse WITH statement with column aliases") {
      val p = analyze("with q1(p1, p2) as (select id, name from A) select * from q1")
      p.outputAttributes.toList shouldBe List(
        // The output should use aliases from the source columns
        ResolvedAttribute("p1", DataType.LongType, Some(tableA), Some(a1)),
        ResolvedAttribute("p2", DataType.StringType, Some(tableA), Some(a2))
      )
    }

    test("parse WITH statement referenced in grouping indexes") {
      val p = analyze("""WITH X AS (
        |  SELECT id FROM A
        |)
        |SELECT max(id), COUNT(*)
        |FROM X GROUP BY 1""".stripMargin)
    }
  }

  test("resolve join attributes") {
    test("join with USING") {
      val p = analyze("select id, A.name from A join B using(id)")
      p.outputAttributes shouldBe List(
        ResolvedAttribute("id", DataType.LongType, Some(tableA), Some(a1)),
        ResolvedAttribute("name", DataType.StringType, Some(tableA), Some(a2))
      )
    }

    test("join with on") {
      val p = analyze("select id, A.name from A join B on A.id = B.id")
      p.outputAttributes shouldBe List(
        ResolvedAttribute("id", DataType.LongType, Some(tableA), Some(a1)),
        ResolvedAttribute("name", DataType.StringType, Some(tableA), Some(a2))
      )
    }

    test("join with different column names") {
      val p = analyze("select pid, name from A join (select id pid from B) on A.id = B.pid")
      p.outputAttributes shouldBe List(
        ResolvedAttribute("pid", DataType.LongType, Some(tableB), Some(b1)),
        ResolvedAttribute("name", DataType.StringType, Some(tableA), Some(a2))
      )
    }

    test("3-way joins") {
      pending("TODO")
    }
  }

  test("resolve UDF inputs") {
    def analyzeAndCollectFunctions(sql: String): List[Expression] = {
      val p = analyze(sql)
      val exprs = p.collectExpressions {
        case f: FunctionCall => true
        case c: Cast         => true
      }
      exprs
    }

    def collectResolvedInputArgs(e: Expression): List[Expression] = {
      e.collectExpressions { case r: ResolvedAttribute => true }
    }

    val r1 = ResolvedAttribute("id", DataType.LongType, Some(tableA), Some(a1))
    val r2 = ResolvedAttribute("name", DataType.StringType, Some(tableA), Some(a2))

    test("simple function") {
      val fns   = analyzeAndCollectFunctions("select max(id) from A")
      val attrs = fns.flatMap(collectResolvedInputArgs)
      attrs shouldBe List(r1)
    }

    test("function args with cast") {
      val fns = analyzeAndCollectFunctions("select max(cast(id as double)) from A")
      fns match {
        case List(
              f @ FunctionCall("max", _, _, _, _),
              c @ Cast(_, "double", _)
            ) =>
          collectResolvedInputArgs(f) shouldBe List(r1)
          collectResolvedInputArgs(c) shouldBe List(r1)
        case other =>
          fail(s"Unexpected functions: ${other}")
      }
    }

    test("aggregation query") {
      val fns = analyzeAndCollectFunctions("select id, max(name) from A group by id")
      fns.flatMap(collectResolvedInputArgs) shouldBe List(r2)
    }
  }
}
