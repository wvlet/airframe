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
package examples

import wvlet.airspec.AirSpec

/**
  *
  */
class CommonSpec extends AirSpec {
  scalaJsSupport

  def hello: Unit = {
    assert(true)
  }

  def failTest: Unit = {
    ignore()
    fail("test failure")
  }

  def exceptionTest: Unit = {
    ignore()
    throw new IllegalArgumentException("invalid argument")
  }
}

object ObjSpec extends AirSpec {
  scalaJsSupport

  def hello1: Unit = {
    assert(true)
  }
}
