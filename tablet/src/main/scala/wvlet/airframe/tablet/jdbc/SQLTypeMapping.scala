package wvlet.airframe.tablet.jdbc

import java.sql.JDBCType._

import wvlet.airframe.tablet.Schema
import wvlet.airframe.tablet.Schema.ColumnType

/**
  *
  */
object SQLTypeMapping {

  // See also https://github.com/embulk/embulk-input-jdbc/blob/9ce3e5528a205f86e9c2892dd8a3739f685e07e7/embulk-input-jdbc/src/main/java/org/embulk/input/jdbc/getter/ColumnGetterFactory.java#L92
  val default: java.sql.JDBCType => ColumnType = {
    case BIT | BOOLEAN => Schema.BOOLEAN

    case TINYINT | SMALLINT => Schema.INTEGER
    case INTEGER | BIGINT   => Schema.INTEGER

    case FLOAT | REAL => Schema.FLOAT
    case DOUBLE       => Schema.FLOAT

    case NUMERIC | DECIMAL                                      => Schema.STRING // TODO
    case CHAR | VARCHAR | LONGVARCHAR | CLOB | NCHAR | NVARCHAR => Schema.STRING
    case DATE                                                   => Schema.STRING // TODO
    case ARRAY                                                  => Schema.ARRAY(Schema.ANY)
    case _                                                      => Schema.STRING
  }
}
