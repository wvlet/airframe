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

import wvlet.airframe.stream.spi.SQL.SQLModel

/**
  * ANTLR parse tree -> SQL model classes
  */
class SQLInterpreter extends SqlBaseBaseVisitor[SQLModel] {

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSingleStatement(ctx: SqlBaseParser.SingleStatementContext): SQLModel =
    super
      .visitSingleStatement(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSingleExpression(ctx: SqlBaseParser.SingleExpressionContext): SQLModel =
    super
      .visitSingleExpression(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitStatementDefault(ctx: SqlBaseParser.StatementDefaultContext): SQLModel =
    super
      .visitStatementDefault(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitUse(ctx: SqlBaseParser.UseContext): SQLModel =
    super
      .visitUse(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCreateSchema(ctx: SqlBaseParser.CreateSchemaContext): SQLModel =
    super
      .visitCreateSchema(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDropSchema(ctx: SqlBaseParser.DropSchemaContext): SQLModel =
    super
      .visitDropSchema(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitRenameSchema(ctx: SqlBaseParser.RenameSchemaContext): SQLModel =
    super
      .visitRenameSchema(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCreateTableAsSelect(ctx: SqlBaseParser.CreateTableAsSelectContext): SQLModel =
    super
      .visitCreateTableAsSelect(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCreateTable(ctx: SqlBaseParser.CreateTableContext): SQLModel =
    super
      .visitCreateTable(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDropTable(ctx: SqlBaseParser.DropTableContext): SQLModel =
    super
      .visitDropTable(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitInsertInto(ctx: SqlBaseParser.InsertIntoContext): SQLModel =
    super
      .visitInsertInto(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDelete(ctx: SqlBaseParser.DeleteContext): SQLModel =
    super
      .visitDelete(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitRenameTable(ctx: SqlBaseParser.RenameTableContext): SQLModel =
    super
      .visitRenameTable(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitRenameColumn(ctx: SqlBaseParser.RenameColumnContext): SQLModel =
    super
      .visitRenameColumn(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDropColumn(ctx: SqlBaseParser.DropColumnContext): SQLModel =
    super
      .visitDropColumn(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitAddColumn(ctx: SqlBaseParser.AddColumnContext): SQLModel =
    super
      .visitAddColumn(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCreateView(ctx: SqlBaseParser.CreateViewContext): SQLModel =
    super
      .visitCreateView(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDropView(ctx: SqlBaseParser.DropViewContext): SQLModel =
    super
      .visitDropView(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCall(ctx: SqlBaseParser.CallContext): SQLModel =
    super
      .visitCall(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitGrant(ctx: SqlBaseParser.GrantContext): SQLModel =
    super
      .visitGrant(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitRevoke(ctx: SqlBaseParser.RevokeContext): SQLModel =
    super
      .visitRevoke(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowGrants(ctx: SqlBaseParser.ShowGrantsContext): SQLModel =
    super
      .visitShowGrants(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitExplain(ctx: SqlBaseParser.ExplainContext): SQLModel =
    super
      .visitExplain(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowCreateTable(ctx: SqlBaseParser.ShowCreateTableContext): SQLModel =
    super
      .visitShowCreateTable(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowCreateView(ctx: SqlBaseParser.ShowCreateViewContext): SQLModel =
    super
      .visitShowCreateView(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowTables(ctx: SqlBaseParser.ShowTablesContext): SQLModel =
    super
      .visitShowTables(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowSchemas(ctx: SqlBaseParser.ShowSchemasContext): SQLModel =
    super
      .visitShowSchemas(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowCatalogs(ctx: SqlBaseParser.ShowCatalogsContext): SQLModel =
    super
      .visitShowCatalogs(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowColumns(ctx: SqlBaseParser.ShowColumnsContext): SQLModel =
    super
      .visitShowColumns(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowStats(ctx: SqlBaseParser.ShowStatsContext): SQLModel =
    super
      .visitShowStats(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowStatsForQuery(ctx: SqlBaseParser.ShowStatsForQueryContext): SQLModel =
    super
      .visitShowStatsForQuery(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowFunctions(ctx: SqlBaseParser.ShowFunctionsContext): SQLModel =
    super
      .visitShowFunctions(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowSession(ctx: SqlBaseParser.ShowSessionContext): SQLModel =
    super
      .visitShowSession(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSetSession(ctx: SqlBaseParser.SetSessionContext): SQLModel =
    super
      .visitSetSession(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitResetSession(ctx: SqlBaseParser.ResetSessionContext): SQLModel =
    super
      .visitResetSession(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitStartTransaction(ctx: SqlBaseParser.StartTransactionContext): SQLModel =
    super
      .visitStartTransaction(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCommit(ctx: SqlBaseParser.CommitContext): SQLModel =
    super
      .visitCommit(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitRollback(ctx: SqlBaseParser.RollbackContext): SQLModel =
    super
      .visitRollback(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitShowPartitions(ctx: SqlBaseParser.ShowPartitionsContext): SQLModel =
    super
      .visitShowPartitions(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitPrepare(ctx: SqlBaseParser.PrepareContext): SQLModel =
    super
      .visitPrepare(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDeallocate(ctx: SqlBaseParser.DeallocateContext): SQLModel =
    super
      .visitDeallocate(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitExecute(ctx: SqlBaseParser.ExecuteContext): SQLModel =
    super
      .visitExecute(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDescribeInput(ctx: SqlBaseParser.DescribeInputContext): SQLModel =
    super
      .visitDescribeInput(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDescribeOutput(ctx: SqlBaseParser.DescribeOutputContext): SQLModel =
    super
      .visitDescribeOutput(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitQuery(ctx: SqlBaseParser.QueryContext): SQLModel =
    super
      .visitQuery(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitWith(ctx: SqlBaseParser.WithContext): SQLModel =
    super
      .visitWith(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitTableElement(ctx: SqlBaseParser.TableElementContext): SQLModel =
    super
      .visitTableElement(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitColumnDefinition(ctx: SqlBaseParser.ColumnDefinitionContext): SQLModel =
    super
      .visitColumnDefinition(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitLikeClause(ctx: SqlBaseParser.LikeClauseContext): SQLModel =
    super
      .visitLikeClause(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitProperties(ctx: SqlBaseParser.PropertiesContext): SQLModel =
    super
      .visitProperties(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitProperty(ctx: SqlBaseParser.PropertyContext): SQLModel =
    super
      .visitProperty(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitQueryNoWith(ctx: SqlBaseParser.QueryNoWithContext): SQLModel =
    super
      .visitQueryNoWith(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitQueryTermDefault(ctx: SqlBaseParser.QueryTermDefaultContext): SQLModel =
    super
      .visitQueryTermDefault(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSetOperation(ctx: SqlBaseParser.SetOperationContext): SQLModel =
    super
      .visitSetOperation(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitQueryPrimaryDefault(ctx: SqlBaseParser.QueryPrimaryDefaultContext): SQLModel =
    super
      .visitQueryPrimaryDefault(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitTable(ctx: SqlBaseParser.TableContext): SQLModel =
    super
      .visitTable(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitInlineTable(ctx: SqlBaseParser.InlineTableContext): SQLModel =
    super
      .visitInlineTable(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSubquery(ctx: SqlBaseParser.SubqueryContext): SQLModel =
    super
      .visitSubquery(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSortItem(ctx: SqlBaseParser.SortItemContext): SQLModel =
    super
      .visitSortItem(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitQuerySpecification(ctx: SqlBaseParser.QuerySpecificationContext): SQLModel =
    super
      .visitQuerySpecification(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitGroupBy(ctx: SqlBaseParser.GroupByContext): SQLModel =
    super
      .visitGroupBy(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSingleGroupingSet(ctx: SqlBaseParser.SingleGroupingSetContext): SQLModel =
    super
      .visitSingleGroupingSet(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitRollup(ctx: SqlBaseParser.RollupContext): SQLModel =
    super
      .visitRollup(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCube(ctx: SqlBaseParser.CubeContext): SQLModel =
    super
      .visitCube(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitMultipleGroupingSets(ctx: SqlBaseParser.MultipleGroupingSetsContext): SQLModel =
    super
      .visitMultipleGroupingSets(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitGroupingExpressions(ctx: SqlBaseParser.GroupingExpressionsContext): SQLModel =
    super
      .visitGroupingExpressions(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitGroupingSet(ctx: SqlBaseParser.GroupingSetContext): SQLModel =
    super
      .visitGroupingSet(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitNamedQuery(ctx: SqlBaseParser.NamedQueryContext): SQLModel =
    super
      .visitNamedQuery(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSetQuantifier(ctx: SqlBaseParser.SetQuantifierContext): SQLModel =
    super
      .visitSetQuantifier(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSelectSingle(ctx: SqlBaseParser.SelectSingleContext): SQLModel =
    super
      .visitSelectSingle(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSelectAll(ctx: SqlBaseParser.SelectAllContext): SQLModel =
    super
      .visitSelectAll(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitRelationDefault(ctx: SqlBaseParser.RelationDefaultContext): SQLModel =
    super
      .visitRelationDefault(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitJoinRelation(ctx: SqlBaseParser.JoinRelationContext): SQLModel =
    super
      .visitJoinRelation(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitJoinType(ctx: SqlBaseParser.JoinTypeContext): SQLModel =
    super
      .visitJoinType(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitJoinCriteria(ctx: SqlBaseParser.JoinCriteriaContext): SQLModel =
    super
      .visitJoinCriteria(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSampledRelation(ctx: SqlBaseParser.SampledRelationContext): SQLModel =
    super
      .visitSampledRelation(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSampleType(ctx: SqlBaseParser.SampleTypeContext): SQLModel =
    super
      .visitSampleType(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitAliasedRelation(ctx: SqlBaseParser.AliasedRelationContext): SQLModel =
    super
      .visitAliasedRelation(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitColumnAliases(ctx: SqlBaseParser.ColumnAliasesContext): SQLModel =
    super
      .visitColumnAliases(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitTableName(ctx: SqlBaseParser.TableNameContext): SQLModel =
    super
      .visitTableName(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSubqueryRelation(ctx: SqlBaseParser.SubqueryRelationContext): SQLModel =
    super
      .visitSubqueryRelation(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitUnnest(ctx: SqlBaseParser.UnnestContext): SQLModel =
    super
      .visitUnnest(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitLateral(ctx: SqlBaseParser.LateralContext): SQLModel =
    super
      .visitLateral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitParenthesizedRelation(ctx: SqlBaseParser.ParenthesizedRelationContext): SQLModel =
    super
      .visitParenthesizedRelation(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitExpression(ctx: SqlBaseParser.ExpressionContext): SQLModel =
    super
      .visitExpression(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitLogicalNot(ctx: SqlBaseParser.LogicalNotContext): SQLModel =
    super
      .visitLogicalNot(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitPredicated(ctx: SqlBaseParser.PredicatedContext): SQLModel =
    super
      .visitPredicated(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitLogicalBinary(ctx: SqlBaseParser.LogicalBinaryContext): SQLModel =
    super
      .visitLogicalBinary(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitComparison(ctx: SqlBaseParser.ComparisonContext): SQLModel =
    super
      .visitComparison(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitQuantifiedComparison(ctx: SqlBaseParser.QuantifiedComparisonContext): SQLModel =
    super
      .visitQuantifiedComparison(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitBetween(ctx: SqlBaseParser.BetweenContext): SQLModel =
    super
      .visitBetween(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitInList(ctx: SqlBaseParser.InListContext): SQLModel =
    super
      .visitInList(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitInSubquery(ctx: SqlBaseParser.InSubqueryContext): SQLModel =
    super
      .visitInSubquery(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitLike(ctx: SqlBaseParser.LikeContext): SQLModel =
    super
      .visitLike(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitNullPredicate(ctx: SqlBaseParser.NullPredicateContext): SQLModel =
    super
      .visitNullPredicate(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDistinctFrom(ctx: SqlBaseParser.DistinctFromContext): SQLModel =
    super
      .visitDistinctFrom(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitValueExpressionDefault(ctx: SqlBaseParser.ValueExpressionDefaultContext): SQLModel =
    super
      .visitValueExpressionDefault(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitConcatenation(ctx: SqlBaseParser.ConcatenationContext): SQLModel =
    super
      .visitConcatenation(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitArithmeticBinary(ctx: SqlBaseParser.ArithmeticBinaryContext): SQLModel =
    super
      .visitArithmeticBinary(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitArithmeticUnary(ctx: SqlBaseParser.ArithmeticUnaryContext): SQLModel =
    super
      .visitArithmeticUnary(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitAtTimeZone(ctx: SqlBaseParser.AtTimeZoneContext): SQLModel =
    super
      .visitAtTimeZone(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDereference(ctx: SqlBaseParser.DereferenceContext): SQLModel =
    super
      .visitDereference(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitTypeConstructor(ctx: SqlBaseParser.TypeConstructorContext): SQLModel =
    super
      .visitTypeConstructor(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSpecialDateTimeFunction(ctx: SqlBaseParser.SpecialDateTimeFunctionContext): SQLModel =
    super
      .visitSpecialDateTimeFunction(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSubstring(ctx: SqlBaseParser.SubstringContext): SQLModel =
    super
      .visitSubstring(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCast(ctx: SqlBaseParser.CastContext): SQLModel =
    super
      .visitCast(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitLambda(ctx: SqlBaseParser.LambdaContext): SQLModel =
    super
      .visitLambda(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitParenthesizedExpression(ctx: SqlBaseParser.ParenthesizedExpressionContext): SQLModel =
    super
      .visitParenthesizedExpression(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitParameter(ctx: SqlBaseParser.ParameterContext): SQLModel =
    super
      .visitParameter(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitNormalize(ctx: SqlBaseParser.NormalizeContext): SQLModel =
    super
      .visitNormalize(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitIntervalLiteral(ctx: SqlBaseParser.IntervalLiteralContext): SQLModel =
    super
      .visitIntervalLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitNumericLiteral(ctx: SqlBaseParser.NumericLiteralContext): SQLModel =
    super
      .visitNumericLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitBooleanLiteral(ctx: SqlBaseParser.BooleanLiteralContext): SQLModel =
    super
      .visitBooleanLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSimpleCase(ctx: SqlBaseParser.SimpleCaseContext): SQLModel =
    super
      .visitSimpleCase(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitColumnReference(ctx: SqlBaseParser.ColumnReferenceContext): SQLModel =
    super
      .visitColumnReference(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitNullLiteral(ctx: SqlBaseParser.NullLiteralContext): SQLModel =
    super
      .visitNullLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitRowConstructor(ctx: SqlBaseParser.RowConstructorContext): SQLModel =
    super
      .visitRowConstructor(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSubscript(ctx: SqlBaseParser.SubscriptContext): SQLModel =
    super
      .visitSubscript(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSubqueryExpression(ctx: SqlBaseParser.SubqueryExpressionContext): SQLModel =
    super
      .visitSubqueryExpression(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitBinaryLiteral(ctx: SqlBaseParser.BinaryLiteralContext): SQLModel =
    super
      .visitBinaryLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCurrentUser(ctx: SqlBaseParser.CurrentUserContext): SQLModel =
    super
      .visitCurrentUser(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitExtract(ctx: SqlBaseParser.ExtractContext): SQLModel =
    super
      .visitExtract(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitStringLiteral(ctx: SqlBaseParser.StringLiteralContext): SQLModel =
    super
      .visitStringLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitArrayConstructor(ctx: SqlBaseParser.ArrayConstructorContext): SQLModel =
    super
      .visitArrayConstructor(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitFunctionCall(ctx: SqlBaseParser.FunctionCallContext): SQLModel =
    super
      .visitFunctionCall(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitExists(ctx: SqlBaseParser.ExistsContext): SQLModel =
    super
      .visitExists(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitPosition(ctx: SqlBaseParser.PositionContext): SQLModel =
    super
      .visitPosition(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSearchedCase(ctx: SqlBaseParser.SearchedCaseContext): SQLModel =
    super
      .visitSearchedCase(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitGroupingOperation(ctx: SqlBaseParser.GroupingOperationContext): SQLModel =
    super
      .visitGroupingOperation(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitBasicStringLiteral(ctx: SqlBaseParser.BasicStringLiteralContext): SQLModel =
    super
      .visitBasicStringLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitUnicodeStringLiteral(ctx: SqlBaseParser.UnicodeStringLiteralContext): SQLModel =
    super
      .visitUnicodeStringLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitTimeZoneInterval(ctx: SqlBaseParser.TimeZoneIntervalContext): SQLModel =
    super
      .visitTimeZoneInterval(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitTimeZoneString(ctx: SqlBaseParser.TimeZoneStringContext): SQLModel =
    super
      .visitTimeZoneString(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitComparisonOperator(ctx: SqlBaseParser.ComparisonOperatorContext): SQLModel =
    super
      .visitComparisonOperator(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitComparisonQuantifier(ctx: SqlBaseParser.ComparisonQuantifierContext): SQLModel =
    super
      .visitComparisonQuantifier(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitBooleanValue(ctx: SqlBaseParser.BooleanValueContext): SQLModel =
    super
      .visitBooleanValue(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitInterval(ctx: SqlBaseParser.IntervalContext): SQLModel =
    super
      .visitInterval(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitIntervalField(ctx: SqlBaseParser.IntervalFieldContext): SQLModel =
    super
      .visitIntervalField(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitNormalForm(ctx: SqlBaseParser.NormalFormContext): SQLModel =
    super
      .visitNormalForm(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitType(ctx: SqlBaseParser.TypeContext): SQLModel =
    super
      .visitType(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitTypeParameter(ctx: SqlBaseParser.TypeParameterContext): SQLModel =
    super
      .visitTypeParameter(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitBaseType(ctx: SqlBaseParser.BaseTypeContext): SQLModel =
    super
      .visitBaseType(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitWhenClause(ctx: SqlBaseParser.WhenClauseContext): SQLModel =
    super
      .visitWhenClause(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitFilter(ctx: SqlBaseParser.FilterContext): SQLModel =
    super
      .visitFilter(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitOver(ctx: SqlBaseParser.OverContext): SQLModel =
    super
      .visitOver(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitWindowFrame(ctx: SqlBaseParser.WindowFrameContext): SQLModel =
    super
      .visitWindowFrame(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitUnboundedFrame(ctx: SqlBaseParser.UnboundedFrameContext): SQLModel =
    super
      .visitUnboundedFrame(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitCurrentRowBound(ctx: SqlBaseParser.CurrentRowBoundContext): SQLModel =
    super
      .visitCurrentRowBound(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitBoundedFrame(ctx: SqlBaseParser.BoundedFrameContext): SQLModel =
    super
      .visitBoundedFrame(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitExplainFormat(ctx: SqlBaseParser.ExplainFormatContext): SQLModel =
    super
      .visitExplainFormat(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitExplainType(ctx: SqlBaseParser.ExplainTypeContext): SQLModel =
    super
      .visitExplainType(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitIsolationLevel(ctx: SqlBaseParser.IsolationLevelContext): SQLModel =
    super
      .visitIsolationLevel(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitTransactionAccessMode(ctx: SqlBaseParser.TransactionAccessModeContext): SQLModel =
    super
      .visitTransactionAccessMode(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitReadUncommitted(ctx: SqlBaseParser.ReadUncommittedContext): SQLModel =
    super
      .visitReadUncommitted(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitReadCommitted(ctx: SqlBaseParser.ReadCommittedContext): SQLModel =
    super
      .visitReadCommitted(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitRepeatableRead(ctx: SqlBaseParser.RepeatableReadContext): SQLModel =
    super
      .visitRepeatableRead(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitSerializable(ctx: SqlBaseParser.SerializableContext): SQLModel =
    super
      .visitSerializable(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitPositionalArgument(ctx: SqlBaseParser.PositionalArgumentContext): SQLModel =
    super
      .visitPositionalArgument(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitNamedArgument(ctx: SqlBaseParser.NamedArgumentContext): SQLModel =
    super
      .visitNamedArgument(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitPrivilege(ctx: SqlBaseParser.PrivilegeContext): SQLModel =
    super
      .visitPrivilege(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitQualifiedName(ctx: SqlBaseParser.QualifiedNameContext): SQLModel =
    super
      .visitQualifiedName(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitUnquotedIdentifier(ctx: SqlBaseParser.UnquotedIdentifierContext): SQLModel =
    super
      .visitUnquotedIdentifier(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitQuotedIdentifier(ctx: SqlBaseParser.QuotedIdentifierContext): SQLModel =
    super
      .visitQuotedIdentifier(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitBackQuotedIdentifier(ctx: SqlBaseParser.BackQuotedIdentifierContext): SQLModel =
    super
      .visitBackQuotedIdentifier(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDigitIdentifier(ctx: SqlBaseParser.DigitIdentifierContext): SQLModel =
    super
      .visitDigitIdentifier(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDecimalLiteral(ctx: SqlBaseParser.DecimalLiteralContext): SQLModel =
    super
      .visitDecimalLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitDoubleLiteral(ctx: SqlBaseParser.DoubleLiteralContext): SQLModel =
    super
      .visitDoubleLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitIntegerLiteral(ctx: SqlBaseParser.IntegerLiteralContext): SQLModel =
    super
      .visitIntegerLiteral(ctx)

  /**
    * {@inheritDoc }
    *
    * <p>The default implementation returns the result of calling
    * {@link #visitChildren} on {@code ctx}.</p>
    */
  override def visitNonReserved(ctx: SqlBaseParser.NonReservedContext): SQLModel =
    super
      .visitNonReserved(ctx)
}
