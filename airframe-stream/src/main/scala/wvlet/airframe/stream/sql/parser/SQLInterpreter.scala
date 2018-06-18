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
import wvlet.airframe.stream.spi.SQL
import wvlet.airframe.stream.spi.SQL._
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
    visit(ctx.queryNoWith())
  }

  override def visitQueryNoWith(ctx: QueryNoWithContext): SQLModel = {
    val term = visit(ctx.queryTerm())
    // TODO
    term
  }
  override def visitQueryTermDefault(ctx: QueryTermDefaultContext): SQLModel = {
    visit(ctx.queryPrimary())
  }

  override def visitQueryPrimaryDefault(ctx: QueryPrimaryDefaultContext): SQLModel = {
    visit(ctx.querySpecification())
  }

  override def visitQuerySpecification(ctx: QuerySpecificationContext): SQLModel = {
    val selectItem = ctx.selectItem().asScala.map { x =>
      visit(x).asInstanceOf[SelectItem]
    }
    val r = ctx.relation().asScala.map { x =>
      visit(x).asInstanceOf[Relation]
    }
    val w =
      Option(ctx.where)
        .map(visit(_))
        .collectFirst { case e: Expression => e }

    val q =
      Query(item = selectItem,
            isDistinct = false,
            from = r.headOption,
            where = w,
            groupBy = Seq.empty,
            having = None,
            orderBy = Seq.empty,
            limit = None)
    q
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
    QName(ctx.identifier().asScala.map(_.getText))
  }

  override def visitSelectAll(ctx: SelectAllContext): SelectItem = {
    // TODO parse qName
    ctx.qualifiedName()
    AllColumns(None)
  }

  override def visitSelectSingle(ctx: SelectSingleContext): SelectItem = {
    SingleColumn(visitExpression(ctx.expression()), None)
  }

  override def visitExpression(ctx: ExpressionContext): SQL.Expression = {
    debug(s"expr: ${print(ctx)}")
    null
  }

  def expression(ctx: ParserRuleContext): Expression = {
    ctx.accept(this).asInstanceOf[Expression]
  }

  override def visitPredicated(ctx: PredicatedContext): Expression = {
    val e = expression(ctx.valueExpression)
    if (ctx.predicate != null) {
      // TODO add predicate
      e
    } else {
      e
    }
  }
  override def visitComparison(ctx: ComparisonContext): Expression = {
    debug(s"comparison: ${print(ctx)}")
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

  override def visitNumericLiteral(ctx: NumericLiteralContext): SQLModel = {
    visit(ctx.number())
  }

  override def visitIntegerLiteral(ctx: IntegerLiteralContext): SQLModel = {
    SQL.LongLiteral(ctx.getText.toInt)
  }
  override def visitUnquotedIdentifier(ctx: UnquotedIdentifierContext): SQLModel = {
    QName(ctx.nonReserved().getText)
  }

}
