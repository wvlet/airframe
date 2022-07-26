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
package wvlet.airframe.di
import wvlet.airframe.Design
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

/**
  */
class PathDependentTypeTest extends AirSpec {
  test("pass dependent types") {
    // .....,.
    import PathDependentType._
    val s = Surface.of[JdbcProfile#Backend#Database]

    val d = Design.newSilentDesign
      .bind[JdbcProfile#Backend#Database].toInstance(
        new PathDependentType.MyBackend.DatabaseDef().asInstanceOf[JdbcProfile#Backend#Database]
      )

    d.build[JdbcService] { s => s.p.hello shouldBe "hello jdbc" }
  }
}

object PathDependentType {
  object MyBackend extends JdbcBackend

  class JdbcService(val p: JdbcProfile#Backend#Database)

  trait JdbcProfile {
    type Backend = JdbcBackend
  }

  trait JdbcBackend {
    type Database = DatabaseDef
    class DatabaseDef {
      def hello = "hello jdbc"
    }
  }
}
