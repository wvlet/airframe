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
package wvlet.airframe.tablet

import java.util.Locale

import wvlet.airframe.AirframeSpec
import wvlet.airframe.tablet.Schema._

/**
  *
  */
class SchemaTest extends AirframeSpec {
  "DataType" should {

    "have primitive types" in {
      Schema.primitiveTypes should contain(Schema.NIL)
      Schema.primitiveTypes should contain(Schema.INTEGER)
      Schema.primitiveTypes should contain(Schema.FLOAT)
      Schema.primitiveTypes should contain(Schema.BOOLEAN)
      Schema.primitiveTypes should contain(Schema.STRING)
      Schema.primitiveTypes should contain(Schema.TIMESTAMP)
      Schema.primitiveTypes should contain(Schema.BINARY)
      Schema.primitiveTypes should contain(Schema.JSON)

      for (p <- Schema.primitiveTypes) {
        val name = p.toString.toLowerCase(Locale.ENGLISH)
        p.typeName shouldBe name
        p.signature shouldBe name
        p.typeArgs shouldBe empty
      }
    }

    "should have any" in {
      val a = Schema.ANY
      a.typeName shouldBe "any"
      a.signature shouldBe "any"
      a.typeArgs shouldBe empty
    }

    "should have typeName" in {
      Schema.NIL.typeName shouldBe "nil"
    }

    "support array types" in {
      val a = Schema.ARRAY(Schema.INTEGER)
      a.signature shouldBe "array[integer]"
      a.typeName shouldBe "array"
      a.typeArgs shouldBe Seq(Schema.INTEGER)
    }

    "support map types" in {
      val m = Schema.MAP(Schema.INTEGER, Schema.STRING)
      m.signature shouldBe "map[integer,string]"
      m.typeName shouldBe "map"
      m.typeArgs shouldBe Seq(Schema.INTEGER, Schema.STRING)
    }

    "support record types" in {
      val c1 = Column("c1", Schema.INTEGER)
      val c2 = Column("c2", Schema.FLOAT)
      val r  = Schema.RecordType("MyType", Seq(c1, c2))
      r.signature shouldBe "MyType(c1:integer,c2:float)"
      r.typeName shouldBe "MyType"
      r.typeArgs shouldBe empty

      r.size shouldBe 2

      r.columnType(0) shouldBe c1
      r.columnType(1) shouldBe c2

      r.columnType(0) shouldBe c1
      r.columnType(1) shouldBe c2

      r.columnIndex("c1") shouldBe 0
      r.columnIndex("c2") shouldBe 1
    }

    "detect duplicate column names" in {
      intercept[IllegalArgumentException] {
        Schema.RecordType("A", Seq(Column("c", Schema.INTEGER), Column("c", Schema.STRING)))
      }
    }

    "support union types" in {
      val r1 = Schema.RecordType("A", Seq(Column("c1", Schema.INTEGER), Column("c2", Schema.FLOAT)))
      val r2 = Schema.RecordType("B", Seq(Column("c1", Schema.INTEGER), Column("c2", Schema.FLOAT), Column("c3", Schema.JSON)))
      val u  = Schema.UNION(Seq(r1, r2))
      u.signature shouldBe "union[A|B]"
      u.typeArgs shouldBe Seq(r1, r2)
      u.typeName shouldBe "union"
    }

  }

}
