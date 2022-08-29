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

import wvlet.airframe.sql.model.LogicalPlan
import wvlet.airframe.sql.model.LogicalPlan.{Query, With}
import wvlet.log.LogSupport

object CTEResolver extends LogSupport {

  def resolveCTE(analyzerContext: AnalyzerContext, p: LogicalPlan): LogicalPlan = {
    p.transform { case q @ Query(With(_, queryDefs), body) =>
      queryDefs.map { x =>
        val pair = x.name.value -> x.query
        info(x.query.resolved)
        info(x.query.outputAttributes)
      }
      q
    }
  }

}
