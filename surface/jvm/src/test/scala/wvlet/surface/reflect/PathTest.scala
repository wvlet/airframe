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
package wvlet.surface.reflect

import wvlet.surface.SurfaceSpec

/**
  *
  */
class PathTest extends SurfaceSpec {
  import Path._

  "Path" should {
    "define root" in {
      val r = Path.root
      r.size should be(0)
      r.fullPath should be("/")
    }

    "define absolute paths" in {
      val p = Path("/abs/path")
      p.size should be(2)
      p.name should be("path")
      p.isRelative should be(false)
      p.isAbsolute should be(true)
      p.fullPath should be("/abs/path")
      val pp = p.parent
      pp should be('defined)
      pp map { _.fullPath should be("/abs") }

      val p2 = Path("/abs/path")
      p should be(p2)

      val p3 = p / "hello"
      p3.fullPath should be("/abs/path/hello")
    }

    "define relative path" in {
      val p = Path("hello/world")
      p.fullPath should be("hello/world")
      p.isRelative should be(true)
      p.isAbsolute should be(false)
    }

    "be comparable" in {
      Path("/abs/path") should be(Path("/abs/path"))
      Path("/a") should not be (Path("/b"))

      Path("a") should be(Path("a"))
      Path("b/c") should be(Path("b/c"))
      Path("b/c") should not be (Path("a/c"))
    }
  }
}
