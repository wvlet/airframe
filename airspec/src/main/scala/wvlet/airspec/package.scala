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
package wvlet

/**
  */
package object airspec {
  // For Scala, Scala.js compatibility
  val compat: CompatApi = wvlet.airspec.Compat

  private[airspec] lazy val inCI = {
    sys.env.get("CI").map(_.toBoolean).getOrElse(false) || inTravisCI || inCircleCI || inGitHubAction
  }
  private[airspec] lazy val inTravisCI: Boolean = {
    sys.env.get("TRAVIS").map(_.toBoolean).getOrElse(false)
  }
  private[airspec] lazy val inCircleCI: Boolean = {
    sys.env.get("CIRCLECI").map(_.toBoolean).getOrElse(false)
  }
  private[airspec] lazy val inGitHubAction: Boolean = {
    sys.env.get("GITHUB_ACTION").isDefined
  }
}
