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

import wvlet.airframe.sql.catalog.Catalog._
import wvlet.airframe.sql.catalog.{Catalog, DataType, InMemoryCatalog}
import wvlet.airframe.sql.model.Expression._
import wvlet.airframe.sql.model.LogicalPlan.{
  Aggregate,
  Distinct,
  Except,
  Filter,
  InnerJoin,
  Intersect,
  Join,
  Project,
  Query,
  Sort,
  With
}
import wvlet.airframe.sql.model.{
  Attribute,
  CTERelationRef,
  ColumnPath,
  Expression,
  LogicalPlan,
  NodeLocation,
  ResolvedAttribute,
  SourceColumn
}
import wvlet.airframe.sql.parser.{SQLGenerator, SQLParser}
import wvlet.airframe.sql.{SQLError, SQLErrorCode}
import wvlet.airspec.AirSpec
import wvlet.log.Logger

class TypeResolverTest extends AirSpec with ResolverTestHelper {

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

  private val c1 = TableColumn("id", DataType.LongType, properties = Map("tag" -> Seq("personal_identifier")))
  private val c2 = TableColumn("name", DataType.StringType, properties = Map("tag" -> Seq("private")))

  private val tableC = Catalog.newTable(
    "default",
    "C",
    Catalog.newSchema
      .addColumn(c1)
      .addColumn(c2)
  )

  override protected def demoCatalog: Catalog = {
    val catalog = new InMemoryCatalog(
      "global",
      None,
      Seq.empty
    )

    catalog.createDatabase(Catalog.Database("default"), CreateMode.CREATE_IF_NOT_EXISTS)
    catalog.createTable(tableA, CreateMode.CREATE_IF_NOT_EXISTS)
    catalog.createTable(tableB, CreateMode.CREATE_IF_NOT_EXISTS)
    catalog.createTable(tableC, CreateMode.CREATE_IF_NOT_EXISTS)
    catalog
  }

  private val ra1 = ResolvedAttribute("id", DataType.LongType, None, Some(SourceColumn(tableA, a1)), None)
  private val ra2 = ResolvedAttribute("name", DataType.StringType, None, Some(SourceColumn(tableA, a2)), None)
  private val rb1 = ResolvedAttribute("id", DataType.LongType, None, Some(SourceColumn(tableB, b1)), None)
  private val rb2 = ResolvedAttribute("name", DataType.StringType, None, Some(SourceColumn(tableB, b2)), None)
  private val rc1 = ResolvedAttribute("id", DataType.LongType, None, Some(SourceColumn(tableC, c1)), None)
  private val rc2 = ResolvedAttribute("name", DataType.StringType, None, Some(SourceColumn(tableC, c2)), None)

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
    val p = analyze("select default.A.id from default.A")
    p.outputAttributes shouldBe List(
      ra1.withQualifier("default.A")
    )
  }

  test("resolveRelation") {

    test("resolve a filter") {
      val p = analyze(s"select * from A where id = 1")
      p.inputAttributes shouldBe Seq(ra1, ra2)
      p.children.headOption shouldBe defined
      p.children.head.childExpressions.toList shouldMatch { case List(Eq(`ra1`, LongLiteral(1, _), _)) =>
        ()
      }
    }

    test("resolve a filter condition for multiple tables") {
      val p = analyze(s"select A.id id_a, B.id id_b from A, B where A.id = 1 and B.id = 2")
      p shouldMatch {
        case Project(Filter(_, And(Eq(a, LongLiteral(1, _), _), Eq(b, LongLiteral(2, _), _), _), _), _, _) =>
          a shouldBe ra1.withQualifier("A")
          b shouldBe rb1.withQualifier("B")
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
    test("u1: resolve union") {
      val p = analyze("select id from A union all select id from B")
      p.inputAttributes shouldBe List(ra1, ra2, rb1, rb2)
      p.outputAttributes shouldBe List(
        MultiSourceColumn(List(ra1, rb1), None, None)
      )
    }

    test("u2: resolve union from the same source") {
      val p = analyze("select id from A union all select id from A")
      p.inputAttributes shouldBe List(ra1, ra2, ra1, ra2)
      p.outputAttributes shouldBe List(
        MultiSourceColumn(List(ra1, ra1), None, None)
      )
    }

    test("resolve union with select *") {
      val p = analyze("select * from A union all select * from B")
      p.inputAttributes shouldBe List(ra1, ra2, rb1, rb2)
      p.outputAttributes shouldMatch {
        case Seq(
              MultiSourceColumn(
                Seq(AllColumns(None, Some(Seq(`ra1`, `ra2`)), _), AllColumns(None, Some(Seq(`rb1`, `rb2`)), _)),
                None,
                _
              )
            ) =>
      }
    }

    test("ru1: resolve select * from union sub query") {
      val p = analyze("select * from (select * from A union all select * from B)")
      p.inputAttributes shouldMatch {
        case Seq(
              MultiSourceColumn(
                Seq(
                  AllColumns(None, Some(Seq(`ra1`, `ra2`)), _),
                  AllColumns(None, Some(Seq(`rb1`, `rb2`)), _)
                ),
                None,
                _
              )
            ) =>
          ()
      }
    }

    test("ru2: resolve union with column alias") {
      val p = analyze("select p1 from (select id as p1 from A union all select id as p1 from B)")
      p.inputAttributes shouldMatch { case Seq(m @ MultiSourceColumn(Seq(c1, c2), None, _)) =>
        m.name shouldBe "p1"
        c1 shouldBe ra1.withAlias("p1")
        c2 shouldBe rb1.withAlias("p1")
      }
      p.outputAttributes shouldMatch { case Seq(MultiSourceColumn(Seq(c1, c2), None, _)) =>
        c1 shouldBe ra1.withAlias("p1")
        c2 shouldBe rb1.withAlias("p1")
      }
    }

    test("ru3: resolve union with column alias and qualifier") {
      val p = analyze("select q1.p1 from (select id as p1 from A union all select id as p1 from B) q1")
      p.inputAttributes shouldMatch { case Seq(m @ MultiSourceColumn(Seq(c1, c2), _, _)) =>
        m.name shouldBe "p1"
        c1 shouldBe ra1.withAlias("p1")
        c2 shouldBe rb1.withAlias("p1")
      }
      p.outputAttributes shouldMatch { case Seq(m @ MultiSourceColumn(Seq(c1, c2), Some("q1"), _)) =>
        c1 shouldBe ra1.withAlias("p1").withQualifier("q1")
        c2 shouldBe rb1.withAlias("p1").withQualifier("q1")
      }
    }

    test("ru4: resolve aggregation key with union") {
      val p   = analyze("select count(*), id from (select * from A union all select * from B) group by id")
      val agg = p shouldMatch { case a: Aggregate => a }
      agg.groupingKeys(0).child shouldMatch { case m @ MultiSourceColumn(Seq(`ra1`, `rb1`), _, _) =>
        m.name shouldBe "id"
      }
    }

    test("resolve union with expression") {
      val p = analyze("select id + 1 from A union all select id + 1 from B")
      p.inputAttributes shouldBe List(ra1, ra2, rb1, rb2)
      p.outputAttributes.head shouldMatch {
        case MultiSourceColumn(
              Seq(
                SingleColumn(ArithmeticBinaryExpr(Add, `ra1`, LongLiteral(1, _), _), None, _),
                SingleColumn(ArithmeticBinaryExpr(Add, `rb1`, LongLiteral(1, _), _), None, _)
              ),
              _,
              _
            ) =>

      }
    }

    test("resolve intersect") {
      val p = analyze("select id from A intersect select id from B") // => Distinct(Intersect(...))
      p shouldMatch { case Distinct(i @ Intersect(_, _), _) =>
        i.inputAttributes shouldBe List(ra1, ra2, rb1, rb2)
        i.outputAttributes shouldMatch { case Seq(m @ MultiSourceColumn(Seq(`ra1`, `rb1`), _, _)) =>
          m.name shouldBe "id"
        }
      }
    }

    test("resolve except") {
      val p = analyze("select id from A except select id from B") // => Distinct(Except(...))
      p shouldMatch { case Distinct(e @ Except(_, _, _), _) =>
        e.inputAttributes shouldBe List(ra1, ra2) // TODO ra2 shouldn't be included?
        e.outputAttributes shouldBe List(ra1)
      }
    }
  }

  test("resolve aggregation queries") {
    test("group by column name") {
      val p = analyze("select id, count(*) from A group by id")
      p shouldMatch { case Aggregate(_, _, List(GroupingKey(`ra1`, _)), _, _) =>
        ()
      }
    }

    test("group by index") {
      val p = analyze("select id, count(*) from A group by 1")
      p shouldMatch { case Aggregate(_, _, List(GroupingKey(`ra1`, _)), _, _) =>
        ()
      }
    }

    test("group by multiple keys") {
      val p = analyze("select id, name, count(*) from A group by 1, 2")
      p shouldMatch { case Aggregate(_, _, List(GroupingKey(`ra1`, _), GroupingKey(`ra2`, _)), _, _) =>
        ()
      }
    }

    test("a1: resolve qualified column used in GROUP BY clause") {
      val p = analyze("SELECT a.cnt, a.name FROM (SELECT count(id) cnt, name FROM A GROUP BY name) a")
      p.outputAttributes shouldMatch { case Seq(c1, c2) =>
        c1 shouldMatch { case ResolvedAttribute("cnt", DataType.LongType, Some("a"), None, _) => }
        c2 shouldBe ra2.withQualifier("a")
      }
    }

    test("a2: group by with renamed keys") {
      val p   = analyze("select xxx, count(*) from (select id as xxx from A) group by 1")
      val agg = p shouldMatch { case a: Aggregate => a }

      agg.groupingKeys shouldMatch { case List(GroupingKey(r: Attribute, _)) =>
        r shouldMatch { case ResolvedAttribute("xxx", DataType.LongType, _, c, _) =>
          c shouldBe Some(SourceColumn(tableA, a1))
        }
      }
    }

    test("a3: resolve having") {
      val p = analyze("select id, count(*) cnt from A group by id having count(*) > 10")
      p shouldMatch {
        case Aggregate(
              _,
              List(c1, Alias(_, "cnt", SingleColumn(f: FunctionCall, _, _), _)),
              List(GroupingKey(`ra1`, _)),
              Some(GreaterThan(col, LongLiteral(10, _), _)),
              _
            ) if c1.name == "id" && f.functionName == "count" =>
          f.args shouldMatch { case List(AllColumns(_, Some(cols), _)) =>
            cols.toSet shouldBe Set(ra1, ra2)
          }
          col shouldMatch { case FunctionCall("count", Seq(ac: AllColumns), false, _, _, _) =>
            ac.columns shouldNotBe empty
            ac.columns.get.collect { case r: ResolvedAttribute => r }.toSet shouldBe Set(ra1, ra2)
          }
      }
    }
  }

  test("resolve CTE (WITH statement) queries") {
    test("w1: parse WITH statement") {
      val p = analyze("with q1 as (select id from A) select id from q1")
      p.outputAttributes.toList shouldMatch {
        case List(ResolvedAttribute("id", DataType.LongType, Some("q1"), Some(SourceColumn(`tableA`, `a1`)), _)) =>
      }
    }

    test("resolve CTE redundant column alias") {
      val p = analyze("with q1 as (select id as id from A) select id from q1")
      p.outputAttributes.toList shouldMatch {
        case List(ResolvedAttribute("id", DataType.LongType, Some("q1"), Some(SourceColumn(`tableA`, `a1`)), _)) =>
      }
    }

    test("parse multiple WITH sub queries") {
      val p = analyze("with q1 as (select id, name from A), q2 as (select name from q1) select * from q2")
      p.outputAttributes.toList shouldMatch { case List(AllColumns(None, Some(Seq(c)), _)) =>
        c shouldBe ra2.copy(qualifier = Some("q2"))
      }
    }

    test("parse WITH statement with column aliases") {
      val p = analyze("with q1(p1, p2) as (select id, name from A) select * from q1")
      p.outputAttributes.toList shouldMatch {
        // The output should use aliases from the source columns
        case List(AllColumns(None, Some(Seq(c1, c2)), _)) =>
          c1 shouldMatch { case Alias(Some("q1"), "p1", `ra1`, _) => }
          c2 shouldMatch { case Alias(Some("q1"), "p2", `ra2`, _) => }
      }
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
      p.outputAttributes shouldBe List(ra1.withQualifier("q1"))

      val joinKeys = p
        .collectExpressions { case _: JoinOnEq =>
          true
        }.map(_.asInstanceOf[JoinOnEq].keys)
      joinKeys shouldBe List(List(ra2.withQualifier("q1"), ra2.withQualifier("q2")))
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
      p.outputAttributes shouldMatch { case List(AllColumns(None, Some(Seq(c1, c2)), _)) =>
        c1 shouldBe ra1.copy(qualifier = Some("a"))
        c2 shouldBe ra2.copy(qualifier = Some("a"))
      }
    }
  }

  test("error on resolving join keys") {
    val e = intercept[SQLError] {
      analyze("select id, a.name from A a join B b on a.pid = b.id")
    }
    e.errorCode shouldBe SQLErrorCode.ColumnNotFound
    e.message.contains("UnresolvedAttribute") shouldBe false
  }

  test("join: resolve join attributes") {
    test("j1: join with USING") {
      val p = analyze("select id, A.name from A join B using(id)")
      p.outputAttributes shouldMatch { case Seq(m @ MultiSourceColumn(Seq(c1, c2), _, _), c3) =>
        m.name shouldBe "id"
        c1 shouldBe ra1.withQualifier("A")
        c2 shouldBe rb1.withQualifier("B")
        c3 shouldBe ra2.withQualifier("A")
      }
    }

    test("j2: resolve USING with 3 tables") {
      val p = analyze("""select id, count(1)
          |from A a
          |join B b using (id)
          |join C c using (id)
          |group by id
          |having count(1) > 160
          |""".stripMargin)

      val joinKeys = p.collectExpressions { case u: ResolvedJoinUsing =>
        true
      }
      joinKeys shouldMatch {
        case List(
              ResolvedJoinUsing(Seq(MultiSourceColumn(Seq(c1, c2), _, _)), _),
              ResolvedJoinUsing(Seq(MultiSourceColumn(Seq(c3, c4, c5), _, _)), _)
            ) =>
          c1 shouldBe ra1.withQualifier("a")
          c2 shouldBe rb1.withQualifier("b")

          c3 shouldBe ra1.withQualifier("a")
          c4 shouldBe rb1.withQualifier("b")
          c5 shouldBe rc1.withQualifier("c")
      }
    }

    test("j3: join with on") {
      val p = analyze("select A.id, A.name, B.name from A join B on A.id = B.id")
      p.outputAttributes shouldMatch { case List(c1, c2, c3) =>
        c1 shouldBe ra1.withQualifier("A")
        c2 shouldBe ra2.withQualifier("A")
        c3 shouldBe rb2.withQualifier("B")
      }
    }

    test("j4: join with on condition for aliased columns") {
      val p = analyze("select a.id, a.name, b.name from A a join B b on a.id = b.id")
      p.outputAttributes shouldMatch { case List(c1, c2, c3) =>
        c1 shouldBe ra1.withQualifier("a")
        c2 shouldBe ra2.withQualifier("a")
        c3 shouldBe rb2.withQualifier("b")
      }
    }

    test("j5: join with on condition for qualified columns") {
      val p =
        analyze("select A.id, A.name, B.name from default.A join default.B on default.A.id = default.B.id")
      p.outputAttributes shouldMatch { case List(c1, c2, c3) =>
        c1 shouldBe ra1.withQualifier("A")
        c2 shouldBe ra2.withQualifier("A")
        c3 shouldBe rb2.withQualifier("B")
      }
    }

    test("j6: join with different column names") {
      val p = analyze("select pid, name from A join (select id as pid from B) on A.id = pid")
      p.outputAttributes shouldMatch {
        case List(
              ResolvedAttribute("pid", DataType.LongType, None, Some(SourceColumn(`tableB`, `b1`)), _),
              ResolvedAttribute("name", DataType.StringType, Some("A"), Some(SourceColumn(`tableA`, `a2`)), _)
            ) =>
          ()
      }
    }

    test("j7: refer to duplicated key of equi join") {
      val p = analyze("select B.id from A inner join B on A.id = B.id")
      p.outputAttributes shouldMatch {
        case List(ResolvedAttribute("id", DataType.LongType, Some("B"), Some(SourceColumn(`tableB`, `b1`)), _)) =>
      }
    }

    test("j8: 3-way joins") {
      val p = analyze("select A.id, B.id, C.id from A join B on A.id = B.id join C on B.id = C.id")
      p.outputAttributes shouldMatch { case List(c1, c2, c3) =>
        c1 shouldBe ra1.withQualifier("A")
        c2 shouldBe rb1.withQualifier("B")
        c3 shouldBe rc1.withQualifier("C")
      }
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
      fns shouldMatch {
        case List(
              f @ FunctionCall("max", _, _, _, _, _),
              c @ Cast(_, "double", _, _)
            ) =>
          collectResolvedInputArgs(f) shouldBe List(ra1)
          collectResolvedInputArgs(c) shouldBe List(ra1)
      }
    }

    test("aggregation query") {
      val fns = analyzeAndCollectFunctions("select id, max(name) from A group by id")
      fns.flatMap(collectResolvedInputArgs) shouldBe List(ra2)
    }
  }

  test("sub query: resolve sub queries in FROM clause") {
    test("n1: resolve a sub query") {
      val p = analyze("SELECT id, name FROM (SELECT id, name FROM A)")
      p.outputAttributes.toList shouldBe List(ra1, ra2)
    }

    test("n2: resolve a sub query with column aliases") {
      val p = analyze("SELECT p1, p2 FROM (SELECT id as p1, name as p2 FROM A)")
      p.outputAttributes.toList shouldBe List(ra1.copy(name = "p1"), ra2.copy(name = "p2"))
    }

    test("n3: resolve a sub query with SELECT *") {
      val p = analyze("SELECT id, name FROM (SELECT * FROM A)")
      p.outputAttributes.toList shouldBe List(ra1, ra2)
    }

    test("n4: resolve a sub query with table alias") {
      val p = analyze("SELECT a.id, a.name FROM (SELECT id, name FROM A) a")
      p.outputAttributes.toList shouldBe List(
        ra1.withQualifier("a"),
        ra2.withQualifier("a")
      )
    }

    test("n5: resolve nested sub queries") {
      val p = analyze("SELECT id, name FROM (SELECT id, name FROM (SELECT id, name FROM A))")
      p.outputAttributes.toList shouldBe List(ra1, ra2)
    }

    test("n6: resolve join keys from nested sub queries") {
      val p = analyze("""select * from
          |(select id from (select id from A)) x
          |inner join
          |(select id from (select id from B)) y on x.id = y.id""".stripMargin)
      p.outputAttributes shouldMatch { case Seq(AllColumns(_, Some(c), _)) =>
        c shouldMatch { case List(c1, c2) =>
          c1 shouldBe ra1.withQualifier("x")
          c2 shouldBe rb1.withQualifier("y")
        }
      }
      p shouldMatch { case Project(Join(_, _, _, join: JoinOnEq, _), _, _) =>
        join.keys shouldBe List(
          ra1.withQualifier("x"),
          rb1.withQualifier("y")
        )
      }
    }
  }

  test("resolve sub queries in WHERE clause") {
    test("resolve a sub query in IN") {
      val p = analyze("SELECT id FROM A WHERE A.id IN (SELECT * FROM B WHERE id = 1)")
      p shouldMatch { case Project(Filter(_, InSubQuery(_, Project(f: Filter, _, _), _), _), _, _) =>
        f.filterExpr shouldMatch { case Eq(`rb1`, LongLiteral(1, _), _) => () }
        f.outputAttributes shouldBe List(rb1, rb2)
      }
    }

    test("resolve a sub query in NOT IN") {
      val p = analyze("SELECT id FROM A WHERE A.id NOT IN (SELECT * FROM B WHERE id = 1)")
      p shouldMatch { case Project(Filter(_, NotInSubQuery(_, Project(f: Filter, _, _), _), _), _, _) =>
        f.filterExpr shouldMatch { case Eq(rb1, LongLiteral(1, _), _) => () }
        f.outputAttributes shouldBe List(rb1, rb2)
      }
    }

    test("resolve a sub query in EXISTS") {
      val p = analyze("SELECT id FROM A WHERE EXISTS (SELECT * FROM B WHERE B.id = A.id)")
      p shouldMatch { case Project(Filter(_, Exists(SubQueryExpression(Project(f: Filter, _, _), _), _), _), _, _) =>
        f.filterExpr shouldMatch { case Eq(c1, c2, _) =>
          c1 shouldBe rb1.withQualifier("B")
          c2 shouldBe ra1.withQualifier("A")
        }
        f.outputAttributes shouldBe List(rb1, rb2)
      }
    }

    test("resolve a scalar sub query") {
      val p = analyze("SELECT id FROM A WHERE id = (SELECT max(id) FROM B WHERE name = 'one')")
      p shouldMatch { case Project(Filter(_, Eq(left, SubQueryExpression(Project(f: Filter, _, _), _), _), _), _, _) =>
        left shouldBe ra1
        f.filterExpr shouldMatch { case Eq(`rb2`, StringLiteral("one", _), _) => () }
        f.outputAttributes shouldBe List(rb1, rb2)
      }
    }
  }

  test("exp: resolve expression column") {
    test("resolve expression column from sub query") {
      val p = analyze("SELECT id, name FROM (SELECT id + 1 as id, name FROM A) a WHERE a.id = 99")

      p.outputAttributes.toList shouldMatch {
        case List(
              ResolvedAttribute("id", DataType.LongType, _, _, _),
              ResolvedAttribute("name", DataType.StringType, Some("a"), _, _)
            ) =>
      }

      p shouldMatch { case Project(filter @ Filter(_, _, _), _, _) =>
        filter.filterExpr shouldMatch { case Eq(r: Attribute, LongLiteral(99, _), _) =>
          // a.id is transformed (with +1), so no need to propagate column tags
          r.fullName shouldBe "a.id"
        }
      }
    }

    test("resolve expression column from CTE") {
      val p = analyze("WITH q1 AS (SELECT id + 1 as id, name FROM A) SELECT id, name FROM q1 WHERE q1.id = 99")
      p.outputAttributes.toList shouldMatch {
        case List(
              ResolvedAttribute("id", DataType.LongType, _, _, _),
              ResolvedAttribute("name", DataType.StringType, Some("q1"), _, _)
            ) =>
      }

      p match {
        case Query(With(_, _, _), Project(filter @ Filter(CTERelationRef(_, _, _), _, _), _, _), _) =>
          filter.filterExpr shouldMatch { case Eq(r: Attribute, LongLiteral(99, _), _) =>
            // q1.id is transformed (with +1), so no need to propagate column tags
            r.fullName shouldBe "q1.id"
          }
      }
    }
  }

  test("count: resolve count(*)") {
    test("resolve simple count(*)") {
      val p = analyze("select count(*) from A")
      p.outputAttributes shouldMatch {
        case List(SingleColumn(FunctionCall("count", Seq(c @ AllColumns(_, _, _)), _, _, _, _), _, _)) =>
          c.columns shouldBe Some(Seq(ra1, ra2))
      }
    }

    test("resolve count(*) in expression") {
      val p = analyze("select count(*) + 1 from A")
      p.outputAttributes shouldMatch {
        case List(
              SingleColumn(
                ArithmeticBinaryExpr(
                  _,
                  FunctionCall("count", Seq(c @ AllColumns(_, _, _)), _, _, _, _),
                  LongLiteral(1, _),
                  _
                ),
                _,
                _
              )
            ) =>
          c.columns shouldBe Some(Seq(ra1, ra2))
      }
    }

    test("resolve count(*) in sub query") {
      val p = analyze("select cnt from (select count(*) as cnt from A)")
      p.outputAttributes shouldMatch { case List(ResolvedAttribute("cnt", DataType.LongType, _, _, _)) => }
    }

    test("resolve count(*) in CTE") {
      val p = analyze("WITH q AS (select count(*) as cnt from A) select cnt from q")
      p.outputAttributes shouldMatch { case List(ResolvedAttribute("cnt", DataType.LongType, Some("q"), _, _)) => }
    }

    test("resolve count(*) in Union") {
      val p = analyze("select count(*) as cnt from A union all select count(*) as cnt from B")
      p.outputAttributes shouldMatch { case Seq(m: MultiSourceColumn) =>
        m.inputs.size shouldBe 2
        m.inputs(0) shouldMatch {
          case Alias(_, "cnt", SingleColumn(f: FunctionCall, _, _), _) if f.functionName == "count" =>
            f.args.size shouldBe 1
            f.args(0).asInstanceOf[AllColumns].columns shouldBe Some(Seq(ra1, ra2))
        }
        m.inputs(1) shouldMatch {
          case Alias(_, "cnt", SingleColumn(f: FunctionCall, _, _), _) if f.functionName == "count" =>
            f.args.size shouldBe 1
            f.args(0).asInstanceOf[AllColumns].columns shouldBe Some(Seq(rb1, rb2))
        }
      }
    }

    test("resolve count(*) from Union") {
      val p = analyze("select count(*) from (select id from A union all select id from B)")
      p.outputAttributes shouldMatch {
        case List(
              SingleColumn(
                FunctionCall(
                  "count",
                  Seq(ac: AllColumns),
                  false,
                  None,
                  None,
                  _
                ),
                None,
                _
              )
            ) =>
          ac.columns shouldMatch { case Some(Seq(m @ MultiSourceColumn(Seq(`ra1`, `rb1`), _, _))) =>
            m.name shouldBe "id"
          }
      }
    }
  }

  test("sort: resolve order by") {
    test("s1: resolve simple order by") {
      val p = analyze("""SELECT id, name FROM A ORDER BY id""".stripMargin)
      p.asInstanceOf[Sort].orderBy.toList shouldMatch { case List(SortItem(`ra1`, None, None, _)) =>
        ()
      }
    }

    test("s2: resolve order by alias") {
      val p = analyze("""SELECT * FROM (SELECT id as p1, name FROM A) ORDER BY p1""".stripMargin)
      p.asInstanceOf[Sort].orderBy.toList shouldMatch { case List(SortItem(c, None, None, _)) =>
        c.attributeName shouldBe "p1"
        c.dataType shouldBe DataType.LongType
      }
    }

    test("s3: resolve order by index") {
      val p = analyze("""SELECT id, name FROM A ORDER BY 1""".stripMargin)
      p.asInstanceOf[Sort].orderBy.toList shouldMatch { case List(SortItem(c, None, None, _)) =>
        c.attributeName shouldBe "id"
        c.dataType shouldBe DataType.LongType
      }
    }

    test("s4: resolve order by with duplicated join key") {
      val p = analyze("""SELECT A.id FROM A INNER JOIN B on A.id = B.id ORDER BY B.id DESC""".stripMargin)
      p.asInstanceOf[Sort].orderBy.toList shouldMatch { case List(SortItem(c1, Some(Descending), None, _)) =>
        c1 shouldBe rb1.withQualifier("B")
      }
    }
  }

  test("resolve UNNEST") {
    test("resolve UNNEST array column") {
      val p = analyze("SELECT id, n FROM A CROSS JOIN UNNEST (name) AS t (n)")
      p.outputAttributes shouldMatch { case List(c1: Attribute, c2: Attribute) =>
        c1.fullName shouldBe "A.id"
        c2.fullName shouldBe "t.n"
      }
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
        ResolvedAttribute("key", DataType.StringType, Some("t"), None, None),
        ResolvedAttribute("value", DataType.LongType, Some("t"), None, None)
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
                  m1,
                  m2
                )
              ),
              _
            )
          ) =>
        m1.fullName shouldBe "t.id"
        m1.dataType shouldBe DataType.LongType
        m2.fullName shouldBe "t.name"
        m2.dataType shouldBe DataType.StringType
    }
  }

  test("resolve select 1 from subquery") {
    val p = analyze("select cnt from (select cnt from (select 1 as cnt))")
    p.outputAttributes shouldMatch { case List(ResolvedAttribute(cnt, DataType.LongType, _, _, _)) =>
      ()
    }
  }

  test("resolve select * from (select 1)") {
    val p = analyze("select * from (select 1)")
    p.outputAttributes shouldMatch { case List(AllColumns(None, Some(List(r: Attribute)), _)) =>
      r.dataType shouldBe DataType.LongType
    }
  }

  test("resolve nested aggregations") {
    val p = analyze("""select name, count(*) cnt from (
        |  select id, arbitrary(name) name from A
        |  group by 1
        |)
        |group by 1
        |""".stripMargin)
    p.outputAttributes shouldMatch {
      case List(c1: Attribute, c2: Attribute) if c1.name == "name" && c2.name == "cnt" =>
    }
  }

  test("resolve join keys with qualifiers") {
    val p = analyze("""select count(*)
      |  from
      |    (select * from A) t1
      |  join
      |    (select * from B) t2
      |  on t1.id = t2.id
      |""".stripMargin)

    p shouldMatch { case Project(Join(InnerJoin, _, _, JoinOnEq(Seq(k1: Attribute, k2: Attribute), _), _), _, _) =>
      k1.fullName shouldBe "t1.id"
      k2.fullName shouldBe "t2.id"
    }
  }
}
