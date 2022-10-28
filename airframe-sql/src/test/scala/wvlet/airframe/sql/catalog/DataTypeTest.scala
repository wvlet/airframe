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

class DataTypeTest extends AirSpec {
  protected def parse(t: String, expected: DataType): Unit = {
    debug(s"parse ${t}")
    val parsed = DataType.parse(t)
    parsed shouldBe expected
  }

  test("parse primitive types") {
    parse("byte", ByteType)
    parse("short", ShortType)
    parse("int", IntegerType)
    parse("long", LongType)
    parse("float", FloatType)
    parse("real", RealType)
    parse("double", DoubleType)
    parse("boolean", BooleanType)
    parse("any", AnyType)
    parse("null", NullType)
    parse("date", DateType)
    parse("json", JsonType)
    parse("binary", BinaryType)
  }

  test("parse Trino SQL types in https://trino.io/docs/current/language/types.html") {
    parse("ipaddress", GenericType("ipaddress"))
  }

  test("parse decimal types") {
    parse("decimal(10,2)", DecimalType(10, 2))
    parse("decimal(34,0)", DecimalType(34, 0))
    parse("decimal(34, 0)", DecimalType(34, 0))
  }

  test("parse timestamp types") {
    parse(
      "timestamp(1)",
      TimestampType(TimestampField.TIMESTAMP, withTimeZone = false, precision = Some(IntConstant(1)))
    )
    parse(
      "timestamp(2) with time zone",
      TimestampType(TimestampField.TIMESTAMP, withTimeZone = true, precision = Some(IntConstant(2)))
    )

    parse(
      "time(1)",
      TimestampType(TimestampField.TIME, withTimeZone = false, precision = Some(IntConstant(1)))
    )
    parse(
      "time(2) with time zone",
      TimestampType(TimestampField.TIME, withTimeZone = true, precision = Some(IntConstant(2)))
    )
  }

  test("parse array/map types") {
    parse("array(int)", ArrayType(IntegerType))
    parse("array(array(string))", ArrayType(ArrayType(StringType)))
    parse("array<int>", ArrayType(IntegerType))
    parse("array<array<string>>", ArrayType(ArrayType(StringType)))
    parse("map(string,long)", MapType(StringType, LongType))
    parse("map(string,array(string))", MapType(StringType, ArrayType(StringType)))
    parse("map<string,long>", MapType(StringType, LongType))
    parse("map<string,array<string>>", MapType(StringType, ArrayType(StringType)))
  }

  test("parse row types") {
    parse(
      """row(id long,name string)""",
      DataType.RecordType(Seq(NamedType("id", LongType), NamedType("name", StringType)))
    )
    parse(
      """row(id long,name string,address array(string))""",
      DataType.RecordType(
        Seq(NamedType("id", LongType), NamedType("name", StringType), NamedType("address", ArrayType(StringType)))
      )
    )
  }

  test("parse char(x)") {
    // parse("char(x)", StringType)
  }

  test("parse varchar(x)") {
    parse("varchar", VarcharType(None))
    parse("varchar(x)", VarcharType(Some(TypeVariable("x"))))
    parse("varchar(10)", VarcharType(Some(IntConstant(10))))
  }
}
