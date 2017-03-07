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

  def check(body: => Frame) {
    val frame = body
    info(s"[${frame.getClass.getSimpleName}] $frame")
  }

  "Frame" should {
    "resolve types" in {
      check(Frame.of[A])
      check(Frame.of[B])
    }

    "resolve alias" in {
      check(Frame.of[MyA])
    }

    "resolve trait" in {
      check(Frame.of[C])
    }

    "resolve array types" in {
      check(Frame.of[Array[Int]])
      check(Frame.of[Array[Byte]])
      check(Frame.of[Array[A]])
    }

    "resolve option types" in {
      check(Frame.of[Option[A]])
    }

    "resolve Collection types" in {
      check(Frame.of[Seq[A]])
      check(Frame.of[List[A]])
      check(Frame.of[Map[String, A]])
      check(Frame.of[Set[String]])
      check(Frame.of[IndexedSeq[A]])
    }

    "resolve mutable Collection types" in {
      check(Frame.of[collection.mutable.Seq[String]])
    }

  }
}
