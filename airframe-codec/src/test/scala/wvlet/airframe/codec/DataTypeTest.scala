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
package wvlet.airframe.codec

import wvlet.airframe.AirframeSpec
import wvlet.airframe.codec.DataType.Column

/**
  *
  */
class DataTypeTest extends AirframeSpec {
  "DataType" should {

    "have primitive types" in {
      DataType.primitiveTypes should contain(DataType.NIL)
      DataType.primitiveTypes should contain(DataType.INTEGER)
      DataType.primitiveTypes should contain(DataType.FLOAT)
      DataType.primitiveTypes should contain(DataType.BOOLEAN)
      DataType.primitiveTypes should contain(DataType.STRING)
      DataType.primitiveTypes should contain(DataType.TIMESTAMP)
      DataType.primitiveTypes should contain(DataType.BINARY)
      DataType.primitiveTypes should contain(DataType.JSON)

      for (p <- DataType.primitiveTypes) {
        val name = p.toString.toLowerCase()
        p.typeName shouldBe name
        p.signature shouldBe name
        p.typeArgs shouldBe empty
      }
    }

    "should have any" in {
      val a = DataType.ANY
      a.typeName shouldBe "any"
      a.signature shouldBe "any"
      a.typeArgs shouldBe empty
    }

    "should have typeName" in {
      DataType.NIL.typeName shouldBe "nil"
    }

    "support array types" in {
      val a = DataType.ARRAY(DataType.INTEGER)
      a.signature shouldBe "array[integer]"
      a.typeName shouldBe "array"
      a.typeArgs shouldBe Seq(DataType.INTEGER)
    }

    "support map types" in {
      val m = DataType.MAP(DataType.INTEGER, DataType.STRING)
      m.signature shouldBe "map[integer,string]"
      m.typeName shouldBe "map"
      m.typeArgs shouldBe Seq(DataType.INTEGER, DataType.STRING)
    }

    "support record types" in {
      val c1 = Column("c1", DataType.INTEGER)
      val c2 = Column("c2", DataType.FLOAT)
      val r  = DataType.RecordType("MyType", Seq(c1, c2))
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
        DataType.RecordType("A", Seq(Column("c", DataType.INTEGER), Column("c", DataType.STRING)))
      }
    }

    "support union types" in {
      val r1 = DataType.RecordType("A", Seq(Column("c1", DataType.INTEGER), Column("c2", DataType.FLOAT)))
      val r2 = DataType.RecordType(
        "B",
        Seq(Column("c1", DataType.INTEGER), Column("c2", DataType.FLOAT), Column("c3", DataType.JSON)))
      val u = DataType.UNION(Seq(r1, r2))
      u.signature shouldBe "union[A|B]"
      u.typeArgs shouldBe Seq(r1, r2)
      u.typeName shouldBe "union"
    }

  }

}
