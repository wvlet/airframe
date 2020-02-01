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
package wvlet.airspec.spi

import scala.scalajs.js

/**
  *
  */
private[airspec] object JsObjectMatcher {

  import Asserts._

  def matcher: PartialFunction[(Any, Any), TestResult] = {
    case (a: js.Object, b: js.Object) =>
      check(jsObjEquals(a, b))
  }

  def jsObjEquals(v1: js.Object, v2: js.Object): Boolean = {
    if (v1 == v2) {
      true
    } else if (v1 == null || v2 == null) {
      false
    } else {
      deepEqual(v1, v2)
    }
  }

  @inline
  private def deepEqual(v1: js.Object, v2: js.Object): Boolean = {
    val k1 = js.Object.keys(v1)
    val k2 = js.Object.keys(v2)

    if (k1.length != k2.length) {
      false
    } else if (k1.length == 0) {
      js.JSON.stringify(v1) == js.JSON.stringify(v2)
    } else {
      // TODO: Wait until js.Object.entries will be available https://github.com/scala-js/scala-js/pull/3869
      v1 == v2
    }
//    else {
//      v1.
//      val values1 = v1.entries1.sortBy(_._1).map(p => (p._1, p._2.asInstanceOf[js.Any]))
//      val values2 = entries2.sortBy(_._1).map(p => (p._1, p._2.asInstanceOf[js.Any]))
//      values1.zip(values2).forall {
//        case ((k1, _), (k2, _)) if k1 != k2 => false
//        case ((_, v1), (_, v2)) =>
//          if (js.typeOf(v1) == "object" && js.typeOf(v2) == "object")
//            this.apply(v1.asInstanceOf[js.Object], v2.asInstanceOf[js.Object])
//          else
//            v1 == v2
//      }
//    }
  }
}
