package wvlet.msgframe.sql.model
import wvlet.airframe.surface.Surface

/**
  *
  */
object SQLSchema {
  sealed trait Schema
  case class AnonSchema(columns: Seq[Column])                extends Schema
  case class TableSchema(name: String, columns: Seq[Column]) extends Schema

  case class Column(name: String, columnType: DataType)

  sealed trait DataType
  case object IntegerType                                    extends DataType
  case object FloatType                                      extends DataType
  case object BooleanType                                    extends DataType
  case object StringType                                     extends DataType
  case object JSONType                                       extends DataType
  case object BinaryType                                     extends DataType
  case object NullType                                       extends DataType
  case object TimestampType                                  extends DataType
  case class ObjectType(surface: Surface)                    extends DataType
  case class AraryType(elemType: DataType)                   extends DataType
  case class MapType(keyType: DataType, valueType: DataType) extends DataType
  case object AnyType                                        extends DataType
}
