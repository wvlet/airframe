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
package wvlet.frame


object Examples {

  case class A(
    b:Boolean,
    bt:Byte,
    st:Short,
    i:Int,
    l:Long,
    f:Float,
    d:Double,
    str:String
  )

  case class B(a:A)

  type MyA = A

  trait C

}

import Examples._
/**
  *
  */
class FrameTest extends FrameSpec {

  "Frame" should {
    "resolve types" in {
      val a = Frame.of[A]
      info(a)

      val b = Frame.of[B]
      info(b)
    }

    "resolve alias" in {
      val alias = Frame.of[MyA]
      info(alias)
    }

    "resolve trait" in {
      val t = Frame.of[C]
      info(t)
    }

    "resolve array types" in {
      val a = Frame.of[Array[Int]]
      info(a)
      val b = Frame.of[Array[Byte]]
      info(b)
      val g = Frame.of[Array[A]]
      info(g)
    }

    "resolve option types" in {
      val o = Frame.of[Option[A]]
      info(o)
    }

    "resolve Collection types" in {
      val s = Frame.of[Seq[A]]
      info(s)

      val l = Frame.of[List[A]]
      info(l)

      val m = Frame.of[Map[String, A]]
      info(m)

      val set = Frame.of[Set[String]]
      info(set)

      val is = Frame.of[IndexedSeq[A]]
      info(is)
    }



  }
}
