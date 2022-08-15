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

package wvlet.airframe.sql.catalog
import wvlet.airframe.sql.catalog.DataType._
import wvlet.airspec.AirSpec
import wvlet.log.io.{IOUtil, Resource}

/**
  */
class DataTypeTest extends AirSpec {
  protected def parse(t: String, expected: DataType): Unit = {
    debug(s"parse ${t}")
    val parsed = DataType.parse(t)
    debug(parsed.getClass)
    parsed shouldBe expected
  }

  test("parse DataType names") {
    parse("byte", ByteType)
    parse("char", GenericType("char"))
    parse("short", ShortType)
    parse("int", IntegerType)
    parse("long", LongType)
    parse("float", FloatType)
    parse("real", RealType)
    parse("double", DoubleType)
    parse("boolean", BooleanType)
    parse("any", AnyType)
    parse("null", NullType)
    parse("decimal(10,2)", DecimalType(10, 2))
    parse("decimal(34,0)", DecimalType(34, 0))
    parse("decimal(34, 0)", DecimalType(34, 0))
    parse("json", JsonType)
    parse("binary", BinaryType)
    parse("timestamp", TimestampType())
    parse("array(int)", ArrayType(LongType))
    parse("array(array(string))", ArrayType(ArrayType(StringType)))
    parse("map(string,long)", MapType(StringType, LongType))
    parse("map(string,array(string))", MapType(StringType, ArrayType(StringType)))
    parse(
      """{id:long,name:string}""",
      DataType.RecordType(Seq(NamedType("id", LongType), NamedType("name", StringType)))
    )
    parse(
      """{id:long,name:string,address:array(string)}""",
      DataType.RecordType(
        Seq(NamedType("id", LongType), NamedType("name", StringType), NamedType("address", ArrayType(StringType)))
      )
    )
  }

  test("parse char(x)") {
    // parse("char(x)", StringType)
  }

  test("parse varchar(x)") {
    parse("varchar", StringType)
    parse("varchar(x)", StringType)
    parse("varchar(10)", StringType)
  }

  test("return any type for unknown types") {
    parse("unknown", UnknownType)
    parse("map(bit,long)", MapType(AnyType, LongType))
  }

}
