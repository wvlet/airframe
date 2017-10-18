package wvlet.airframe.tablet.jdbc

import java.sql.Connection

import wvlet.log.LogSupport
import wvlet.log.io.IOUtil._
import wvlet.surface.reflect.SurfaceFactory
import wvlet.surface.{Primitive, Surface}

/**
  *
  */
object SQLObjectMapper extends LogSupport {

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

  def createTableSQLFor[A: ru.TypeTag](tableName: String): String = {
    val schema = SurfaceFactory.of[A]
    val params = for (p <- schema.params) yield {
      s"${p.name} ${sqlTypeOf(p.surface)}"
    }
    s"create table if not exists ${tableName} (${params.mkString(", ")})"
  }

  def quote(s: String) = s"'${s}'"

  def insertRecord[A: ru.TypeTag](conn: Connection, tableName: String, obj: A) {
    val schema  = SurfaceFactory.of[A]
    val colSize = schema.params.size
    val tuple   = ("?" * colSize).mkString(", ")
    withResource(conn.prepareStatement(s"insert into ${tableName} values(${tuple})")) { prep =>
      for ((p, i) <- schema.params.zipWithIndex) yield {
        val v = p.get(obj)
        prep.setObject(i + 1, v)
      }
      prep.execute()
    }
  }

}
