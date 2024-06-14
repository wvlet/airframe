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
package wvlet.airspec.runner

import wvlet.log.LogSupport

import scala.util.matching.Regex

/**
  * Find test specs matching the pattern
  */
class AirSpecMatcher(pattern: String) extends LogSupport {

  private val regexPatterns: Seq[Regex] = pattern
    .split("/")
    .map(_.replaceAll("\\*", ".*"))
    .map { x =>
      try {
        s"(?i)${x}".r
      } catch {
        case e: Throwable =>
          throw new IllegalArgumentException(s"Invalid regex pattern: ${x}")
      }
    }
    .toSeq

  /**
    * '/ (slash)'-separated task names
    * @param taskName
    * @return
    */
  def matchWith(taskName: String): Boolean = matchWith(taskName.split("/").toSeq)

  /**
    * @param taskName
    *   task name (from parent to the current test name)
    * @return
    *   true if the specName matches with the pre-defined pattern
    */
  def matchWith(taskName: Seq[String]): Boolean = {
    taskName.zipAll(regexPatterns, "", "(?i).*".r).forall { case (part, regex) =>
      if part.isEmpty then {
        true
      } else {
        val hasMatch = regex.findFirstIn(part).isDefined
        hasMatch
      }
    }
  }
}

object AirSpecMatcher {
  def all: AirSpecMatcher = new AirSpecMatcher("*")
}
