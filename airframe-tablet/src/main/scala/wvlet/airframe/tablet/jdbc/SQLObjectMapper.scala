package wvlet.airframe.tablet.jdbc

import java.sql.Connection
import java.sql.JDBCType._

import wvlet.airframe.tablet.Schema
import wvlet.airframe.tablet.Schema.DataType
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil._
import wvlet.airframe.surface.reflect.SurfaceFactory
import wvlet.airframe.surface.{Primitive, Surface}

/**
  *
  */
object SQLObjectMapper extends LogSupport {

  // See also https://github.com/embulk/embulk-input-jdbc/blob/9ce3e5528a205f86e9c2892dd8a3739f685e07e7/embulk-input-jdbc/src/main/java/org/embulk/input/jdbc/getter/ColumnGetterFactory.java#L92
  val jdbcToDataType: java.sql.JDBCType => DataType = {
    case BIT | BOOLEAN                                          => Schema.BOOLEAN
    case TINYINT | SMALLINT | INTEGER | BIGINT                  => Schema.INTEGER
    case FLOAT | REAL | DOUBLE                                  => Schema.FLOAT
    case NUMERIC | DECIMAL                                      => Schema.STRING // TODO
    case CHAR | VARCHAR | LONGVARCHAR | CLOB | NCHAR | NVARCHAR => Schema.STRING
    case DATE                                                   => Schema.STRING // TODO
    case ARRAY                                                  => Schema.ARRAY(Schema.ANY)
    case _                                                      => Schema.STRING
  }

  import scala.reflect.runtime.{universe => ru}

  def sqlTypeOf(tpe: Surface): String = {
    tpe match {
      case Primitive.Int | Primitive.Short | Primitive.Byte | Primitive.Char | Primitive.Long => "integer"
      case Primitive.Float | Primitive.Double                                                 => "float"
      case Primitive.Boolean                                                                  => "boolean"
      case Primitive.String                                                                   => "string"
      case _ =>
        debug(s"Unknown SQL type for ${tpe}. Use string instead for SQL")
        "string"
    }
  }

  def createTableSQLFor[A: ru.TypeTag](tableName: String, columnConfig: Map[String, String] = Map.empty): String = {
    val schema = SurfaceFactory.of[A]
    val params = for (p <- schema.params) yield {
      val decl = s""""${p.name}" ${sqlTypeOf(p.surface)}"""
      columnConfig
        .get(p.name).map { config =>
          s"${decl} ${config}"
        }.getOrElse(decl)
    }
    val sql = s"create table if not exists ${tableName} (${params.mkString(", ")})"
    debug(sql)
    sql
  }

  def quote(s: String) = s"'${s}'"

  def insertRecord[A: ru.TypeTag](conn: Connection, tableName: String, obj: A): Unit = {
    val schema  = SurfaceFactory.of[A]
    val colSize = schema.params.size
    val tuple   = ("?" * colSize).toSeq.mkString(", ")
    withResource(conn.prepareStatement(s"insert into ${tableName} values(${tuple})")) { prep =>
      for ((p, i) <- schema.params.zipWithIndex) yield {
        val v = p.get(obj).asInstanceOf[AnyRef]
        if (v == null) {
          prep.setObject(i + 1, null)
        } else {
          p.surface match {
            case Primitive.String =>
              prep.setString(i + 1, v.toString)
            case Primitive.Int =>
              prep.setInt(i + 1, Int.unbox(v))
            case Primitive.Long =>
              prep.setLong(i + 1, Long.unbox(v))
            case Primitive.Float =>
              prep.setFloat(i + 1, Float.unbox(v))
            case Primitive.Double =>
              prep.setDouble(i + 1, Double.unbox(v))
            case Primitive.Boolean =>
              prep.setBoolean(i + 1, Boolean.unbox(v))
            case Primitive.Byte =>
              prep.setByte(i + 1, Byte.unbox(v))
            case Primitive.Short =>
              prep.setShort(i + 1, Short.unbox(v))
            case _ =>
              prep.setObject(i + 1, v)
          }
        }
      }
      prep.execute()
    }
  }
}
