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

/**
  * Find specs matching the pattern
  */
class AirSpecMatcher(pattern: String) extends LogSupport {

  private val regexPatterns = pattern
    .split("/")
    .map(_.replaceAll("\\*", ".*"))
    .map(x => s"(?i)${x}".r)
    .toSeq

  def matchWith(specName: String): Boolean = matchWith(specName.split("/").toSeq)

  /**
    * @param specName
    *   a list of test spec names
    * @return
    */
  def matchWith(specName: Seq[String]): Boolean = {

    specName.zipAll(regexPatterns, "", "(?i).*".r).forall { case (part, regex) =>
      if (part.isEmpty) {
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
