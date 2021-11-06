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
package wvlet.airframe.parquet

import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type.Repetition
import wvlet.airframe.surface.Primitive
import wvlet.airframe.surface.Primitive.PrimitiveSurface
import wvlet.airspec.AirSpec

object ParquetSchemaTest extends AirSpec {
  test("Convert PrimitiveSurface to Parquet type") {
    def check(surface: PrimitiveSurface, expected: PrimitiveTypeName): Unit = {
      val p = ParquetSchema.toParquetType("c1", surface)
      p.getName shouldBe "c1"
      p.getRepetition shouldBe Repetition.OPTIONAL
      p.asPrimitiveType().getPrimitiveTypeName shouldBe expected
    }

    check(Primitive.Short, PrimitiveTypeName.INT32)
    check(Primitive.Byte, PrimitiveTypeName.INT32)
    check(Primitive.Int, PrimitiveTypeName.INT32)
    check(Primitive.Long, PrimitiveTypeName.INT64)
    check(Primitive.Char, PrimitiveTypeName.BINARY)
    check(Primitive.String, PrimitiveTypeName.BINARY)
    check(Primitive.Float, PrimitiveTypeName.FLOAT)
    check(Primitive.Double, PrimitiveTypeName.DOUBLE)
    check(Primitive.Boolean, PrimitiveTypeName.BOOLEAN)

    // Non native types
    check(Primitive.BigInt, PrimitiveTypeName.BINARY)
    check(Primitive.Unit, PrimitiveTypeName.BINARY)
  }

  test("...") {}
}
