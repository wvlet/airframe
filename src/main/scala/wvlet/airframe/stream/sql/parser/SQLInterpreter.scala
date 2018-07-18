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

/**
  * ANTLR parse tree -> SQL model classes
  */
class SQLInterpreter extends SqlBaseBaseVisitor[SQLModel] with LogSupport {
  import scala.collection.JavaConverters._
  private val parserRules = SqlBaseParser.ruleNames.toList.asJava

  private def print(ctx: ParserRuleContext): String = {
    ctx.toStringTree(parserRules)
  }

  private def unknown(ctx: ParserRuleContext): Exception = {
    new IllegalArgumentException("Unknown parser context: " + ctx.toStringTree(parserRules))
  }

  def interpret(ctx: ParserRuleContext): SQLModel = {
    trace(s"interpret: ${print(ctx)}")
    val m = ctx.accept(this)
    debug(m)
    m
  }

  override def visitSingleStatement(ctx: SingleStatementContext): SQLModel = {
    visit(ctx.statement())
  }

  override def visitStatementDefault(ctx: StatementDefaultContext): SQLModel = {
    visit(ctx.query())
  }

  override def visitQuery(ctx: QueryContext): SQLModel = {
    val inputRelation = visit(ctx.queryNoWith()).asInstanceOf[Relation]
    inputRelation
  }

  override def visitQueryNoWith(ctx: QueryNoWithContext): SQLModel = {
    val inputRelation = visit(ctx.queryTerm()).asInstanceOf[Relation]
    // TODO

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
    val key  = expression(ctx.expression())
    var sort = SortItem(key)
    if (ctx.ordering != null) {
      ctx.ordering.getType match {
        case SqlBaseParser.ASC =>
          sort = SortItem(key, ordering = Ascending)
        case SqlBaseParser.DESC =>
          sort = SortItem(key, ordering = Descending)
      }
    }

    if (ctx.nullOrdering != null) {
      ctx.nullOrdering.getType match {
        case SqlBaseParser.FIRST =>
          sort = SortItem(key, sort.ordering, nullOrdering = SQLModel.NullIsFirst)
        case SqlBaseParser.LAST =>
          sort = SortItem(key, sort.ordering, nullOrdering = SQLModel.NullIsLast)
      }
    }
    sort
  }

  override def visitQueryTermDefault(ctx: QueryTermDefaultContext): SQLModel = {
    visit(ctx.queryPrimary())
  }

  override def visitQueryPrimaryDefault(ctx: QueryPrimaryDefaultContext): SQLModel = {
    visit(ctx.querySpecification())
  }

  override def visitQuerySpecification(ctx: QuerySpecificationContext): SQLModel = {

    val inputRelation: Option[Relation] = fromClause(ctx)

    val withFilter: Option[Relation] = {
      if (ctx.where == null)
        inputRelation
      else {
        Option(ctx.where)
          .map(visit(_))
          .collectFirst { case e: Expression => e }
          .flatMap { w =>
            inputRelation.map(in => Filter(in, w))
          }
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
        Project(withFilter, false, selectItem)
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

        val g = Aggregate(withFilter.get, selectItem, groupByKeys)

        // having
        if (ctx.having != null) {
          Filter(g, expression(ctx.having))
        } else {
          g
        }
      }
    }

    withAggregation
  }

  private def fromClause(ctx: QuerySpecificationContext): Option[Relation] = {
    Option(ctx.relation())
      .flatMap { r =>
        val relations = r.asScala.toSeq
        relations.size match {
          case 1 =>
            relations.map(x => visit(x).asInstanceOf[Relation]).headOption
          case other =>
            // TODO join processing
            throw unknown(relations.head)
        }
      }
  }

  override def visitRelationDefault(ctx: RelationDefaultContext): SQLModel = {
    visit(ctx.aliasedRelation())
  }

  override def visitAliasedRelation(ctx: AliasedRelationContext): SQLModel = {
    val r = visit(ctx.relationPrimary())
    ctx.identifier() match {
      case i: IdentifierContext =>
        AliasedRelation(r.asInstanceOf[Relation], i.getText, None)
      case null =>
        r
    }
  }

  override def visitTableName(ctx: TableNameContext): Table = {
    val tableName = visitQualifiedName(ctx.qualifiedName())
    Table(tableName)
  }

  override def visitQualifiedName(ctx: QualifiedNameContext): QName = {
    QName(ctx.identifier().asScala.map(_.getText).toSeq)
  }

  override def visitSelectAll(ctx: SelectAllContext): SelectItem = {
    // TODO parse qName
    ctx.qualifiedName()
    AllColumns(None)
  }

  override def visitSelectSingle(ctx: SelectSingleContext): SelectItem = {
    SingleColumn(expression(ctx.expression()), None)
  }

  override def visitExpression(ctx: ExpressionContext): SQLModel = {
    trace(s"expr: ${print(ctx)}")
    visit(ctx.booleanExpression())
  }

  override def visitLogicalNot(ctx: LogicalNotContext): SQLModel = {
    Not(expression(ctx.booleanExpression()))
  }

  def expression(ctx: ParserRuleContext): Expression = {
    ctx.accept(this).asInstanceOf[Expression]
  }

  override def visitPredicated(ctx: PredicatedContext): Expression = {
    val e = expression(ctx.valueExpression)
    if (ctx.predicate != null) {
      // TODO add predicate
      ctx.predicate() match {
        case n: NullPredicateContext =>
          if (n.NOT() == null) IsNull(e) else IsNotNull(e)
        case other =>
          // TODO
          warn(s"unhandled predicate: ${print(ctx.predicate())}")
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

  override def visitUnquotedIdentifier(ctx: UnquotedIdentifierContext): SQLModel = {
    val id = Option(ctx.nonReserved()).map(_.getText).getOrElse(ctx.getText)
    QName(id)
  }
  override def visitBackQuotedIdentifier(ctx: BackQuotedIdentifierContext): SQLModel = {
    QName(ctx.getText.replaceAll("(^`|`$)", ""))
  }
  override def visitQuotedIdentifier(ctx: QuotedIdentifierContext): SQLModel = {
    QName(ctx.getText.replaceAll("(^\"|\"$)", ""))
  }
  override def visitDigitIdentifier(ctx: DigitIdentifierContext): SQLModel = {
    DigitId(ctx.getText.toInt)
  }
}
