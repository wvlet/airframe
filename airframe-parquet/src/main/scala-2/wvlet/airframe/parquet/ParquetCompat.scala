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

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.{ParquetReader, ParquetWriter}
import wvlet.airframe.surface.Surface

import scala.reflect.runtime.{universe => ru}

trait ParquetCompat {
  def newWriter[A: ru.TypeTag](
      path: String,
      // Hadoop filesystem specific configuration, e.g., fs.s3a.access.key
      hadoopConf: Configuration = new Configuration(),
      config: ParquetWriterAdapter.Builder[A] => ParquetWriterAdapter.Builder[A] =
        identity[ParquetWriterAdapter.Builder[A]](_)
  ): ParquetWriter[A] = {
    Parquet.newObjectWriter[A](Surface.of[A], path, hadoopConf, config)
  }

  def newReader[A: ru.TypeTag](
      path: String,
      // Hadoop filesystem specific configuration, e.g., fs.s3a.access.key
      hadoopConf: Configuration = new Configuration(),
      config: ParquetReader.Builder[A] => ParquetReader.Builder[A] = identity[ParquetReader.Builder[A]](_)
  ): ParquetReader[A] = {
    Parquet.newObjectReader[A](Surface.of[A], path, hadoopConf, config)
  }

  def query[A: ru.TypeTag](
      path: String,
      sql: String,
      hadoopConf: Configuration = new Configuration(),
      config: ParquetReader.Builder[A] => ParquetReader.Builder[A] = identity[ParquetReader.Builder[A]](_)
  ): ParquetReader[A] = {
    Parquet.queryObject[A](Surface.of[A], path, sql, hadoopConf, config)
  }
}
