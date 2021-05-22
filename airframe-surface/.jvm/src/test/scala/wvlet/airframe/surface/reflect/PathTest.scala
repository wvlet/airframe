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
package wvlet.airframe.surface.reflect

import wvlet.airframe.surface.SurfaceSpec

/**
  */
class PathTest extends SurfaceSpec {
  import Path._

  test("define root") {
    val r = Path.root
    assert(r.size == 0)
    assert(r.fullPath == "/")
  }

  test("define absolute paths") {
    val p = Path("/abs/path")
    assert(p.size == 2)
    assert(p.name == "path")
    assert(p.isRelative == false)
    assert(p.isAbsolute == true)
    assert(p.fullPath == "/abs/path")
    val pp = p.parent
    assert(pp.isDefined)
    pp.map { pp => assert(pp.fullPath == "/abs") }

    val p2 = Path("/abs/path")
    assert(p == p2)

    val p3 = p / "hello"
    assert(p3.fullPath == "/abs/path/hello")
  }

  test("define relative path") {
    val p = Path("hello/world")
    assert(p.fullPath == "hello/world")
    assert(p.isRelative == true)
    assert(p.isAbsolute == false)
  }

  test("be comparable") {
    assert(Path("/abs/path") == Path("/abs/path"))
    assert(Path("/a") != Path("/b"))

    assert(Path("a") == Path("a"))
    assert(Path("b/c") == Path("b/c"))
    assert(Path("b/c") != Path("a/c"))
  }
}
