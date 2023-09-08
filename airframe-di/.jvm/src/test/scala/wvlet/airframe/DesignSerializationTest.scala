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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, ObjectInputStream, ObjectOutputStream}

import DesignTest.*
import wvlet.airspec.AirSpec

class CustomClassLoader(in: InputStream) extends ObjectInputStream(in) {
  override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
    try {
      Class.forName(desc.getName, false, getClass.getClassLoader)
    } catch {
      case _: ClassNotFoundException => super.resolveClass(desc)
    }
  }
}

object DesignSerializationTest {
  def serialize(d: Design): Array[Byte] = {
    val b  = new ByteArrayOutputStream()
    val oo = new ObjectOutputStream(b)
    oo.writeObject(d)
    oo.close()
    b.toByteArray
  }

  def deserialize(b: Array[Byte]): Design = {
    val in = new ByteArrayInputStream(b)
    val oi = new CustomClassLoader(in)
    oi.readObject().asInstanceOf[Design]
  }
}

/**
  */
class DesignSerializationTest extends AirSpec {
  import DesignSerializationTest.*

  test("be serializable") {
    val b   = serialize(d1)
    val d1s = deserialize(b)
    d1s shouldBe (d1)
  }

  test("serialize instance binding") {
    val d  = Design.blanc.bind[Message].toInstance(Hello("world"))
    val b  = serialize(d)
    val ds = deserialize(b)
    ds shouldBe (d)
  }
}
