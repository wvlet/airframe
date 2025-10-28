package wvlet.airframe.parquet

import org.apache.parquet.hadoop.{ParquetReader, ParquetWriter}
import wvlet.airframe.surface.Surface

trait ParquetCompat:

  inline def newWriter[A](
      path: String,
      config: ParquetWriterAdapter.Builder[A] => ParquetWriterAdapter.Builder[A] =
        identity[ParquetWriterAdapter.Builder[A]](_)
  ): ParquetWriter[A] =
    Parquet.newObjectWriter[A](Surface.of[A], path, config)

  inline def newReader[A](
      path: String,
      config: ParquetReader.Builder[A] => ParquetReader.Builder[A] = identity[ParquetReader.Builder[A]](_)
  ): ParquetReader[A] =
    Parquet.newObjectReader[A](Surface.of[A], path, config)

  inline def query[A](
      path: String,
      sql: String,
      config: ParquetReader.Builder[A] => ParquetReader.Builder[A] = identity[ParquetReader.Builder[A]](_)
  ): ParquetReader[A] =
    Parquet.queryObject[A](Surface.of[A], path, sql, config)
