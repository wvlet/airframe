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
package wvlet.airframe

import org.scalatest._
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogSupport, Logger}

import scala.language.implicitConversions

/**
  */
trait AirframeSpec
    extends WordSpec
    with Matchers
    with GivenWhenThen
    with BeforeAndAfter
    with BeforeAndAfterAll
    with LogSupport {
  // A tag for the current working test
  protected val working = Tag("working")
  // A tag for the failing test.
  protected val failing = Tag("failing")

  protected def inCI: Boolean = {
    sys.env.get("TRAVIS").map(_.toBoolean).getOrElse(false)
  }

  implicit def toTag(s: String) = Tag(s)
  override def run(testName: Option[String], args: Args): Status = {
    // Add source code location to the debug logs
    Logger.setDefaultFormatter(SourceCodeLogFormatter)

    // Periodically scan log level file
    Logger.scheduleLogLevelScan
    val s = super.run(testName, args)
    Logger.stopScheduledLogLevelScan
    s
  }
//  Temporarily removed for Scala 2.13.0-M4, which doesn't have parallel collection yet.
//  https://github.com/scala/scala-parallel-collections/issues/41
//
//  private[airframe] object CompatParColls {
//    val Converters = {
//      import Compat._
//
//      {
//        import scala.collection.parallel._
//
//        CollectionConverters
//      }
//    }
//
//    object Compat {
//      object CollectionConverters
//    }
//  }
}
