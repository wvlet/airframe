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
import wvlet.airframe.surface.{ArraySurface, GenericSurface, OptionSurface, Primitive, Surface}
import wvlet.airframe.surface.Primitive.PrimitiveSurface
import wvlet.airspec.AirSpec

object ParquetSchemaTest extends AirSpec {

  private val primitiveTypeMapping = Map(
    Primitive.Short   -> PrimitiveTypeName.INT32,
    Primitive.Byte    -> PrimitiveTypeName.INT32,
    Primitive.Int     -> PrimitiveTypeName.INT32,
    Primitive.Long    -> PrimitiveTypeName.INT64,
    Primitive.Char    -> PrimitiveTypeName.BINARY,
    Primitive.String  -> PrimitiveTypeName.BINARY,
    Primitive.Float   -> PrimitiveTypeName.FLOAT,
    Primitive.Double  -> PrimitiveTypeName.DOUBLE,
    Primitive.Boolean -> PrimitiveTypeName.BOOLEAN
  )

  test("Convert PrimitiveSurface to Parquet type") {
    def check(surface: PrimitiveSurface, expected: PrimitiveTypeName): Unit = {
      val p = ParquetSchema.toParquetType("c1", surface)
      p.getName shouldBe "c1"
      p.getRepetition shouldBe Repetition.OPTIONAL
      p.asPrimitiveType().getPrimitiveTypeName shouldBe expected
    }

    // Check for all primitive types
    for ((surface, tpe) <- primitiveTypeMapping) {
      check(surface, tpe)
    }

    // Non native types
    check(Primitive.BigInt, PrimitiveTypeName.BINARY)
    check(Primitive.Unit, PrimitiveTypeName.BINARY)
  }

  test("Array type") {
    for ((surface, tpe) <- primitiveTypeMapping) {
      val t = ParquetSchema.toParquetType("a1", ArraySurface(classOf[Array[_]], surface))
      t.getRepetition shouldBe Repetition.REPEATED
      t.asPrimitiveType().getPrimitiveTypeName shouldBe tpe
    }
  }

  test("Seq type") {
    for ((surface, tpe) <- primitiveTypeMapping) {
      val t = ParquetSchema.toParquetType("a1", new GenericSurface(classOf[Seq[_]], Seq(surface)))
      t.getRepetition shouldBe Repetition.REPEATED
      t.asPrimitiveType().getPrimitiveTypeName shouldBe tpe
    }
  }

  test("Option type") {
    for ((surface, tpe) <- primitiveTypeMapping) {
      val t = ParquetSchema.toParquetType("c0", OptionSurface(surface.rawType, surface))
      t.getRepetition shouldBe Repetition.OPTIONAL
      t.asPrimitiveType().getPrimitiveTypeName shouldBe tpe
    }
  }

  test("Map type") {
    val t = ParquetSchema.toParquetType("m0", Surface.of[Map[String, Int]])
    t.getRepetition shouldBe Repetition.OPTIONAL
    val mt = t.asGroupType().getType(0).asGroupType()
    val kt = mt.getType(0)
    kt.getRepetition shouldBe Repetition.REQUIRED
    kt.asPrimitiveType().getPrimitiveTypeName shouldBe PrimitiveTypeName.BINARY
    val vt = mt.getType(1)
    vt.getRepetition shouldBe Repetition.REQUIRED
    vt.asPrimitiveType().getPrimitiveTypeName shouldBe PrimitiveTypeName.INT32
  }

  case class A(
      id: Int,
      name: String,
      address: Seq[String],
      cls: Option[Long],
      dept: B,
      refs: Seq[B],
      metadata: Map[String, Any]
  )

  case class B(
      id: Long,
      name: Boolean
  )

  test("object type") {
    val s = ParquetSchema.toParquetSchema(Surface.of[A])
    info(s)
  }

}
