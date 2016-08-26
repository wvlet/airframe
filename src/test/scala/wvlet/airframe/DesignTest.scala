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
package wvlet.airframe

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import wvlet.obj.ObjectType
import wvlet.obj.tag.@@

trait Message
case class Hello(message: String) extends Message
trait Production

trait Development
/**
  *
  */
class DesignTest extends AirframeSpec {

  val d0 = Design.blanc
  val d1 =
    d0.bind[Hello].toInstance(Hello("world"))
    .bind[Message].toSingleton
    .bind[Message].toEagerSingleton
    .bind[Message].toSingletonOf[Hello]
    .bind[Message @@ Production].toInstance(Hello("production"))
    .bind[Message @@ Development].toInstance(Hello("development"))

  "Design" should {
    "be immutable" in {
      d0 shouldEqual Design.blanc

      val d2 = d1.bind[Hello].toInstance(Hello("airframe"))
      d2 should not equal(d1)
    }

    "remove binding" in {
      val dd = d1.remove[Message]

      def hasMessage(d:Design) : Boolean =
        d.binding.exists(_.from == ObjectType.ofTypeTag[Message])
      def hasProductionMessage(d:Design) : Boolean =
        d.binding.exists(_.from == ObjectType.ofTypeTag[Message @@ Production])

      hasMessage(d1) shouldBe true
      hasMessage(dd) shouldBe false

      hasProductionMessage(d1) shouldBe true
      hasProductionMessage(dd) shouldBe true
    }

    "be serializable" in {
      val b = new ByteArrayOutputStream()
      val oo = new ObjectOutputStream(b)
      oo.writeObject(d1)
      oo.close()

      val in = new ByteArrayInputStream(b.toByteArray)
      val oi = new ObjectInputStream(in)
      val obj = oi.readObject().asInstanceOf[Design]

      obj shouldBe (d1)
    }
  }
}
