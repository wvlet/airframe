package wvlet.airframe.parquet

import org.apache.parquet.hadoop.ParquetWriter
import wvlet.airframe.parquet.io.AirframeParquetWriter

import scala.reflect.runtime.{universe => ru}

object Parquet {

  def writer[A: ru.TypeTag](
      path: String,
      config: AirframeParquetWriter.Builder[A] => AirframeParquetWriter.Builder[A] =
        identity[AirframeParquetWriter.Builder[A]](_)
  ): ParquetWriter[A] = {
    val b       = AirframeParquetWriter.builder[A](path)
    val builder = config(b)
    builder.build()
  }

}
