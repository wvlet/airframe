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
package wvlet.airframe.stream.sql.parser

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import wvlet.airframe.stream.spi.SQLModel
import wvlet.airframe.stream.spi.SQLModel._
import wvlet.airframe.stream.sql.parser.SqlBaseParser._
import wvlet.log.{LogSupport, Logger}

object SQLInterpreter {
  private[parser] def unquote(s: String): String = {
    s.substring(1, s.length - 1).replace("''", "'")
  }
}

/**
  * ANTLR parse tree -> SQL model classes
  */
class SQLInterpreter extends SqlBaseBaseVisitor[SQLModel] with LogSupport {
  import SQLInterpreter._
  import scala.collection.JavaConverters._

  private val parserRules            = SqlBaseParser.ruleNames.toList.asJava
  private var parameterPosition: Int = 0

  private def print(ctx: ParserRuleContext): String = {
    ctx.toStringTree(parserRules)
  }

  private def unknown(ctx: ParserRuleContext): Exception = {
    new IllegalArgumentException("Unknown parser context: " + ctx.toStringTree(parserRules))
  }

  def interpret(ctx: ParserRuleContext): SQLModel = {
    trace(s"interpret: ${print(ctx)}")
    val m = ctx.accept(this)
    trace(m)
    m
  }

  override def visitSingleStatement(ctx: SingleStatementContext): SQLModel = {
    visit(ctx.statement())
  }

  override def visitStatementDefault(ctx: StatementDefaultContext): SQLModel = {
    visit(ctx.query())
  }

  override def visitQuery(ctx: QueryContext): Relation = {
    val inputRelation = visit(ctx.queryNoWith()).asInstanceOf[Relation]

    if (ctx.`with`() == null) {
      inputRelation
    } else {
      val w = visitWith(ctx.`with`())
      Query(w, inputRelation)
    }
  }

  override def visitWith(ctx: WithContext): With = {
    val queries = ctx.namedQuery().asScala.map(x => visitNamedQuery(x)).toSeq
    With(false, queries)
  }

  override def visitNamedQuery(ctx: NamedQueryContext): WithQuery = {
    val name = visitIdentifier(ctx.name)
    val columnAliases = Option(ctx.columnAliases()).map { x =>
      x.identifier().asScala.map { i =>
          visitIdentifier(i)
        }.toSeq
    }
    WithQuery(name, visitQuery(ctx.query()), columnAliases)
  }

  private def visitIdentifier(ctx: IdentifierContext): Identifier = {
    visit(ctx).asInstanceOf[Identifier]
  }

  override def visitSetOperation(ctx: SetOperationContext): SQLModel = {

    val children   = Seq(ctx.left, ctx.right).map(visit(_).asInstanceOf[Relation]).toSeq
    val isDistinct = Option(ctx.setQuantifier()).map(_.DISTINCT() != null).getOrElse(false)
    if (ctx.INTERSECT() != null) {
      Intersect(children, isDistinct)
    } else if (ctx.UNION() != null) {
      Union(children, isDistinct)
    } else if (ctx.EXCEPT() != null) {
      Except(children(0), children(1), isDistinct)
    } else {
      throw unknown(ctx)
    }
  }

  override def visitQueryNoWith(ctx: QueryNoWithContext): SQLModel = {
    val inputRelation = visit(ctx.queryTerm()).asInstanceOf[Relation]
    // TODO

    // TODO union, except, intersect
    val withSort = if (ctx.sortItem().isEmpty) {
      inputRelation
    } else {
      val sortKeys = ctx
        .sortItem()
        .asScala
        .map { x =>
          visitSortItem(x)
        }
        .toSeq
      Sort(inputRelation, sortKeys)
    }

    if (ctx.limit == null) {
      withSort
    } else {
      Option(ctx.INTEGER_VALUE())
        .map { limit =>
          val l = limit.getText.toInt
          Limit(withSort, l)
        }
        .getOrElse(withSort)
    }
  }

  override def visitSortItem(ctx: SortItemContext): SortItem = {
    val key = expression(ctx.expression())
    val ordering = Option(ctx.ordering).map { x =>
      x.getType match {
        case SqlBaseParser.ASC  => Ascending
        case SqlBaseParser.DESC => Descending
      }
    }

    val nullOrdering = Option(ctx.nullOrdering).map { x =>
      x.getType match {
        case SqlBaseParser.FIRST =>
          SQLModel.NullIsFirst
        case SqlBaseParser.LAST =>
          SQLModel.NullIsLast
      }
    }
    SortItem(key, ordering, nullOrdering)
  }

  override def visitQueryTermDefault(ctx: QueryTermDefaultContext): SQLModel = {
    visit(ctx.queryPrimary())
  }

  override def visitQueryPrimaryDefault(ctx: QueryPrimaryDefaultContext): SQLModel = {
    visit(ctx.querySpecification())
  }

  override def visitQuerySpecification(ctx: QuerySpecificationContext): SQLModel = {

    val inputRelation: Option[Relation] = fromClause(ctx)

    val filter: Option[Expression] = {
      if (ctx.where == null)
        None
      else {
        Option(ctx.where)
          .map(visit(_))
          .collectFirst { case e: Expression => e }
      }
    }

    val selectItem: Seq[SelectItem] = ctx
      .selectItem().asScala.map { x =>
        visit(x).asInstanceOf[SelectItem]
      }.toSeq

    val withAggregation = {
      if (ctx.groupBy() == null) {
        // No aggregation
        // TODO distinct check
        val distinct = Option(ctx.setQuantifier()).map(_.DISTINCT() != null).getOrElse(false)
        Select(distinct, selectItem, inputRelation, filter)
      } else {
        // aggregation
        val gb = ctx.groupBy()
        assert(gb != null)
        if (inputRelation.isEmpty) {
          throw new IllegalArgumentException(s"group by statement requires input relation")
        }

        // group by
        val groupByKeys =
          gb.expression()
            .asScala
            .map {
              expression(_)
            }
            .toSeq

        // having
        val having = Option(ctx.having).map(expression(_))
        Aggregate(selectItem, inputRelation, filter, groupByKeys, having)
      }
    }

    withAggregation
  }

  private def fromClause(ctx: QuerySpecificationContext): Option[Relation] = {
    Option(ctx.relation())
      .flatMap { r =>
        val relations = r.asScala
        relations.foldLeft(None: Option[Relation]) { (left, x) =>
          val right = visit(x).asInstanceOf[Relation]
          left match {
            case None    => Some(right)
            case Some(l) =>
              // TODO resolve join types
              Some(Join(ImplicitJoin, l, right, NaturalJoin))
          }
        }
      }
  }

  override def visitRelationDefault(ctx: RelationDefaultContext): Relation = {
    visitAliasedRelation(ctx.aliasedRelation())
  }

  override def visitAliasedRelation(ctx: AliasedRelationContext): Relation = {
    val r: Relation = ctx.relationPrimary() match {
      case p: ParenthesizedRelationContext =>
        ParenthizedRelation(visit(p.relation()).asInstanceOf[Relation])
      //case u: UnnestContext                =>
//        u.expression().asScala.map(x => expression(x))
      case s: SubqueryRelationContext =>
        visitQuery(s.query())
      case l: LateralContext =>
        visitQuery(l.query())
      case t: TableNameContext =>
        Table(QName(t.qualifiedName().getText))
      case other =>
        throw unknown(other)
    }

    ctx.identifier() match {
      case i: IdentifierContext =>
        AliasedRelation(r, i.getText, None)
      case other =>
        r
    }
  }

  override def visitJoinRelation(ctx: JoinRelationContext): SQLModel = {
    val tmpJoinType = ctx.joinType() match {
      case null                     => None
      case jt if jt.LEFT() != null  => Some(LeftOuterJoin)
      case jt if jt.RIGHT() != null => Some(RightOuterJoin)
      case jt if jt.FULL() != null  => Some(FullOuterJoin)
      case _                        => None
    }

    val (joinType, joinCriteria) = Option(ctx.joinCriteria()) match {
      case Some(c) if c.USING() != null =>
        (tmpJoinType.getOrElse(InnerJoin), JoinUsing(c.identifier().asScala.map(_.getText).toSeq))
      case Some(c) if c.booleanExpression() != null =>
        (tmpJoinType.getOrElse(InnerJoin), JoinOn(expression(c.booleanExpression())))
      case _ =>
        (CrossJoin, NaturalJoin)
    }
    val l     = visit(ctx.left).asInstanceOf[Relation]
    val right = Option(ctx.aliasedRelation()).getOrElse(ctx.relation(0))
    val r     = visit(right).asInstanceOf[Relation]
    val j     = Join(joinType, l, r, joinCriteria)
    j
  }

  override def visitTableName(ctx: TableNameContext): Table = {
    val tableName = visitQualifiedName(ctx.qualifiedName())
    Table(tableName)
  }

  override def visitQualifiedName(ctx: QualifiedNameContext): QName = {
    QName(ctx.identifier().asScala.map(_.getText).toSeq)
  }

  override def visitDereference(ctx: DereferenceContext): SQLModel = {
    QName(s"${ctx.base.getText}.${ctx.fieldName.getText}")
  }

  override def visitSelectAll(ctx: SelectAllContext): SelectItem = {
    // TODO parse qName
    ctx.qualifiedName()
    AllColumns(None)
  }

  override def visitSelectSingle(ctx: SelectSingleContext): SelectItem = {
    val alias = Option(ctx.AS()).map(x => expression(ctx.identifier()))
    SingleColumn(expression(ctx.expression()), alias)
  }

  override def visitExpression(ctx: ExpressionContext): SQLModel = {
    trace(s"expr: ${print(ctx)}")
    val b: BooleanExpressionContext = ctx.booleanExpression()
    b match {
      case lb: LogicalBinaryContext =>
        if (lb.AND() != null) {
          And(expression(lb.left), expression(lb.right))
        } else if (lb.OR() != null) {
          Or(expression(lb.left), expression(lb.right))
        } else {
          throw unknown(lb)
        }
      case ln: LogicalNotContext =>
        if (ln.NOT() != null) {
          Not(expression(ln.booleanExpression()))
        } else {
          throw unknown(ln)
        }
      case bd: BooleanDeafaultContext =>
        visitPredicated(bd.predicated())
      case other =>
        warn(s"Unknown expression: ${other.getClass}")
        visit(ctx.booleanExpression())
    }
  }

  override def visitLogicalNot(ctx: LogicalNotContext): SQLModel = {
    Not(expression(ctx.booleanExpression()))
  }

  def expression(ctx: ParserRuleContext): Expression = {
    ctx.accept(this).asInstanceOf[Expression]
  }

  override def visitValueExpressionDefault(ctx: ValueExpressionDefaultContext): Expression = {
    expression(ctx.primaryExpression())
  }

  override def visitTypeConstructor(ctx: TypeConstructorContext): Expression = {
    val v = expression(ctx.str()).asInstanceOf[StringLiteral].value

    if (ctx.DOUBLE_PRECISION() != null) {
      // TODO
      GenericLiteral("DOUBLE", v)
    } else {
      val tpe = ctx.identifier().getText
      tpe.toLowerCase match {
        case "time"      => TimeLiteral(v)
        case "timestamp" => TimestampLiteral(v)
        case "decimal"   => DecimalLiteral(v)
        case "char"      => CharLiteral(v)
        case other =>
          GenericLiteral(tpe, v)
      }
    }
  }

  override def visitBasicStringLiteral(ctx: BasicStringLiteralContext): StringLiteral = {
    StringLiteral(unquote(ctx.STRING().getText))
  }

  override def visitUnicodeStringLiteral(ctx: UnicodeStringLiteralContext): StringLiteral = {
    // Decode unicode literal
    StringLiteral(ctx.getText)
  }

  override def visitBinaryLiteral(ctx: BinaryLiteralContext): Expression = {
    BinaryLiteral(ctx.BINARY_LITERAL().getText)
  }

  override def visitParameter(ctx: ParameterContext): Expression = {
    // Prepared statement parameter
    parameterPosition += 1
    Parameter(parameterPosition)
  }

  override def visitSimpleCase(ctx: SimpleCaseContext): Expression = {
    val operand       = expression(ctx.valueExpression())
    val whenClauses   = ctx.whenClause().asScala.map(visitWhenClause(_)).toSeq
    val defaultClause = Option(ctx.elseExpression).map(expression(_))

    CaseExpr(Some(operand), whenClauses, defaultClause)
  }

  override def visitWhenClause(ctx: WhenClauseContext): WhenClause = {
    WhenClause(expression(ctx.condition), expression(ctx.result))
  }

  override def visitSearchedCase(ctx: SearchedCaseContext): Expression = {
    val whenClauses    = ctx.whenClause().asScala.map(visitWhenClause(_)).toSeq
    val defaultClauses = Option(ctx.elseExpression).map(expression(_))

    CaseExpr(None, whenClauses, defaultClauses)
  }
  override def visitCast(ctx: CastContext): Expression = {
    if (ctx.CAST() != null) {
      Cast(expression(ctx.expression()), ctx.`type`().getText)
    } else if (ctx.TRY_CAST() != null) {
      Cast(expression(ctx.expression()), ctx.`type`().getText, tryCast = true)
    } else {
      throw unknown(ctx)
    }
  }

  override def visitParenthesizedExpression(ctx: ParenthesizedExpressionContext): Expression = {
    ParenthizedExpression(expression(ctx.expression()))
  }

  override def visitSubqueryExpression(ctx: SubqueryExpressionContext): Expression = {
    SubQueryExpression(visitQuery(ctx.query()))
  }

  override def visitSubquery(ctx: SubqueryContext): SQLModel = {
    visitQueryNoWith(ctx.queryNoWith())
  }

  override def visitPredicated(ctx: PredicatedContext): Expression = {
    val e = expression(ctx.valueExpression)
    if (ctx.predicate != null) {
      // TODO add predicate
      ctx.predicate() match {
        case n: NullPredicateContext =>
          if (n.NOT() == null) IsNull(e) else IsNotNull(e)
        case b: BetweenContext =>
          Between(e, expression(b.lower), expression(b.upper))
        case i: InSubqueryContext =>
          val subQuery = visitQuery(i.query())
          if (i.NOT() == null) InSubQuery(e, subQuery) else NotInSubQuery(e, subQuery)
        case i: InListContext =>
          val inList = i.expression().asScala.map(x => expression(x)).toSeq
          if (i.NOT() == null) In(e, inList) else NotIn(e, inList)
        case l: LikeContext =>
          // TODO: Handle ESCAPE
          val likeExpr = expression(l.pattern)
          if (l.NOT() == null) Like(e, likeExpr) else NotLike(e, likeExpr)
        case d: DistinctFromContext =>
          val distinctExpr = expression(d.valueExpression())
          if (d.NOT() == null) DistinctFrom(e, distinctExpr) else NotDistinctFrom(e, distinctExpr)
        case other =>
          // TODO
          warn(s"unhandled predicate ${ctx.predicate().getClass}:\n${print(ctx.predicate())}")
          e
      }
    } else {
      e
    }
  }

  override def visitLogicalBinary(ctx: LogicalBinaryContext): SQLModel = {
    val left  = expression(ctx.left)
    val right = expression(ctx.right)
    ctx.operator.getType match {
      case SqlBaseParser.AND =>
        And(left, right)
      case SqlBaseParser.OR =>
        Or(left, right)
    }
  }

  override def visitArithmeticBinary(ctx: ArithmeticBinaryContext): SQLModel = {
    val left  = expression(ctx.left)
    val right = expression(ctx.right)
    val binaryExprType: BinaryExprType =
      ctx.operator match {
        case op if ctx.PLUS() != null     => Add
        case op if ctx.MINUS() != null    => Subtract
        case op if ctx.ASTERISK() != null => Multiply
        case op if ctx.SLASH() != null    => Divide
        case op if ctx.PERCENT() != null  => Modulus
        case _ =>
          throw unknown(ctx)
      }
    ArithmeticBinaryExpr(binaryExprType, left, right)
  }

  override def visitComparison(ctx: ComparisonContext): Expression = {
    trace(s"comparison: ${print(ctx)}")
    val left  = expression(ctx.left)
    val right = expression(ctx.right)
    val op    = ctx.comparisonOperator().getChild(0).asInstanceOf[TerminalNode]
    op.getSymbol.getType match {
      case SqlBaseParser.EQ =>
        Eq(left, right)
      case SqlBaseParser.LT =>
        LessThan(left, right)
      case SqlBaseParser.LTE =>
        LessThanOrEq(left, right)
      case SqlBaseParser.GT =>
        GreaterThan(left, right)
      case SqlBaseParser.GTE =>
        GreaterThanOrEq(left, right)
      case SqlBaseParser.NEQ =>
        NotEq(left, right)
    }
  }

  override def visitExists(ctx: ExistsContext): Expression = {
    Exists(SubQueryExpression(visitQuery(ctx.query())))
  }

  override def visitBooleanLiteral(ctx: BooleanLiteralContext): SQLModel = {
    if (ctx.booleanValue().TRUE() != null) {
      TrueLiteral
    } else {
      FalseLiteral
    }
  }

  override def visitNumericLiteral(ctx: NumericLiteralContext): SQLModel = {
    visit(ctx.number())
  }

  override def visitDoubleLiteral(ctx: DoubleLiteralContext): SQLModel = {
    DoubleLiteral(ctx.getText.toDouble)
  }

  override def visitDecimalLiteral(ctx: DecimalLiteralContext): SQLModel = {
    DoubleLiteral(ctx.getText.toDouble)
  }

  override def visitIntegerLiteral(ctx: IntegerLiteralContext): SQLModel = {
    LongLiteral(ctx.getText.toInt)
  }

  override def visitStringLiteral(ctx: StringLiteralContext): SQLModel = {
    val text = ctx.str().getText.replaceAll("(^'|'$)", "")
    StringLiteral(text)
  }

  override def visitUnquotedIdentifier(ctx: UnquotedIdentifierContext): Identifier = {
    val id = Option(ctx.nonReserved()).map(_.getText).getOrElse(ctx.getText)
    UnquotedIdentifier(id)
  }
  override def visitBackQuotedIdentifier(ctx: BackQuotedIdentifierContext): Identifier = {
    BackQuotedIdentifier(ctx.getText.replaceAll("(^`|`$)", ""))
  }
  override def visitQuotedIdentifier(ctx: QuotedIdentifierContext): Identifier = {
    QuotedIdentifier(ctx.getText.replaceAll("(^\"|\"$)", ""))
  }
  override def visitDigitIdentifier(ctx: DigitIdentifierContext): Identifier = {
    DigitId(ctx.getText.toInt)
  }

  override def visitOver(ctx: OverContext): Window = {
    // PARTITION BY
    val partition = Option(ctx.PARTITION())
      .map { p =>
        ctx.partition.asScala.map(expression(_)).toSeq
      }.getOrElse(Seq.empty)
    val orderBy = Option(ctx.ORDER())
      .map { o =>
        ctx.sortItem().asScala.map(visitSortItem(_)).toSeq
      }.getOrElse(Seq.empty)
    val windowFrame = Option(ctx.windowFrame()).map(visitWindowFrame(_))

    Window(partition, orderBy, windowFrame)
  }

  override def visitWindowFrame(ctx: WindowFrameContext): WindowFrame = {
    val s = visitFrameBound(ctx.start)
    val e = Option(ctx.BETWEEN()).map { x =>
      visitFrameBound(ctx.end)
    }
    if (ctx.RANGE() != null) {
      WindowFrame(RangeFrame, s, e)
    } else {
      WindowFrame(RowsFrame, s, e)
    }
  }

  private def visitFrameBound(ctx: FrameBoundContext): FrameBound = {
    ctx match {
      case bf: BoundedFrameContext =>
        val bound: Long = expression(bf.expression()) match {
          case l: LongLiteral =>
            l.value
          case other =>
            throw new IllegalArgumentException(s"Unknown bound context: ${other}")
        }
        if (bf.PRECEDING() != null) {
          Preceding(bound)
        } else if (bf.FOLLOWING() != null) {
          Following(bound)
        } else {
          throw unknown(bf)
        }
      case ub: UnboundedFrameContext =>
        if (ub.PRECEDING() != null) {
          UnboundedPreceding
        } else if (ub.FOLLOWING() != null) {
          UnboundedFollowing
        } else {
          throw unknown(ctx)
        }
      case cb: CurrentRowBoundContext =>
        CurrentRow
    }
  }

  override def visitBoundedFrame(ctx: BoundedFrameContext): SQLModel = {
    super.visitBoundedFrame(ctx)
  }

  override def visitFunctionCall(ctx: FunctionCallContext): SQLModel = {
    val name = QName(ctx.qualifiedName().getText)
    val filter: Option[Expression] = Option(ctx.filter()).map { f: FilterContext =>
      expression(f.booleanExpression())
    }
    val over: Option[Window] = Option(ctx.over()).map { o: OverContext =>
      visitOver(o)
    }

    val isDistinct = Option(ctx.setQuantifier()).map(x => visitSetQuantifier(x).isDistinct).getOrElse(false)

    if (ctx.ASTERISK() != null) {
      FunctionCall(name, Seq(AllColumns(None)), isDistinct, filter, over)
    } else {
      val args = ctx.expression().asScala.map(expression(_)).toSeq
      FunctionCall(name, args, isDistinct, filter, over)
    }
  }

  override def visitSetQuantifier(ctx: SetQuantifierContext): SetQuantifier = {
    if (ctx.DISTINCT() != null) {
      Distinct
    } else {
      All
    }
  }

  override def visitNullLiteral(ctx: NullLiteralContext): SQLModel = NullLiteral

  override def visitInterval(ctx: IntervalContext): SQLModel = {
    val sign = if (ctx.MINUS() != null) {
      Negative
    } else {
      Positive
    }

    val value = ctx.str().getText

    val from = visitIntervalField(ctx.from)
    val to   = Option(ctx.TO()).map(x => visitIntervalField(ctx.intervalField(0)))

    IntervalLiteral(unquote(value), sign, from, to)
  }

  override def visitIntervalField(ctx: IntervalFieldContext): IntervalField = {
    if (ctx.YEAR() != null) {
      Year
    } else if (ctx.MONTH() != null) {
      Month
    } else if (ctx.DAY() != null) {
      Day
    } else if (ctx.HOUR() != null) {
      Hour
    } else if (ctx.MINUTE() != null) {
      Minute
    } else if (ctx.SECOND() != null) {
      Second
    } else {
      throw unknown(ctx)
    }
  }

  override def visitArrayConstructor(ctx: ArrayConstructorContext): Expression = {
    val elems = ctx.expression().asScala.map(expression(_)).toSeq
    ArrayConstructor(elems)
  }

}
