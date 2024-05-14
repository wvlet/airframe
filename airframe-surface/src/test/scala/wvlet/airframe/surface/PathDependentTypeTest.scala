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
package wvlet.airframe.surface

import wvlet.airspec.AirSpec

// extracted from wvlet.airframe.di.PathDependentTypeTest

class PathDependentTypeTest extends AirSpec {
  test("pass dependent types") {
    import PathDependentType.*
    val s = Surface.of[MyProfile#Backend#Database]
    assert(s.name == "Database")
    assert(s.toString == "Database:=DatabaseDef")
  }
}

object PathDependentType {
  object MyBackend extends MyBackend

  class MyService(val p: MyProfile#Backend#Database)

  trait MyProfile {
    type Backend = MyBackend
  }

  trait MyBackend {
    type Database = DatabaseDef
    class DatabaseDef {
      def hello = "hello my"
    }
  }
}
