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
  */
object Optimizer extends LogSupport {

  val optimizerRules: List[Rule] = {
    Optimizer.pruneColumns _ ::
      Nil
  }

  def extractInputs(expressions: Seq[Expression]): Set[Attribute] = {
    val newAttributes: Seq[Attribute] = expressions.collect { case s: Attribute =>
      s
    }
    newAttributes.toSet
  }

  /**
    * Recursively prune columns that are not used for the final projection
    * @param context
    * @return
    */
  def pruneColumns(context: AnalyzerContext): PlanRewriter = {
    case p @ Project(child, selectItems, _) =>
      val newContext = context.withAttributes(selectItems)
      Project(pruneRelationColumns(child, newContext), selectItems, p.nodeLocation)
    case r: Relation =>
      pruneRelationColumns(r, context.withAttributes(r.outputAttributes))
  }

  /**
    * @param relation
    * @param context
    * @return
    */
  def pruneRelationColumns(relation: Relation, context: AnalyzerContext): Relation = {
    relation match {
      case t @ TableScan(table, columns, _) if context.parentAttributes.nonEmpty =>
        val parentAttributes = context.parentAttributes.get
        val accessedColumns  = columns.filter { col => parentAttributes.exists(x => x.name == col.name) }
        TableScan(table, accessedColumns, t.nodeLocation)
      case _ => relation
    }
  }
}
