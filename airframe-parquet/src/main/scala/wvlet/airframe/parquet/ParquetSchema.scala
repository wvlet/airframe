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

import org.apache.parquet.schema.LogicalTypeAnnotation.stringType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.{MessageType, PrimitiveType, Type, Types}
import org.apache.parquet.schema.Types.PrimitiveBuilder
import wvlet.airframe.surface.Primitive.PrimitiveSurface
import wvlet.airframe.surface.{OptionSurface, Parameter, Primitive, Surface}

import scala.jdk.CollectionConverters._

object ParquetSchema {

  private def toParquetPrimitiveTypeName(s: PrimitiveSurface): PrimitiveTypeName = {
    s match {
      case Primitive.Int | Primitive.Short | Primitive.Byte =>
        PrimitiveTypeName.INT32
      case Primitive.Long =>
        PrimitiveTypeName.INT64
      case Primitive.Float =>
        PrimitiveTypeName.FLOAT
      case Primitive.Double =>
        PrimitiveTypeName.DOUBLE
      case Primitive.String | Primitive.Char =>
        PrimitiveTypeName.BINARY
      case Primitive.Boolean =>
        PrimitiveTypeName.BOOLEAN
      case _ =>
        PrimitiveTypeName.BINARY
    }
  }

  private def toParquetPrimitive(
      surface: PrimitiveSurface,
      rep: Option[Type.Repetition] = None
  ): PrimitiveBuilder[PrimitiveType] = {

    val repetition = rep.getOrElse(Type.Repetition.OPTIONAL)
    val typeName   = toParquetPrimitiveTypeName(surface)
    surface match {
      case Primitive.String | Primitive.Char =>
        Types.primitive(typeName, repetition).as(stringType)
      case _ =>
        Types.primitive(typeName, repetition)
    }
  }

  def toParquetType(name: String, surface: Surface, rep: Option[Type.Repetition] = None): Type = {
    val repetition = rep.getOrElse(Type.Repetition.OPTIONAL)
    surface match {
      case p: PrimitiveSurface =>
        toParquetPrimitive(p, rep).named(name)
      case o: OptionSurface =>
        toParquetType(name, o.elementSurface, Some(Type.Repetition.OPTIONAL))
      case s: Surface if s.isSeq || s.isArray =>
        val elementSurface = s.typeArgs(0)
        toParquetType(name, elementSurface, Some(Type.Repetition.REPEATED))
      case m: Surface if m.isMap =>
        val keySurface   = m.typeArgs(0)
        val valueSurface = m.typeArgs(1)
        keySurface match {
          case p: PrimitiveSurface =>
            val keyType = toParquetPrimitiveTypeName(p)
            val mapType = Types.map(rep.getOrElse(Type.Repetition.REPEATED)).key(keyType)
            valueSurface match {
              case vp: PrimitiveSurface =>
                val valueType = toParquetPrimitiveTypeName(vp)
                mapType.optionalValue(valueType).named(name)
              case other =>
                ???
              // mapType.optionalValue()

            }

        }

      case _ =>
        // TODO Support Array/Seq/Map types. Just use MsgPack binary here
        Types.primitive(PrimitiveTypeName.BINARY, repetition).named(name)
    }
  }

  def toParquetSchema(surface: Surface): MessageType = {

    def toType(p: Parameter): Type = {
      toParquetType(p.name, p.surface, if (p.isRequired) Some(Type.Repetition.REQUIRED) else None)
    }

    new MessageType(
      surface.fullName,
      surface.params.map(p => toType(p)).toList.asJava
    )
  }

}
