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

object PathDependentType {
  class MyService(p: MyProfile#Backend)
  class MyService2(db: MyProfile#Backend#Database)

  trait MyProfile {
    type Backend = MyBackend
  }

  trait MyBackend {
    type Database = DatabaseDef
  }

  class DatabaseDef {
    def hello = "hello my"
  }
}

class PathDependentTypeTest extends AirSpec {
  import PathDependentType.*

  test("pass dependent types") {
    val s = Surface.of[MyProfile#Backend]
    s.name shouldBe "Backend"
    s.toString shouldBe "Backend:=MyBackend"
  }

  test("nested path dependent types") {
    val s = Surface.of[MyProfile#Backend#Database]
    s.name shouldBe "Database"
    s.toString shouldBe "Database:=DatabaseDef"
  }
}
