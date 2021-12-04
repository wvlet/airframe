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
import org.apache.parquet.schema.Type.Repetition
import org.apache.parquet.schema.{LogicalTypeAnnotation, MessageType, PrimitiveType, Type, Types}
import org.apache.parquet.schema.Types.{MapBuilder, PrimitiveBuilder}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.surface.Primitive.PrimitiveSurface
import wvlet.airframe.surface.{
  ArraySurface,
  OptionSurface,
  Parameter,
  Primitive,
  RecordParameter,
  RecordSurface,
  Surface
}
import wvlet.airframe.ulid.ULID

import java.util.UUID
import scala.jdk.CollectionConverters._

object ParquetSchema {

  // Convert surface into primitive
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
      rep: Option[Repetition] = None
  ): PrimitiveBuilder[PrimitiveType] = {

    val repetition = rep.getOrElse(Repetition.OPTIONAL)
    val typeName   = toParquetPrimitiveTypeName(surface)
    surface match {
      case Primitive.String | Primitive.Char =>
        Types.primitive(typeName, repetition).as(stringType)
      case _ =>
        Types.primitive(typeName, repetition)
    }
  }

  private def buildParquetType(surface: Surface, rep: Option[Repetition]): Types.Builder[_, _] = {
    surface match {
      case p: PrimitiveSurface =>
        toParquetPrimitive(p, rep)
      case o: OptionSurface =>
        buildParquetType(o.elementSurface, Some(Repetition.OPTIONAL))
      case s: Surface if s.isSeq || s.isArray =>
        val elementSurface = s.typeArgs(0)
        buildParquetType(elementSurface, Some(Repetition.REPEATED))
      case m: Surface if m.isMap =>
        // Encode Map[_, _] type as Binary and make it optional as Map can be empty
        Types.primitive(PrimitiveTypeName.BINARY, Repetition.OPTIONAL)
//      case m: Surface if m.isMap =>
//        val keySurface   = m.typeArgs(0)
//        val valueSurface = m.typeArgs(1)
//        val keyType      = toParquetType("key", keySurface, Some(Repetition.REQUIRED))
//        val valueType    = toParquetType("value", valueSurface, Some(Repetition.REQUIRED))
//        val mapType      = Types.map(rep.getOrElse(Repetition.OPTIONAL))
//        mapType.key(keyType).value(valueType)
      case s: Surface if s.rawType == classOf[ULID] || s.rawType == classOf[UUID] =>
        // Use string type for ULID
        Types.primitive(PrimitiveTypeName.BINARY, rep.getOrElse(Repetition.OPTIONAL)).as(stringType())
      case s: Surface if s.params.size > 0 =>
        // e.g., case class objects
        var groupType = Types.buildGroup(rep.getOrElse(Repetition.OPTIONAL))
        for (p <- s.params) {
          groupType = groupType.addField(toParquetType(p.name, p.surface, Some(Repetition.REQUIRED)))
        }
        groupType
      case s: Surface =>
        // Use MsgPack for other types
        Types.primitive(PrimitiveTypeName.BINARY, rep.getOrElse(Repetition.OPTIONAL))
    }
  }

  def buildSurfaceFromParquetSchema(schema: Type): Surface = {
    def toSurface(t: Type): Surface = {
      if (t.isPrimitive) {
        val p = t.asPrimitiveType()
        p.getPrimitiveTypeName match {
          case PrimitiveTypeName.INT32 =>
            Primitive.Int
          case PrimitiveTypeName.INT64 =>
            Primitive.Long
          case PrimitiveTypeName.FLOAT =>
            Primitive.Float
          case PrimitiveTypeName.DOUBLE =>
            Primitive.Double
          case PrimitiveTypeName.BOOLEAN =>
            Primitive.Boolean
          case PrimitiveTypeName.BINARY if p.getLogicalTypeAnnotation == stringType() =>
            Primitive.String
          case _ =>
            Surface.of[MsgPack]
        }
      } else {
        val g = t.asGroupType()
        var r = RecordSurface.newSurface(t.getName)
        for ((f, i) <- g.getFields.asScala.zipWithIndex) {
          r = r.addParam(RecordParameter(i, f.getName, toSurface(f)))
        }
        r
      }
    }
    toSurface(schema)
  }

  def toParquetType(name: String, surface: Surface, rep: Option[Repetition] = None): Type = {
    buildParquetType(surface, rep).named(name).asInstanceOf[Type]
  }

  def toParquetSchema(surface: Surface): MessageType = {
    def toType(p: Parameter): Type = {
      toParquetType(p.name, p.surface, Some(Repetition.REQUIRED))
    }
    val tpe = new MessageType(
      surface.fullName,
      surface.params.map(p => toType(p)).toList.asJava
    )
    tpe
  }

}
