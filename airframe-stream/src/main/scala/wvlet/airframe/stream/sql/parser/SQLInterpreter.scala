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
    debug(s"interpret: ${print(ctx)}")
    val m = ctx.accept(this)
    info(m)
    m
  }

  override def visitSingleStatement(ctx: SingleStatementContext): SQLModel = {
    visit(ctx.statement())
  }

  override def visitStatementDefault(ctx: StatementDefaultContext): SQLModel = {
    visit(ctx.query())
  }

  override def visitQuery(ctx: QueryContext): SQLModel = {
    debug(s"query: ${print(ctx)}")
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
    debug(s"select item: ${selectItem}")
    val r = ctx.relation().asScala.map { x =>
      visit(x).asInstanceOf[Relation]
    }
    val q =
      Query(item = selectItem,
            isDistinct = false,
            from = None,
            where = None,
            groupBy = Seq.empty,
            having = None,
            orderBy = Seq.empty,
            limit = None)

    q
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
}
