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
package wvlet.airframe.sql.parser

import org.antlr.v4.runtime.{ParserRuleContext, Token}
import org.antlr.v4.runtime.tree.TerminalNode
import wvlet.airframe.sql.SQLErrorCode
import wvlet.log.LogSupport
import wvlet.airframe.sql.model._
import wvlet.airframe.sql.model.LogicalPlan._
import wvlet.airframe.sql.parser.SqlBaseParser._

object SQLInterpreter {
  private[parser] def unquote(s: String): String = {
    s.substring(1, s.length - 1).replace("''", "'")
  }
}

/**
  * ANTLR parse tree -> SQL LogicalPlan
  */
class SQLInterpreter(withNodeLocation: Boolean = true) extends SqlBaseBaseVisitor[Any] with LogSupport {
  import SQLInterpreter._
  import wvlet.airframe.sql.model.Expression._

  import scala.jdk.CollectionConverters._

  private val parserRules            = SqlBaseParser.ruleNames.toList.asJava
  private var parameterPosition: Int = 0

  private def print(ctx: ParserRuleContext): String = {
    ctx.toStringTree(parserRules)
  }

  private def unknown(ctx: ParserRuleContext): Exception = {
    new IllegalArgumentException("Unknown parser context: " + ctx.toStringTree(parserRules))
  }

  private def getLocation(token: Token): Option[NodeLocation] = {
    if (withNodeLocation) {
      Some(NodeLocation(token.getLine, token.getCharPositionInLine + 1))
    } else {
      None
    }
  }

  private def getLocation(ctx: ParserRuleContext): Option[NodeLocation] = getLocation(ctx.getStart)

  private def getLocation(node: TerminalNode): Option[NodeLocation] = getLocation(node.getSymbol)

  def interpret(ctx: ParserRuleContext): LogicalPlan = {
    trace(s"interpret: ${print(ctx)}")
    val m = ctx.accept(this)
    trace(m)
    m.asInstanceOf[LogicalPlan]
  }

  override def visitSingleStatement(ctx: SingleStatementContext): LogicalPlan = {
    visit(ctx.statement()).asInstanceOf[LogicalPlan]
  }

  override def visitStatementDefault(ctx: StatementDefaultContext): LogicalPlan = {
    visit(ctx.query()).asInstanceOf[LogicalPlan]
  }

  override def visitQuery(ctx: QueryContext): Relation = {
    val inputRelation = visit(ctx.queryNoWith()).asInstanceOf[Relation]

    if (ctx.`with`() == null) {
      inputRelation
    } else {
      val w = visitWith(ctx.`with`())
      Query(w, inputRelation, getLocation(ctx))
    }
  }

  override def visitWith(ctx: WithContext): With = {
    val queries = ctx.namedQuery().asScala.map(x => visitNamedQuery(x)).toSeq
    With(false, queries, getLocation(ctx))
  }

  override def visitNamedQuery(ctx: NamedQueryContext): WithQuery = {
    val name = visitIdentifier(ctx.name)
    val columnAliases = Option(ctx.columnAliases()).map { x =>
      x.identifier()
        .asScala
        .map { i =>
          visitIdentifier(i)
        }
        .toSeq
    }
    WithQuery(name, visitQuery(ctx.query()), columnAliases, getLocation(ctx))
  }

  private def visitIdentifier(ctx: IdentifierContext): Identifier = {
    visit(ctx).asInstanceOf[Identifier]
  }

  override def visitInlineTable(ctx: InlineTableContext): LogicalPlan = {
    Values(ctx.expression().asScala.map(expression _).toSeq, getLocation(ctx))
  }

  override def visitRowConstructor(ctx: RowConstructorContext): RowConstructor = {
    RowConstructor(ctx.expression().asScala.map(expression _).toSeq, getLocation(ctx))
  }

  override def visitSetOperation(ctx: SetOperationContext): LogicalPlan = {
    val children =
      Seq(ctx.left, ctx.right).map(x => visit(x).asInstanceOf[Relation]).toSeq
    val isDistinct = Option(ctx.setQuantifier())
      .map(visitSetQuantifier(_).isDistinct)
      .getOrElse(true)
    val base = if (ctx.INTERSECT() != null) {
      Intersect(children, getLocation(ctx.INTERSECT()))
    } else if (ctx.UNION() != null) {
      Union(children, getLocation(ctx.UNION()))
    } else if (ctx.EXCEPT() != null) {
      Except(children(0), children(1), getLocation(ctx.EXCEPT()))
    } else {
      throw unknown(ctx)
    }
    if (isDistinct) {
      Distinct(base, base.nodeLocation)
    } else {
      base
    }
  }

  override def visitQueryNoWith(ctx: QueryNoWithContext): LogicalPlan = {
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
      Sort(inputRelation, sortKeys, getLocation(ctx.ORDER()))
    }

    if (ctx.limit == null) {
      withSort
    } else {
      Option(ctx.INTEGER_VALUE())
        .map { limit =>
          val l = LongLiteral(limit.getText.toLong, getLocation(limit))
          Limit(withSort, l, getLocation(ctx.limit))
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
          NullIsFirst
        case SqlBaseParser.LAST =>
          NullIsLast
      }
    }
    SortItem(key, ordering, nullOrdering, getLocation(ctx))
  }

  override def visitQueryTermDefault(ctx: QueryTermDefaultContext): LogicalPlan = {
    visit(ctx.queryPrimary()).asInstanceOf[LogicalPlan]
  }

  override def visitQueryPrimaryDefault(ctx: QueryPrimaryDefaultContext): LogicalPlan = {
    visit(ctx.querySpecification()).asInstanceOf[LogicalPlan]
  }

  override def visitQuerySpecification(ctx: QuerySpecificationContext): LogicalPlan = {
    val filter: Option[Expression] = {
      if (ctx.where == null) {
        None
      } else {
        Option(ctx.where)
          .map(visit(_))
          .collectFirst { case e: Expression => e }
      }
    }

    val inputRelation: Relation = (fromClause(ctx), filter) match {
      case (Some(r), Some(f)) =>
        Filter(r, f, getLocation(ctx.where))
      case (Some(r), None) =>
        r
      case (None, Some(f)) =>
        Filter(EmptyRelation(None), f, getLocation(ctx.where))
      case _ =>
        EmptyRelation(None)
    }

    val selectItem: List[Attribute] = ctx
      .selectItem()
      .asScala
      .map { x =>
        visit(x).asInstanceOf[Attribute]
      }
      .toList

    val withAggregation = {
      val having = Option(ctx.having).map(expression(_))

      if (ctx.groupBy() == null) {
        // No aggregation in the query
        // Check the presence of distinct
        val distinct = Option(ctx.setQuantifier())
          .map(visitSetQuantifier(_).isDistinct)
          .getOrElse(false)

        having match {
          case Some(h) =>
            // Aggregation without grouping keys
            if (distinct) {
              Aggregate(Distinct(inputRelation, getLocation(ctx)), selectItem, List.empty, Some(h), getLocation(ctx))
            } else {
              Aggregate(inputRelation, selectItem, List.empty, Some(h), getLocation(ctx))
            }
          case None =>
            // Regular aggregate
            val p = Project(inputRelation, selectItem, getLocation(ctx))
            if (distinct) {
              Distinct(p, p.nodeLocation)
            } else {
              p
            }
        }
      } else {
        // aggregation
        val gb = ctx.groupBy()
        assert(gb != null)
        if (inputRelation.isInstanceOf[EmptyRelation]) {
          throw SQLErrorCode.SyntaxError.newException(
            "group by statement requires input relation",
            inputRelation.nodeLocation
          )
        }

        // group by
        val groupByKeys =
          gb.expression()
            .asScala
            .map { x =>
              val e = expression(x)
              GroupingKey(e, e.nodeLocation)
            }
            .toList

        // having
        Aggregate(inputRelation, selectItem, groupByKeys, having, getLocation(ctx))
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
              Some(
                Join(ImplicitJoin, l, right, NaturalJoin(l.nodeLocation), l.nodeLocation)
              ) // TODO nodeLocation is correct? => Maybe not, but giving a close location is ok for now
          }
        }
      }
  }

  override def visitRelationDefault(ctx: RelationDefaultContext): Relation = {
    visitAliasedRelation(ctx.aliasedRelation())
  }

  override def visitLateralView(ctx: LateralViewContext): Relation = {
    val left          = visit(ctx.left).asInstanceOf[Relation]
    val exprs         = ctx.expression().asScala.map(expression).toSeq
    val tableAlias    = visitIdentifier(ctx.tableAlias)
    val columnAliases = ctx.identifier().asScala.tail.map(visitIdentifier).toSeq
    LateralView(left, exprs, tableAlias, columnAliases, getLocation(ctx))
  }

  override def visitAliasedRelation(ctx: AliasedRelationContext): Relation = {
    val r: Relation = ctx.relationPrimary() match {
      case p: ParenthesizedRelationContext =>
        ParenthesizedRelation(visit(p.relation()).asInstanceOf[Relation], getLocation(ctx))
      case u: UnnestContext =>
        val ord = Option(u.ORDINALITY()).map(x => true).getOrElse(false)
        Unnest(
          u.expression().asScala.toSeq.map(x => expression(x)),
          withOrdinality = ord,
          getLocation(ctx)
        )
      case s: SubqueryRelationContext =>
        visitQuery(s.query())
      case l: LateralContext =>
        Lateral(visitQuery(l.query()), getLocation(ctx))
      case t: TableNameContext =>
        TableRef(QName(t.qualifiedName().getText, getLocation(t)), getLocation(ctx))
      case other =>
        throw unknown(other)
    }

    ctx.identifier() match {
      case i: IdentifierContext =>
        AliasedRelation(r, visitIdentifier(i), None, getLocation(ctx))
      case other =>
        r
    }
  }

  override def visitJoinRelation(ctx: JoinRelationContext): LogicalPlan = {
    val tmpJoinType = ctx.joinType() match {
      case null                     => None
      case jt if jt.LEFT() != null  => Some(LeftOuterJoin)
      case jt if jt.RIGHT() != null => Some(RightOuterJoin)
      case jt if jt.FULL() != null  => Some(FullOuterJoin)
      case _ if ctx.CROSS() != null => Some(CrossJoin)
      case _                        => None
    }

    val (joinType, joinCriteria, right) = Option(ctx.joinCriteria()) match {
      case Some(c) if c.USING() != null =>
        (
          tmpJoinType.getOrElse(InnerJoin),
          JoinUsing(c.identifier().asScala.toSeq.map(visitIdentifier), getLocation(ctx)),
          ctx.rightRelation
        )
      case Some(c) if c.booleanExpression() != null =>
        (
          tmpJoinType.getOrElse(InnerJoin),
          JoinOn(expression(c.booleanExpression()), getLocation(ctx)),
          ctx.rightRelation
        )
      case _ =>
        (CrossJoin, NaturalJoin(getLocation(ctx)), ctx.right)
    }
    val l = visit(ctx.left).asInstanceOf[Relation]
    val r = visit(right).asInstanceOf[Relation]

    val j = Join(joinType, l, r, joinCriteria, getLocation(ctx))
    j
  }

  override def visitTableName(ctx: TableNameContext): TableRef = {
    val tableName = visitQualifiedName(ctx.qualifiedName())
    TableRef(tableName, getLocation(ctx))
  }

  override def visitQualifiedName(ctx: QualifiedNameContext): QName = {
    QName(ctx.identifier().asScala.map(_.getText).toList, getLocation(ctx))
  }

  override def visitDereference(ctx: DereferenceContext): Attribute = {
    UnresolvedAttribute(s"${ctx.base.getText}.${ctx.fieldName.getText}", getLocation(ctx))
  }

  override def visitSelectAll(ctx: SelectAllContext): Attribute = {
    // TODO parse qName
    ctx.qualifiedName()
    AllColumns(None, getLocation(ctx))
  }

  override def visitSelectSingle(ctx: SelectSingleContext): Attribute = {
    val alias = Option(ctx.AS())
      .map(x => expression(ctx.identifier()))
      .orElse(Option(ctx.identifier()).map(expression(_)))
    SingleColumn(expression(ctx.expression()), alias, None, getLocation(ctx))
  }

  override def visitExpression(ctx: ExpressionContext): Expression = {
    trace(s"expr: ${print(ctx)}")
    val b: BooleanExpressionContext = ctx.booleanExpression()
    b match {
      case lb: LogicalBinaryContext =>
        if (lb.AND() != null) {
          And(expression(lb.left), expression(lb.right), getLocation(ctx))
        } else if (lb.OR() != null) {
          Or(expression(lb.left), expression(lb.right), getLocation(ctx))
        } else {
          throw unknown(lb)
        }
      case ln: LogicalNotContext =>
        if (ln.NOT() != null) {
          Not(expression(ln.booleanExpression()), getLocation(ctx))
        } else {
          throw unknown(ln)
        }
      case bd: BooleanDeafaultContext =>
        visitPredicated(bd.predicated())
      case other =>
        warn(s"Unknown expression: ${other.getClass}")
        visit(ctx.booleanExpression()).asInstanceOf[Expression]
    }
  }

  override def visitLogicalNot(ctx: LogicalNotContext): Expression = {
    Not(expression(ctx.booleanExpression()), getLocation(ctx))
  }

  private def expression(ctx: ParserRuleContext): Expression = {
    ctx.accept(this).asInstanceOf[Expression]
  }

  override def visitValueExpressionDefault(ctx: ValueExpressionDefaultContext): Expression = {
    expression(ctx.primaryExpression())
  }

  override def visitTypeConstructor(ctx: TypeConstructorContext): Expression = {
    val v = expression(ctx.str()).asInstanceOf[StringLiteral].value

    if (ctx.DOUBLE_PRECISION() != null) {
      // TODO Parse double-type precision properly
      GenericLiteral("DOUBLE", v, getLocation(ctx))
    } else {
      val tpe = ctx.identifier().getText
      tpe.toLowerCase match {
        case "time"      => TimeLiteral(v, getLocation(ctx))
        case "timestamp" => TimestampLiteral(v, getLocation(ctx))
        // TODO Parse decimal-type precision properly
        case "decimal" => DecimalLiteral(v, getLocation(ctx))
        case "char"    => CharLiteral(v, getLocation(ctx))
        case other =>
          GenericLiteral(tpe, v, getLocation(ctx))
      }
    }
  }

  override def visitBasicStringLiteral(ctx: BasicStringLiteralContext): StringLiteral = {
    StringLiteral(unquote(ctx.STRING().getText), getLocation(ctx))
  }

  override def visitUnicodeStringLiteral(ctx: UnicodeStringLiteralContext): StringLiteral = {
    // Decode unicode literal
    StringLiteral(ctx.getText, getLocation(ctx))
  }

  override def visitBinaryLiteral(ctx: BinaryLiteralContext): Expression = {
    BinaryLiteral(ctx.BINARY_LITERAL().getText, getLocation(ctx))
  }

  override def visitParameter(ctx: ParameterContext): Expression = {
    // Prepared statement parameter
    parameterPosition += 1
    Parameter(parameterPosition, getLocation(ctx))
  }

  override def visitSimpleCase(ctx: SimpleCaseContext): Expression = {
    val operand       = expression(ctx.valueExpression())
    val whenClauses   = ctx.whenClause().asScala.map(visitWhenClause(_)).toSeq
    val defaultClause = Option(ctx.elseExpression).map(expression(_))

    CaseExpr(Some(operand), whenClauses, defaultClause, getLocation(ctx))
  }

  override def visitWhenClause(ctx: WhenClauseContext): WhenClause = {
    WhenClause(expression(ctx.condition), expression(ctx.result), getLocation(ctx))
  }

  override def visitSearchedCase(ctx: SearchedCaseContext): Expression = {
    val whenClauses    = ctx.whenClause().asScala.map(visitWhenClause(_)).toSeq
    val defaultClauses = Option(ctx.elseExpression).map(expression(_))

    CaseExpr(None, whenClauses, defaultClauses, getLocation(ctx))
  }
  override def visitCast(ctx: CastContext): Expression = {
    if (ctx.CAST() != null) {
      Cast(expression(ctx.expression()), ctx.`type`().getText, tryCast = false, getLocation(ctx))
    } else if (ctx.TRY_CAST() != null) {
      Cast(expression(ctx.expression()), ctx.`type`().getText, tryCast = true, getLocation(ctx))
    } else {
      throw unknown(ctx)
    }
  }

  override def visitParenthesizedExpression(ctx: ParenthesizedExpressionContext): Expression = {
    ParenthesizedExpression(expression(ctx.expression()), getLocation(ctx))
  }

  override def visitSubqueryExpression(ctx: SubqueryExpressionContext): Expression = {
    SubQueryExpression(visitQuery(ctx.query()), getLocation(ctx))
  }

  override def visitSubquery(ctx: SubqueryContext): LogicalPlan = {
    visitQueryNoWith(ctx.queryNoWith())
  }

  override def visitPredicated(ctx: PredicatedContext): Expression = {
    val e = expression(ctx.valueExpression)
    if (ctx.predicate != null) {
      // TODO add predicate
      ctx.predicate() match {
        case n: NullPredicateContext =>
          if (n.NOT() == null) IsNull(e, getLocation(n))
          else IsNotNull(e, getLocation(n))
        case b: BetweenContext =>
          Between(e, expression(b.lower), expression(b.upper), getLocation(b))
        case i: InSubqueryContext =>
          val subQuery = visitQuery(i.query())
          if (i.NOT() == null) InSubQuery(e, subQuery, getLocation(i))
          else NotInSubQuery(e, subQuery, getLocation(i))
        case i: InListContext =>
          val inList = i.expression().asScala.map(x => expression(x)).toSeq
          if (i.NOT() == null) In(e, inList, getLocation(i))
          else NotIn(e, inList, getLocation(i))
        case l: LikeContext =>
          // TODO: Handle ESCAPE
          val likeExpr = expression(l.pattern)
          if (l.NOT() == null) Like(e, likeExpr, getLocation(l))
          else NotLike(e, likeExpr, getLocation(l))
        case d: DistinctFromContext =>
          val distinctExpr = expression(d.valueExpression())
          if (d.NOT() == null) DistinctFrom(e, distinctExpr, getLocation(d))
          else NotDistinctFrom(e, distinctExpr, getLocation(d))
        case other =>
          // TODO
          warn(s"unhandled predicate ${ctx.predicate().getClass}:\n${print(ctx.predicate())}")
          e
      }
    } else {
      e
    }
  }

  override def visitLogicalBinary(ctx: LogicalBinaryContext): Expression = {
    val left  = expression(ctx.left)
    val right = expression(ctx.right)
    ctx.operator.getType match {
      case SqlBaseParser.AND =>
        And(left, right, getLocation(ctx))
      case SqlBaseParser.OR =>
        Or(left, right, getLocation(ctx))
    }
  }

  override def visitArithmeticBinary(ctx: ArithmeticBinaryContext): Expression = {
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
    ArithmeticBinaryExpr(binaryExprType, left, right, getLocation(ctx))
  }

  override def visitComparison(ctx: ComparisonContext): Expression = {
    trace(s"comparison: ${print(ctx)}")
    val left  = expression(ctx.left)
    val right = expression(ctx.right)
    val op    = ctx.comparisonOperator().getChild(0).asInstanceOf[TerminalNode]
    op.getSymbol.getType match {
      case SqlBaseParser.EQ =>
        Eq(left, right, getLocation(ctx.comparisonOperator()))
      case SqlBaseParser.LT =>
        LessThan(left, right, getLocation(ctx.comparisonOperator()))
      case SqlBaseParser.LTE =>
        LessThanOrEq(left, right, getLocation(ctx.comparisonOperator()))
      case SqlBaseParser.GT =>
        GreaterThan(left, right, getLocation(ctx.comparisonOperator()))
      case SqlBaseParser.GTE =>
        GreaterThanOrEq(left, right, getLocation(ctx.comparisonOperator()))
      case SqlBaseParser.NEQ =>
        NotEq(left, right, getLocation(ctx.comparisonOperator()))
    }
  }

  override def visitExists(ctx: ExistsContext): Expression = {
    Exists(
      SubQueryExpression(visitQuery(ctx.query()), getLocation(ctx)),
      getLocation(ctx)
    )
  }

  override def visitBooleanLiteral(ctx: BooleanLiteralContext): Literal = {
    if (ctx.booleanValue().TRUE() != null) {
      TrueLiteral(getLocation(ctx))
    } else {
      FalseLiteral(getLocation(ctx))
    }
  }

  override def visitNumericLiteral(ctx: NumericLiteralContext): Literal = {
    visit(ctx.number()).asInstanceOf[Literal]
  }

  override def visitDoubleLiteral(ctx: DoubleLiteralContext): Literal = {
    DoubleLiteral(ctx.getText.toDouble, getLocation(ctx))
  }

  override def visitDecimalLiteral(ctx: DecimalLiteralContext): Literal = {
    DecimalLiteral(ctx.getText, getLocation(ctx))
  }

  override def visitIntegerLiteral(ctx: IntegerLiteralContext): Literal = {
    LongLiteral(ctx.getText.toLong, getLocation(ctx))
  }

  override def visitStringLiteral(ctx: StringLiteralContext): Literal = {
    val text = ctx.str().getText.replaceAll("(^'|'$)", "")
    StringLiteral(text, getLocation(ctx))
  }

  override def visitUnquotedIdentifier(ctx: UnquotedIdentifierContext): Identifier = {
    val id = Option(ctx.nonReserved()).map(_.getText).getOrElse(ctx.getText)
    UnquotedIdentifier(id, getLocation(ctx))
  }
  override def visitBackQuotedIdentifier(ctx: BackQuotedIdentifierContext): Identifier = {
    BackQuotedIdentifier(ctx.getText.replaceAll("(^`|`$)", ""), getLocation(ctx))
  }
  override def visitQuotedIdentifier(ctx: QuotedIdentifierContext): Identifier = {
    QuotedIdentifier(ctx.getText.replaceAll("(^\"|\"$)", ""), getLocation(ctx))
  }
  override def visitDigitIdentifier(ctx: DigitIdentifierContext): Identifier = {
    DigitId(ctx.getText, getLocation(ctx))
  }

  override def visitOver(ctx: OverContext): Window = {
    // PARTITION BY
    val partition = Option(ctx.PARTITION())
      .map { p =>
        ctx.partition.asScala.map(expression(_)).toSeq
      }
      .getOrElse(Seq.empty)
    val orderBy = Option(ctx.ORDER())
      .map { o =>
        ctx.sortItem().asScala.map(visitSortItem(_)).toSeq
      }
      .getOrElse(Seq.empty)
    val windowFrame = Option(ctx.windowFrame()).map(visitWindowFrame(_))

    Window(partition, orderBy, windowFrame, getLocation(ctx))
  }

  override def visitWindowFrame(ctx: WindowFrameContext): WindowFrame = {
    val s = visitFrameBound(ctx.start)
    val e = Option(ctx.BETWEEN()).map { x =>
      visitFrameBound(ctx.end)
    }
    if (ctx.RANGE() != null) {
      WindowFrame(RangeFrame, s, e, getLocation(ctx))
    } else {
      WindowFrame(RowsFrame, s, e, getLocation(ctx))
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

  override def visitBoundedFrame(ctx: BoundedFrameContext): Expression = {
    super.visitBoundedFrame(ctx).asInstanceOf[Expression]
  }

  override def visitFunctionCall(ctx: FunctionCallContext): FunctionCall = {
    val name = ctx.qualifiedName().getText
    val filter: Option[Expression] = Option(ctx.filter()).map { (f: FilterContext) =>
      expression(f.booleanExpression())
    }
    val over: Option[Window] = Option(ctx.over()).map { (o: OverContext) =>
      visitOver(o)
    }

    val isDistinct = Option(ctx.setQuantifier())
      .map(x => visitSetQuantifier(x).isDistinct)
      .getOrElse(false)

    if (ctx.ASTERISK() != null) {
      FunctionCall(
        name,
        Seq(AllColumns(None, getLocation(ctx))),
        isDistinct,
        filter,
        over,
        getLocation(ctx)
      )
    } else {
      val args = ctx.expression().asScala.map(expression(_)).toSeq
      FunctionCall(name, args, isDistinct, filter, over, getLocation(ctx))
    }
  }

  override def visitSetQuantifier(ctx: SetQuantifierContext): SetQuantifier = {
    if (ctx.DISTINCT() != null) {
      DistinctSet(getLocation(ctx))
    } else {
      All(getLocation(ctx))
    }
  }

  override def visitNullLiteral(ctx: NullLiteralContext): Literal = NullLiteral(getLocation(ctx))

  override def visitInterval(ctx: IntervalContext): IntervalLiteral = {
    val sign = if (ctx.MINUS() != null) {
      Negative
    } else {
      Positive
    }

    val value = ctx.str().getText

    val from = visitIntervalField(ctx.from)
    val to   = Option(ctx.TO()).map(x => visitIntervalField(ctx.intervalField(0)))

    IntervalLiteral(unquote(value), sign, from, to, getLocation(ctx))
  }

  override def visitIntervalField(ctx: IntervalFieldContext): IntervalField = {
    if (ctx.YEAR() != null) {
      Year(getLocation(ctx.YEAR()))
    } else if (ctx.MONTH() != null) {
      Month(getLocation(ctx.MONTH()))
    } else if (ctx.DAY() != null) {
      Day(getLocation(ctx.DAY()))
    } else if (ctx.HOUR() != null) {
      Hour(getLocation(ctx.HOUR()))
    } else if (ctx.MINUTE() != null) {
      Minute(getLocation(ctx.MINUTE()))
    } else if (ctx.SECOND() != null) {
      Second(getLocation(ctx.SECOND()))
    } else {
      throw unknown(ctx)
    }
  }

  override def visitArrayConstructor(ctx: ArrayConstructorContext): Expression = {
    val elems = ctx.expression().asScala.map(expression(_)).toSeq
    ArrayConstructor(elems, getLocation(ctx))
  }

  override def visitCreateSchema(ctx: CreateSchemaContext): LogicalPlan = {
    val schemaName  = visitQualifiedName(ctx.qualifiedName())
    val ifNotExists = Option(ctx.EXISTS()).map(_ => true).getOrElse(false)
    val props = Option(ctx.properties())
      .map(
        _.property().asScala
          .map { p =>
            val key   = visitIdentifier(p.identifier())
            val value = expression(p.expression())
            SchemaProperty(key, value, getLocation(p))
          }.toSeq
      )
    CreateSchema(schemaName, ifNotExists, props, getLocation(ctx))
  }

  override def visitDropSchema(ctx: DropSchemaContext): LogicalPlan = {
    val schemaName = visitQualifiedName(ctx.qualifiedName())
    val ifExists   = Option(ctx.EXISTS()).map(x => true).getOrElse(false)
    val cascade =
      Option(ctx.CASCADE()).map(x => true).getOrElse(false)
    DropSchema(schemaName, ifExists, cascade, getLocation(ctx))
  }

  override def visitRenameSchema(ctx: RenameSchemaContext): LogicalPlan = {
    val schemaName = visitQualifiedName(ctx.qualifiedName())
    val renameTo   = visitIdentifier(ctx.identifier())
    RenameSchema(schemaName, renameTo, getLocation(ctx))
  }

  override def visitCreateTable(ctx: CreateTableContext): LogicalPlan = {
    val ifNotExists = Option(ctx.EXISTS()).map(x => true).getOrElse(false)
    val tableName   = visitQualifiedName(ctx.qualifiedName())
    val tableElements =
      ctx.tableElement().asScala.toSeq.map(x => visitTableElement(x))
    CreateTable(tableName, ifNotExists, tableElements, getLocation(ctx))
  }

  override def visitCreateTableAsSelect(ctx: CreateTableAsSelectContext): LogicalPlan = {
    val ifNotExists = Option(ctx.EXISTS()).map(x => true).getOrElse(false)
    val tableName   = visitQualifiedName(ctx.qualifiedName())
    val columnAliases = Option(ctx.columnAliases())
      .map(_.identifier().asScala.toSeq.map(visitIdentifier(_)))
    val q = visitQuery(ctx.query())
    CreateTableAs(tableName, ifNotExists, columnAliases, q, getLocation(ctx))
  }

  override def visitTableElement(ctx: TableElementContext): TableElement = {
    Option(ctx.columnDefinition())
      .map(x => visitColumnDefinition(x))
      .getOrElse {
        val l         = ctx.likeClause()
        val tableName = visitQualifiedName(l.qualifiedName())
        val includingProps =
          Option(l.EXCLUDING()).map(x => false).getOrElse(true)
        ColumnDefLike(tableName, includingProps, getLocation(ctx))
      }
  }

  override def visitColumnDefinition(ctx: ColumnDefinitionContext): ColumnDef = {
    val name = visitIdentifier(ctx.identifier())
    val tpe  = visitType(ctx.`type`())
    ColumnDef(name, tpe, getLocation(ctx))
  }

  override def visitType(ctx: TypeContext): ColumnType = {
    ColumnType(ctx.getText, getLocation(ctx))
  }

  override def visitDropTable(ctx: DropTableContext): LogicalPlan = {
    val table    = visitQualifiedName(ctx.qualifiedName())
    val ifExists = Option(ctx.EXISTS()).map(x => true).getOrElse(false)
    DropTable(table, ifExists, getLocation(ctx))
  }

  override def visitInsertInto(ctx: InsertIntoContext): LogicalPlan = {
    val table = visitQualifiedName(ctx.qualifiedName())
    val aliases = Option(ctx.columnAliases())
      .map(x => x.identifier().asScala.toSeq)
      .map(x => x.map(visitIdentifier(_)))
    val query = visitQuery(ctx.query())
    InsertInto(table, aliases, query, getLocation(ctx))
  }

  override def visitDelete(ctx: DeleteContext): LogicalPlan = {
    val table = visitQualifiedName(ctx.qualifiedName())
    val cond = Option(ctx.booleanExpression()).map { x =>
      expression(x)
    }
    Delete(table, cond, getLocation(ctx))
  }

  override def visitRenameTable(ctx: RenameTableContext): LogicalPlan = {
    val from = visitQualifiedName(ctx.qualifiedName(0))
    val to   = visitQualifiedName(ctx.qualifiedName(1))
    RenameTable(from, to, getLocation(ctx))
  }

  override def visitRenameColumn(ctx: RenameColumnContext): LogicalPlan = {
    val table = visitQualifiedName(ctx.tableName)
    val from  = visitIdentifier(ctx.from)
    val to    = visitIdentifier(ctx.to)
    RenameColumn(table, from, to, getLocation(ctx))
  }

  override def visitDropColumn(ctx: DropColumnContext): LogicalPlan = {
    val table = visitQualifiedName(ctx.tableName)
    val c     = visitIdentifier(ctx.column)
    DropColumn(table, c, getLocation(ctx))
  }

  override def visitAddColumn(ctx: AddColumnContext): LogicalPlan = {
    val table  = visitQualifiedName(ctx.tableName)
    val coldef = visitColumnDefinition(ctx.column)
    AddColumn(table, coldef, getLocation(ctx))
  }

  override def visitCreateView(ctx: CreateViewContext): LogicalPlan = {
    val viewName = visitQualifiedName(ctx.qualifiedName())
    val replace  = Option(ctx.REPLACE()).map(x => true).getOrElse(false)
    val query    = visitQuery(ctx.query())
    CreateView(viewName, replace, query, getLocation(ctx))
  }

  override def visitDropView(ctx: DropViewContext): LogicalPlan = {
    val viewName = visitQualifiedName(ctx.qualifiedName())
    val ifExists = Option(ctx.EXISTS()).map(x => true).getOrElse(false)
    DropView(viewName, ifExists, getLocation(ctx))
  }
}
