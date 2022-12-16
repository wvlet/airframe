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

import wvlet.airframe.sql.analyzer.SQLAnalyzer.PlanRewriter
import wvlet.airframe.sql.catalog.Catalog._
import wvlet.airframe.sql.catalog.{Catalog, DataType, InMemoryCatalog}
import wvlet.airframe.sql.model.Expression._
import wvlet.airframe.sql.model.LogicalPlan.{
  Aggregate,
  Distinct,
  Except,
  Filter,
  Intersect,
  Join,
  Project,
  Query,
  Sort,
  With
}
import wvlet.airframe.sql.model.{CTERelationRef, Expression, LogicalPlan, NodeLocation, ResolvedAttribute, SourceColumn}
import wvlet.airframe.sql.parser.{SQLGenerator, SQLParser}
import wvlet.airframe.sql.{SQLError, SQLErrorCode}
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

  private def analyze(
      sql: String,
      rules: List[AnalyzerContext => PlanRewriter] = TypeResolver.typerRules
  ): LogicalPlan = {
    trace(s"original sql:\n${sql}")
    trace(s"original plan:\n${SQLParser.parse(sql).pp}")

    val resolvedPlan = resolvePlan(sql, rules)
    val resolvedSql  = generateSql(resolvedPlan)
    trace(s"new plan:\n${resolvedPlan.pp}")
    trace(s"new sql:\n${resolvedSql}")

    // Round-trip plan should be able to be resolved
    resolvePlan(resolvedSql, rules)

    resolvedPlan
  }

  private def resolvePlan(sql: String, rules: List[AnalyzerContext => PlanRewriter]): LogicalPlan = {
    val plan            = SQLParser.parse(sql)
    val analyzerContext = AnalyzerContext("default", demoCatalog).withAttributes(plan.outputAttributes)
    val resolvedPlan    = TypeResolver.resolve(analyzerContext, plan, rules)
    shouldBeResolved(resolvedPlan, sql)
    resolvedPlan
  }

  private def shouldBeResolved(p: LogicalPlan, sql: String): Unit = {
    if (!p.resolved) {
      fail(
        s"""Found unresolved expressions in:
           |[sql]
           |${sql}
           |[plan]
           |${p.pp}
           |[unresolved expressions]
           |${p.unresolvedExpressions.mkString("\n")}""".stripMargin
      )
    }
  }

  private def generateSql(p: LogicalPlan): String = {
    SQLGenerator.print(p)
  }

  private val ra1 = ResolvedAttribute("id", DataType.LongType, None, Seq(SourceColumn(tableA, a1)), None)
  private val ra2 = ResolvedAttribute("name", DataType.StringType, None, Seq(SourceColumn(tableA, a2)), None)
  private val rb1 = ResolvedAttribute("id", DataType.LongType, None, Seq(SourceColumn(tableB, b1)), None)
  private val rb2 = ResolvedAttribute("name", DataType.StringType, None, Seq(SourceColumn(tableB, b2)), None)

  test("resolveTableRef") {
    test("resolve all columns") {
      val p = analyze("select * from A")
      p.inputAttributes shouldBe List(ra1, ra2)
    }

    test("resolve the right table") {
      val p = analyze("select * from B")
      p.inputAttributes shouldBe List(rb1, rb2)
    }

    test("resolve database.table name format") {
      val p = analyze("select * from default.A")
      p.inputAttributes shouldBe List(ra1, ra2)
    }
  }

  test("resolve full table name") {
    val p = analyze("select default.A.id from A")
    p.outputAttributes shouldBe List(
      ra1.withQualifier("A")
    )
  }

  test("resolveRelation") {

    test("resolve a filter") {
      val p = analyze(s"select * from A where id = 1")
      p.inputAttributes shouldBe Seq(ra1, ra2)
      p.children.headOption shouldBe defined
      p.children.head.expressions shouldBe List(
        Expression.Eq(
          ra1,
          Expression.LongLiteral(1, Some(NodeLocation(1, 28))),
          Some(NodeLocation(1, 26))
        )
      )
    }

    test("resolve a filter condition for multiple tables") {
      val p = analyze(s"select A.id id_a, B.id id_b from A, B where A.id = 1 and B.id = 2")
      p match {
        case Project(Filter(_, And(Eq(a, LongLiteral(1, _), _), Eq(b, LongLiteral(2, _), _), _), _), _, _) =>
          a shouldBe ra1.withQualifier("A")
          b shouldBe rb1.withQualifier("B")
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("detect ambiguous column references") {
      val ex = intercept[SQLError] {
        analyze(s"select * from A, B where id = 1")
      }
      ex.errorCode shouldBe SQLErrorCode.SyntaxError
    }
  }

  test("resolve set operations") {
    test("resolve union") {
      val p = analyze("select id from A union all select id from B")
      p.inputAttributes shouldBe List(ra1, ra2, rb1, rb2)
      p.outputAttributes shouldBe List(SingleColumn(MultiColumn(List(ra1, rb1), Some("id"), None), None, None, None))
    }

    test("resolve union with select *") {
      val p = analyze("select * from A union all select * from B")
      p.inputAttributes shouldBe List(ra1, ra2, rb1, rb2)
      p.outputAttributes shouldBe List(
        SingleColumn(
          MultiColumn(
            List(
              AllColumns(None, Some(Seq(ra1, ra2)), Some(NodeLocation(1, 8))),
              AllColumns(None, Some(Seq(rb1, rb2)), Some(NodeLocation(1, 34)))
            ),
            Some("*"),
            Some(NodeLocation(1, 8))
          ),
          None,
          None,
          Some(NodeLocation(1, 8))
        )
      )
    }

    test("resolve select * from union sub query") {
      val p = analyze("select * from (select * from A union all select * from B)")
      p.inputAttributes shouldBe List(
        SingleColumn(
          MultiColumn(
            List(
              AllColumns(None, Some(Seq(ra1, ra2)), Some(NodeLocation(1, 23))),
              AllColumns(None, Some(Seq(rb1, rb2)), Some(NodeLocation(1, 49)))
            ),
            Some("*"),
            Some(NodeLocation(1, 23))
          ),
          None,
          None,
          Some(NodeLocation(1, 23))
        )
      )
    }

    test("resolve union with column alias") {
      val p = analyze("select p1 from (select id as p1 from A union all select id from B)")
      p.inputAttributes shouldBe List(
        SingleColumn(
          MultiColumn(List(ra1.withAlias("p1"), rb1), Some("p1"), None),
          None,
          None,
          None
        )
      )
      p.outputAttributes shouldMatch {
        case List(SingleColumn(MultiColumn(List(c1, c2), Some("p1"), _), None, None, _)) =>
          c1 shouldBe ra1.withAlias("p1")
          c2 shouldBe rb1
      }
    }

    test("resolve union with column alias and qualifier") {
      val p = analyze("select q1.p1 from (select id as p1 from A union all select id from B) q1")
      p.inputAttributes shouldBe List(
        SingleColumn(
          MultiColumn(List(ra1.withAlias("p1"), rb1), Some("p1"), None),
          None,
          Some("q1"),
          None
        )
      )
      p.outputAttributes shouldBe List(
        SingleColumn(
          MultiColumn(List(ra1.withAlias("p1"), rb1), Some("q1.p1"), None),
          None,
          None,
          Some(NodeLocation(1, 8))
        )
      )
    }

    test("resolve aggregation key with union") {
      val p = analyze("select count(*), id from (select * from A union all select * from B) group by id")
      p.asInstanceOf[Aggregate].groupingKeys(0).child shouldMatch { case MultiColumn(List(c1, c2), Some("id"), _) =>
        c1 shouldBe ra1
        c2 shouldBe rb1
      }

    }

    test("resolve union with expression") {
      val p = analyze("select id + 1 from A union all select id + 1 from B")
      p.inputAttributes shouldBe List(ra1, ra2, rb1, rb2)
      p.outputAttributes shouldBe List(
        SingleColumn(
          MultiColumn(
            List(
              SingleColumn(
                ArithmeticBinaryExpr(Add, ra1, LongLiteral(1, Some(NodeLocation(1, 13))), Some(NodeLocation(1, 8))),
                None,
                None,
                Some(NodeLocation(1, 8))
              ),
              SingleColumn(
                ArithmeticBinaryExpr(Add, rb1, LongLiteral(1, Some(NodeLocation(1, 44))), Some(NodeLocation(1, 39))),
                None,
                None,
                Some(NodeLocation(1, 39))
              )
            ),
            Some(
              "ArithmeticBinaryExpr(Add,id:long <- [A.id],Literal(1),Some(NodeLocation(1,8)))"
            ), // TODO should be None
            Some(NodeLocation(1, 8))
          ),
          None,
          None,
          Some(NodeLocation(1, 8))
        )
      )
    }

    test("resolve intersect") {
      val p = analyze("select id from A intersect select id from B") // => Distinct(Intersect(...))
      p match {
        case Distinct(i @ Intersect(_, _), _) =>
          i.inputAttributes shouldBe List(ra1, ra2, rb1, rb2)
          i.outputAttributes shouldBe List(
            SingleColumn(MultiColumn(List(ra1, rb1), Some("id"), None), None, None, None)
          )
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("resolve except") {
      val p = analyze("select id from A except select id from B") // => Distinct(Except(...))
      p match {
        case Distinct(e @ Except(_, _, _), _) =>
          e.inputAttributes shouldBe List(ra1, ra2) // TODO ra2 shouldn't be included?
          e.outputAttributes shouldBe List(ra1)
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }
  }

  test("resolve aggregation queries") {
    test("group by column name") {
      val p = analyze("select id, count(*) from A group by id")
      p match {
        case Aggregate(
              _,
              _,
              List(GroupingKey(key1, _)),
              _,
              _
            ) =>
          key1 shouldBe ra1
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
              List(GroupingKey(key1, None)),
              _,
              Some(NodeLocation(1, 1))
            ) =>
          key1 shouldBe ra1
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
                GroupingKey(key1, None),
                GroupingKey(key2, None)
              ),
              _,
              Some(NodeLocation(1, 1))
            ) =>
          key1 shouldBe ra1
          key2 shouldBe ra2
        case _ =>
          fail(s"unexpected plan: ${p}")
      }
    }

    test("resolve qualified column used in GROUP BY clause") {
      val p = analyze("SELECT a.cnt, a.name FROM (SELECT count(id) cnt, name FROM A GROUP BY name) a")
      p.outputAttributes match {
        case List(SingleColumn(FunctionCall("count", Seq(c1), _, _, _, _), _, _, _), c2) =>
          List(c1, c2) shouldBe List(ra1, ra2.withQualifier("a"))
        case _ => fail(s"unexpected plan:\n${p.pp}")
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
                  ResolvedAttribute("xxx", DataType.LongType, None, Seq(SourceColumn(`tableA`, `a1`)), None),
                  None
                )
              ),
              _,
              Some(NodeLocation(1, 1))
            ) =>
        case _ =>
          fail(s"unexpected plan: ${p}")
      }
    }

    test("resolve having") {
      val p = analyze("select id, count(*) cnt from A group by id having cnt > 10")
      p shouldMatch {
        case Aggregate(_, _, List(GroupingKey(key1, _)), Some(GreaterThan(col, LongLiteral(10, _), _)), _) =>
          key1 shouldBe ra1
          col shouldMatch {
            case FunctionCall("count", Seq(AllColumns(None, Some(Seq(c1, c2)), _)), false, None, None, _) =>
              c1 shouldBe ra1
              c2 shouldBe ra2
          }
      }
    }
  }

  test("resolve CTE (WITH statement) queries") {
    test("parse WITH statement") {
      val p = analyze("with q1 as (select id from A) select id from q1")
      p.outputAttributes.toList shouldBe List(
        ResolvedAttribute("id", DataType.LongType, Some("q1"), Seq(SourceColumn(tableA, a1)), None)
      )
    }

    test("resolve CTE redundant column alias") {
      val p = analyze("with q1 as (select id as id from A) select id from q1")
      p.outputAttributes.toList shouldBe List(
        ResolvedAttribute("id", DataType.LongType, Some("q1"), Seq(SourceColumn(tableA, a1)), None)
      )
    }

    test("parse multiple WITH sub queries") {
      val p = analyze("with q1 as (select id, name from A), q2 as (select name from q1) select * from q2")
      p.outputAttributes.toList shouldBe List(
        AllColumns(None, Some(Seq(ra2.copy(qualifier = Some("q2")))), Some(NodeLocation(1, 73)))
      )
    }

    test("parse WITH statement with column aliases") {
      val p = analyze("with q1(p1, p2) as (select id, name from A) select * from q1")
      p.outputAttributes.toList shouldBe List(
        // The output should use aliases from the source columns
        AllColumns(
          None,
          Some(
            Seq(
              ra1.copy(name = "p1", qualifier = Some("q1")),
              ra2.copy(name = "p2", qualifier = Some("q1"))
            )
          ),
          Some(NodeLocation(1, 52))
        )
      )
    }

    test("parse WITH statement referenced in grouping indexes") {
      val p = analyze("""WITH X AS (
        |  SELECT id FROM A
        |)
        |SELECT max(id), COUNT(*)
        |FROM X GROUP BY 1""".stripMargin)
    }

    test("resolve join keys from CTEs") {
      val p = analyze("""with q1 as (
          |select * from A
          |),
          |q2 as (
          |select * from A
          |)
          |select q1.id from q1 inner join q2 ON q1.name = q2.name""".stripMargin)
      p.outputAttributes shouldBe List(ra1)

      val joinKeys = p
        .collectExpressions { case _: JoinOnEq =>
          true
        }.map(_.asInstanceOf[JoinOnEq].keys)
      joinKeys shouldBe List(List(ra2, ra2))
    }

    test("fail due to a wrong number of columns") {
      val e = intercept[SQLError] {
        val p = analyze("""WITH X(id, name) AS (
            |  SELECT id FROM A
            |)
            |SELECT id, name
            |FROM X""".stripMargin)
      }
      e.message shouldBe "line 1:6 A wrong number of columns 2 is used for WITH statement: X"
    }

    test("fail to resolve CTE") {
      val e = intercept[SQLError] {
        val p = analyze("""WITH X AS (
            |  SELECT id FROM A
            |)
            |SELECT id
            |FROM Y""".stripMargin)
      }
      e.message shouldBe "line 5:6 Table default.Y not found"
    }
  }

  test("resolve aliases") {
    test("rename table") {
      val p = analyze("select a.id from A a")
      p.outputAttributes shouldBe List(
        ra1.withQualifier("a")
      )
    }

    test("rename table and select *") {
      val p = analyze("select * from A a")
      p.outputAttributes shouldBe List(
        AllColumns(
          None,
          Some(Seq(ra1.copy(qualifier = Some("a")), ra2.copy(qualifier = Some("a")))),
          Some(NodeLocation(1, 8))
        )
      )
    }
  }

  test("error on resolving join keys") {
    val e = intercept[SQLError] {
      analyze("select id, a.name from A a join B b on a.pid = b.id")
    }
    e.errorCode shouldBe SQLErrorCode.ColumnNotFound
    e.message.contains("UnresolvedAttribute") shouldBe false
  }

  test("resolve join attributes") {
    test("join with USING") {
      val p = analyze("select id, A.name from A join B using(id)")
      p.outputAttributes shouldMatch {
        case List(
              SingleColumn(MultiColumn(List(c1, c2), Some("id"), _), None, None, _),
              c3
            ) =>
          c1 shouldBe ra1.withQualifier("A")
          c2 shouldBe rb1.withQualifier("B")
          c3 shouldBe ra2.withQualifier("A")
      }
    }

    test("resolve USING with 3 tables") {
      val p = analyze("""select a.id, count(1)
          |from A a
          |join B b using (id)
          |join B c using (name)
          |group by a.id
          |having count(1) > 160
          |""".stripMargin)

      val joins = p.collectExpressions { case _: JoinOnEq =>
        true
      }
      joins shouldMatch {
        case List(
              JoinOnEq(Seq(c1, c2), _),
              JoinOnEq(Seq(c3, c4), _)
            ) =>
          c1 shouldBe ra1.withQualifier("a")
          c2 shouldBe rb1.withQualifier("b")
          c3 shouldBe ra2.withQualifier("a")
          c4 shouldBe rb2.withQualifier("c")
      }
    }

    test("join with on") {
      val p = analyze("select id, A.name from A join B on A.id = B.id")
      p.outputAttributes shouldMatch {
        case List(
              SingleColumn(MultiColumn(List(c1, c2), Some("id"), _), None, None, _),
              c3
            ) =>
          c1 shouldBe ra1.withQualifier("A")
          c2 shouldBe rb1.withQualifier("B")
          c3 shouldBe ra2.withQualifier("A")
      }
    }

    test("join with on condition for aliased columns") {
      val p = analyze("select id, a.name from A a join B b on a.id = b.id")
      p.outputAttributes shouldMatch {
        case List(
              SingleColumn(MultiColumn(List(c1, c2), Some("id"), _), None, None, _),
              c3
            ) =>
          c1 shouldBe ra1.withQualifier("a")
          c2 shouldBe rb1.withQualifier("b")
          c3 shouldBe ra2.withQualifier("a")
      }
    }

    test("join with on condition for qualified columns") {
      val p = analyze("select id, default.A.name from default.A join default.B on default.A.id = default.B.id")
      p.outputAttributes shouldMatch {
        case List(
              SingleColumn(MultiColumn(List(c1, c2), Some("id"), _), None, None, _),
              c3
            ) =>
          c1 shouldBe ra1.withQualifier("A")
          c2 shouldBe rb1.withQualifier("B")
          c3 shouldBe ra2.withQualifier("A")
      }
    }

    test("join with different column names") {
      val p = analyze("select pid, name from A join (select id pid from B) on A.id = B.pid")
      p.outputAttributes shouldBe List(
        ResolvedAttribute("pid", DataType.LongType, None, Seq(SourceColumn(tableB, b1)), None),
        ResolvedAttribute("name", DataType.StringType, Some("A"), Seq(SourceColumn(tableA, a2)), None)
      )
    }

    test("refer to duplicated key of equi join") {
      val p = analyze("select B.id from A inner join B on A.id = B.id")
      p.outputAttributes shouldBe List(
        SingleColumn(
          MultiColumn(List(ra1.withQualifier("A"), rb1.withQualifier("B")), Some("B.id"), None),
          None,
          None,
          Some(NodeLocation(1, 8))
        )
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

    test("simple function") {
      val fns   = analyzeAndCollectFunctions("select max(id) from A")
      val attrs = fns.flatMap(collectResolvedInputArgs)
      attrs shouldBe List(ra1)
    }

    test("function args with cast") {
      val fns = analyzeAndCollectFunctions("select max(cast(id as double)) from A")
      fns match {
        case List(
              f @ FunctionCall("max", _, _, _, _, Some(NodeLocation(1, 8))),
              c @ Cast(_, "double", _, Some(NodeLocation(1, 12)))
            ) =>
          collectResolvedInputArgs(f) shouldBe List(ra1)
          collectResolvedInputArgs(c) shouldBe List(ra1)
        case other =>
          fail(s"Unexpected functions: ${other}")
      }
    }

    test("aggregation query") {
      val fns = analyzeAndCollectFunctions("select id, max(name) from A group by id")
      fns.flatMap(collectResolvedInputArgs) shouldBe List(ra2)
    }
  }

  test("resolve sub queries in FROM clause") {
    test("resolve a sub query") {
      val p = analyze("SELECT id, name FROM (SELECT id, name FROM A)")
      p.outputAttributes.toList shouldBe List(ra1, ra2)
    }

    test("resolve a sub query with column aliases") {
      val p = analyze("SELECT p1, p2 FROM (SELECT id as p1, name as p2 FROM A)")
      p.outputAttributes.toList shouldBe List(ra1.copy(name = "p1"), ra2.copy(name = "p2"))
    }

    test("resolve a sub query with SELECT *") {
      val p = analyze("SELECT id, name FROM (SELECT * FROM A)")
      p.outputAttributes.toList shouldBe List(ra1, ra2)
    }

    test("resolve a sub query with table alias") {
      val p = analyze("SELECT a.id, a.name FROM (SELECT id, name FROM A) a")
      p.outputAttributes.toList shouldBe List(
        ra1.withQualifier("a"),
        ra2.withQualifier("a")
      )
    }

    test("resolve nested sub queries") {
      val p = analyze("SELECT id, name FROM (SELECT id, name FROM (SELECT id, name FROM A))")
      p.outputAttributes.toList shouldBe List(ra1, ra2)
    }

    test("resolve join keys from nested sub queries") {
      val p = analyze("""select * from
          |(select id from (select id from A)) x
          |inner join
          |(select id from (select id from B)) y on x.id = y.id""".stripMargin)
      p.outputAttributes.toList shouldMatch {
        case List(
              AllColumns(
                None,
                Some(Seq(SingleColumn(MultiColumn(List(c1, c2), None, _), None, None, _))),
                _
              )
            ) =>
          c1 shouldBe ra1.copy(qualifier = Some("x"))
          c2 shouldBe rb1.copy(qualifier = Some("y"))
      }
      p match {
        case Project(Join(_, _, _, join: JoinOnEq, _), _, _) =>
          join.keys shouldBe List(
            ra1.withQualifier("x"),
            rb1.withQualifier("y")
          )
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }
  }

  test("resolve sub queries in WHERE clause") {
    test("resolve a sub query in IN") {
      val p = analyze("SELECT id FROM A WHERE A.id IN (SELECT * FROM B WHERE id = 1)")
      p match {
        case Project(Filter(_, InSubQuery(_, Project(f: Filter, _, _), _), _), _, _) =>
          f.filterExpr shouldBe Eq(rb1, LongLiteral(1, Some(NodeLocation(1, 60))), Some(NodeLocation(1, 58)))
          f.outputAttributes shouldBe List(rb1, rb2)
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("resolve a sub query in NOT IN") {
      val p = analyze("SELECT id FROM A WHERE A.id NOT IN (SELECT * FROM B WHERE id = 1)")
      p match {
        case Project(Filter(_, NotInSubQuery(_, Project(f: Filter, _, _), _), _), _, _) =>
          f.filterExpr shouldBe Eq(rb1, LongLiteral(1, Some(NodeLocation(1, 64))), Some(NodeLocation(1, 62)))
          f.outputAttributes shouldBe List(rb1, rb2)
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("resolve a sub query in EXISTS") {
      val p = analyze("SELECT id FROM A WHERE EXISTS (SELECT * FROM B WHERE B.id = A.id)")
      p match {
        case Project(Filter(_, Exists(SubQueryExpression(Project(f: Filter, _, _), _), _), _), _, _) =>
          f.filterExpr shouldBe Eq(rb1.withQualifier("B"), ra1.withQualifier("A"), Some(NodeLocation(1, 59)))
          f.outputAttributes shouldBe List(rb1, rb2)
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("resolve a scalar sub query") {
      val p = analyze("SELECT id FROM A WHERE id = (SELECT max(id) FROM B WHERE name = 'one')")
      p match {
        case Project(Filter(_, Eq(left, SubQueryExpression(Project(f: Filter, _, _), _), _), _), _, _) =>
          left shouldBe ra1
          f.filterExpr shouldBe Eq(rb2, StringLiteral("one", Some(NodeLocation(1, 65))), Some(NodeLocation(1, 63)))
          f.outputAttributes shouldBe List(rb1, rb2)
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }
  }

  test("resolve expression column") {
    test("resolve expression column from sub query") {
      val p = analyze("SELECT id, name FROM (SELECT id + 1 as id, name FROM A) a WHERE a.id = 99")

      p.outputAttributes.toList match {
        case List(SingleColumn(ArithmeticBinaryExpr(Add, c1, LongLiteral(1, _), _), _, _, _), c2) =>
          List(c1, c2) shouldBe List(ra1, ra2.withQualifier("a"))
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }

      p match {
        case Project(filter @ Filter(_, _, _), _, _) =>
          filter.filterExpr match {
            case Eq(ArithmeticBinaryExpr(Add, c, LongLiteral(1, _), _), LongLiteral(99, _), _) => c shouldBe ra1
            case _ => fail(s"unexpected plan:\n${p.pp}")
          }
      }
    }

    test("resolve expression column from CTE") {
      val p = analyze("WITH q1 AS (SELECT id + 1 as id, name FROM A) SELECT id, name FROM q1 WHERE q1.id = 99")

      p.outputAttributes.toList match {
        case List(SingleColumn(ArithmeticBinaryExpr(Add, c1, LongLiteral(1, _), _), _, _, _), c2) =>
          List(c1, c2) shouldBe List(ra1, ra2.withQualifier("q1"))
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }

      p match {
        case Query(With(_, _, _), Project(filter @ Filter(CTERelationRef(_, _, _), _, _), _, _), _) =>
          filter.filterExpr match {
            case Eq(ArithmeticBinaryExpr(Add, c, LongLiteral(1, _), _), LongLiteral(99, _), _) => c shouldBe ra1
            case _ => fail(s"unexpected plan:\n${p.pp}")
          }
      }
    }
  }

  test("resolve count(*)") {
    test("resolve simple count(*)") {
      val p = analyze("select count(*) from A")
      p.outputAttributes match {
        case List(SingleColumn(FunctionCall("count", Seq(c @ AllColumns(_, _, _)), _, _, _, _), _, _, _)) =>
          c.columns shouldBe Some(Seq(ra1, ra2))
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("resolve count(*) in expression") {
      val p = analyze("select count(*) + 1 from A")
      p.outputAttributes match {
        case List(
              SingleColumn(
                ArithmeticBinaryExpr(
                  _,
                  FunctionCall("count", Seq(c @ AllColumns(_, _, _)), _, _, _, _),
                  LongLiteral(1, _),
                  _
                ),
                _,
                _,
                _
              )
            ) =>
          c.columns shouldBe Some(Seq(ra1, ra2))
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("resolve count(*) in sub query") {
      val p = analyze("select cnt from (select count(*) as cnt from A)")
      p.outputAttributes match {
        case List(SingleColumn(FunctionCall("count", Seq(c @ AllColumns(_, _, _)), _, _, _, _), _, _, _)) =>
          c.columns shouldBe Some(Seq(ra1, ra2))
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("resolve count(*) in CTE") {
      val p = analyze("WITH q AS (select count(*) as cnt from A) select cnt from q")
      p.outputAttributes match {
        case List(SingleColumn(FunctionCall("count", Seq(c @ AllColumns(_, _, _)), _, _, _, _), _, _, _)) =>
          c.columns shouldBe Some(Seq(ra1, ra2))
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("resolve count(*) in Union") {
      val p = analyze("select count(*) as cnt from A union all select count(*) as cnt from B")
      p.outputAttributes match {
        case List(SingleColumn(m: MultiColumn, _, _, _)) =>
          m.inputs.size shouldBe 2
          m.inputs(0).asInstanceOf[SingleColumn].expr match {
            case f: FunctionCall if f.name == "count" =>
              f.args.size shouldBe 1
              f.args(0).asInstanceOf[AllColumns].columns shouldBe Some(Seq(ra1, ra2))
            case _ => fail(s"unexpected plan:\n${p.pp}")
          }
          m.inputs(1).asInstanceOf[SingleColumn].expr match {
            case f: FunctionCall if f.name == "count" =>
              f.args.size shouldBe 1
              f.args(0).asInstanceOf[AllColumns].columns shouldBe Some(Seq(rb1, rb2))
            case _ => fail(s"unexpected plan:\n${p.pp}")
          }
        case _ => fail(s"unexpected plan:\n${p.pp}")
      }
    }

    test("resolve count(*) from Union") {
      val p = analyze("select count(*) from (select id from A union all select id from B)")
      p.outputAttributes shouldMatch {
        case List(
              SingleColumn(
                FunctionCall(
                  "count",
                  Seq(
                    AllColumns(
                      None,
                      Some(Seq(SingleColumn(MultiColumn(List(c1, c2), Some("id"), _), None, None, _))),
                      _
                    )
                  ),
                  false,
                  None,
                  None,
                  _
                ),
                None,
                None,
                _
              )
            ) =>
          c1 shouldBe ra1
          c2 shouldBe rb1
      }
    }
  }

  test("resolve order by") {
    test("resolve simple order by") {
      val p = analyze("""SELECT id, name FROM A ORDER BY id""".stripMargin)
      p.asInstanceOf[Sort].orderBy shouldBe List(
        SortItem(
          ra1,
          None,
          None,
          Some(NodeLocation(1, 33))
        )
      )
    }

    test("resolve order by alias") {
      val p = analyze("""SELECT * FROM (SELECT id as p1, name FROM A) ORDER BY p1""".stripMargin)
      p.asInstanceOf[Sort].orderBy shouldBe List(
        SortItem(
          ra1.copy(name = "p1"),
          None,
          None,
          Some(NodeLocation(1, 55))
        )
      )
    }

    test("resolve order by index") {
      val p = analyze("""SELECT id, name FROM A ORDER BY 1""".stripMargin)
      p.asInstanceOf[Sort].orderBy shouldBe List(
        SortItem(
          ra1,
          None,
          None,
          Some(NodeLocation(1, 33))
        )
      )
    }

    test("resolve order by with duplicated join key") {
      val p = analyze("""SELECT A.id FROM A INNER JOIN B on A.id = B.id ORDER BY B.id DESC""".stripMargin)
      p.asInstanceOf[Sort].orderBy shouldBe List(
        SortItem(
          MultiColumn(List(ra1.withQualifier("A"), rb1.withQualifier("B")), Some("B.id"), None),
          Some(Descending),
          None,
          Some(NodeLocation(1, 57))
        )
      )
    }
  }

  test("resolve UNNEST") {
    test("resolve UNNEST array column") {
      val p = analyze("SELECT id, n FROM A CROSS JOIN UNNEST (name) AS t (n)")
      p.outputAttributes shouldBe List(
        ra1.withQualifier("A"),
        ra2.copy(name = "n", qualifier = Some("t"))
      )
    }

    test("resolve UNNEST array") {
      val p = analyze("""SELECT id, t.key, t.value FROM A
          |CROSS JOIN UNNEST (
          |  array['c1', 'c2', 'c3'],
          |  array[1, 2, 3]
          |) AS t (key, value)
          |""".stripMargin)

      p.outputAttributes shouldBe List(
        ra1.withQualifier("A"),
        ResolvedAttribute("key", DataType.AnyType, Some("t"), Nil, None),
        ResolvedAttribute("value", DataType.AnyType, Some("t"), Nil, None)
      )
    }
  }

  test("resolve select from values") {
    val p = analyze("SELECT * FROM (VALUES (1, 'one'), (2, 'two'), (3, 'three')) AS t (id, name)")
    p.outputAttributes shouldMatch {
      case List(
            AllColumns(
              None,
              Some(
                Seq(
                  SingleColumn(
                    MultiColumn(Seq(LongLiteral(1, _), LongLiteral(2, _), LongLiteral(3, _)), None, _),
                    Some("id"),
                    Some("t"),
                    _
                  ),
                  SingleColumn(
                    MultiColumn(
                      Seq(StringLiteral("one", _), StringLiteral("two", _), StringLiteral("three", _)),
                      None,
                      _
                    ),
                    Some("name"),
                    Some("t"),
                    _
                  )
                )
              ),
              _
            )
          ) =>
        ()
    }
  }

  test("resolve select 1 from subquery") {
    val p = analyze("select cnt from (select cnt from (select 1 as cnt))")
    p.outputAttributes shouldMatch { case List(SingleColumn(LongLiteral(1, _), Some("cnt"), None, _)) =>
      ()
    }
  }

  test("resolve select * from (select 1)") {
    val p = analyze("select * from (select 1)")
    p.outputAttributes shouldMatch {
      case List(AllColumns(None, Some(List(SingleColumn(LongLiteral(1, _), None, None, _))), _)) => ()
    }
  }
}
