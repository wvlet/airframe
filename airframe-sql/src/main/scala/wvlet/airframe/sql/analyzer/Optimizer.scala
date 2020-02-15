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
import wvlet.airframe.sql.analyzer.SQLAnalyzer.{PlanRewriter, Rule}
import wvlet.airframe.sql.model.LogicalPlan.{Project, Relation}
import wvlet.airframe.sql.model.{Attribute, Expression, TableScan}
import wvlet.log.LogSupport

/**
  *
  */
object Optimizer extends LogSupport {

  val optimizerRules: List[Rule] = {
    Optimizer.pruneColumns _ ::
      Nil
  }

  def extractInputs(expressions: Seq[Expression]): Set[Attribute] = {
    val newAttributes: Seq[Attribute] = expressions.collect {
      case s: Attribute =>
        s
    }
    newAttributes.toSet
  }

  def pruneColumns(context: AnalyzerContext): PlanRewriter = {
    case p @ Project(child, selectItems) =>
      val newContext = context.withAttributes(selectItems)
      Project(pruneRelationColumns(child, newContext), selectItems)
    case r: Relation =>
      pruneRelationColumns(r, context.withAttributes(r.outputAttributes))
  }

  def pruneRelationColumns(relation: Relation, context: AnalyzerContext): Relation = {
    relation match {
      case t @ TableScan(name, table, columns) =>
        val accessedColumns = columns.filter { col => context.parentAttributes.exists(x => x.name == col) }
        TableScan(name, table, accessedColumns)
      case _ => relation
    }
  }
}
