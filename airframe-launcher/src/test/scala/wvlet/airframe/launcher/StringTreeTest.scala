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

//--------------------------------------
//
// ValueHolderTest.scala
// Since: 2012/11/06 5:41 PM
//
//--------------------------------------

package wvlet.airframe.launcher

import wvlet.airspec.AirSpec

class StringTreeTest extends AirSpec {
  test("have nested values") {
    val h  = StringTree.empty
    val h2 = h.set("a", "apple")
    debug(h2)
    val h3 = h2.set("B/b", "book")
    val h4 = h3.set("B/c", "cookie")
    debug(h4)
    val h5 = h4.set("B/c", "cake")
    debug(h5)
    val h6 = h5.set("B/b", "magazine")
    debug(h6)
    val h7 = h6.set("B/b/note", "my bookshelf")
    debug(h7)

    val b = h7.get("B")
    debug(b)
    val n = h7.get("B/b")
    debug(n)
  }
}
