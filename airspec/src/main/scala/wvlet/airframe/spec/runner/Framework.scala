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
package wvlet.airframe.spec.runner
import sbt.testing
import sbt.testing.{Fingerprint, SubclassFingerprint}
import wvlet.airframe.spec.runner.Framework.AirSpecFingerPrint
import wvlet.log.{LogSupport, Logger}

/**
  *
  */
class Framework extends sbt.testing.Framework with LogSupport {
  Logger.init
  info(s"Running AirSpec")

  override def name(): String                     = "airspec"
  override def fingerprints(): Array[Fingerprint] = Array(AirSpecFingerPrint)
  override def runner(args: Array[String], remoteArgs: Array[String], testClassLoader: ClassLoader): testing.Runner = {
    Runner.newRunner(args, remoteArgs, testClassLoader)
  }
}

object Framework {

  object AirSpecFingerPrint extends SubclassFingerprint {
    override def isModule: Boolean                  = true
    override def superclassName(): String           = "wvlet.airframe.spec.AirSpec"
    override def requireNoArgConstructor(): Boolean = true
  }
}
