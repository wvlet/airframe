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

/**
  */
object EnumTest {

  sealed trait Color
  case object Blue extends Color
  case object Red  extends Color

  object Color {
    def values: Seq[Color] = Seq(Blue, Red)
    def unapply(s: String): Option[Color] = {
      values.find(_.toString == s)
    }
  }
}

class EnumTest extends munit.FunSuite {
  import EnumTest._

  test("Find Surface.stringExtractor") {
    Surface.of[Color] match {
      case s: EnumSurface =>
        val f = s.stringExtractor
        assertEquals(f(classOf[Color], "Blue"), Some(Blue))
        assertEquals(f(classOf[Color], "Red"), Some(Red))
        assertEquals(f(classOf[Color], "White"), None)
      case other =>
        fail(s"EnumSurface should be used: ${other.getClass}")
    }
  }
}
