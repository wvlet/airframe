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
package wvlet.airframe.metrics

import wvlet.airspec.AirSpec

/**
  */
class DataSizeTest extends AirSpec {
  scalaJsSupport

  import DataSize._

  def `parse string`: Unit = {
    DataSize(1234) shouldBe DataSize(1234, DataSize.BYTE)
    "1234".toDataSize shouldBe DataSize(1234, DataSize.BYTE)
    "1234B".toDataSize shouldBe DataSize(1234, DataSize.BYTE)
    "1234kB".toDataSize shouldBe DataSize(1234, DataSize.KILOBYTE)
    "1234MB".toDataSize shouldBe DataSize(1234, DataSize.MEGABYTE)
    "1234GB".toDataSize shouldBe DataSize(1234, DataSize.GIGABYTE)
    "1234TB".toDataSize shouldBe DataSize(1234, DataSize.TERABYTE)
    "1234PB".toDataSize shouldBe DataSize(1234, DataSize.PETABYTE)

    "1234.56B".toDataSize shouldBe DataSize(1234.56, DataSize.BYTE)
    "1234.56kB".toDataSize shouldBe DataSize(1234.56, DataSize.KILOBYTE)
    "1234.56MB".toDataSize shouldBe DataSize(1234.56, DataSize.MEGABYTE)
    "1234.56GB".toDataSize shouldBe DataSize(1234.56, DataSize.GIGABYTE)
    "1234.56TB".toDataSize shouldBe DataSize(1234.56, DataSize.TERABYTE)
    "1234.56PB".toDataSize shouldBe DataSize(1234.56, DataSize.PETABYTE)
  }

  def `generate succinct rep`: Unit = {
    def checkSuccinctRepOf(bytes: Long, repr: String): Unit = {
      DataSize.succinct(bytes).toString shouldBe repr
    }

    checkSuccinctRepOf(123, "123B")
    checkSuccinctRepOf((5.5 * 1024).toLong, "5.50kB")
    checkSuccinctRepOf(3L * 1024 * 1024, "3MB")
    checkSuccinctRepOf(3L * 1024 * 1024 * 1024, "3GB")
    checkSuccinctRepOf(3L * 1024 * 1024 * 1024 * 1024, "3TB")
    checkSuccinctRepOf(3L * 1024 * 1024 * 1024 * 1024 * 1024, "3PB")
  }

  def `be convertible to another unit`: Unit = {
    val d = "10GB".toDataSize
    d.convertTo(DataSize.BYTE).toString shouldBe "10737418240B"
    d.convertTo(DataSize.KILOBYTE).toString shouldBe "10485760kB"
    d.convertTo(DataSize.MEGABYTE).toString shouldBe "10240MB"
    d.convertTo(DataSize.GIGABYTE).toString shouldBe "10GB"
    d.convertTo(DataSize.TERABYTE).toString shouldBe "0.01TB"
    d.convertTo(DataSize.PETABYTE).toString shouldBe "0.00PB"
  }

  def `round to smaller unit`: Unit = {
    10000.toDataSize.roundTo(KILOBYTE) shouldBe 10
    10000000.toDataSize.roundTo(MEGABYTE) shouldBe 10
  }

  def `be comparable`: Unit = {
    val input = Seq(
      "1GB".toDataSize,
      "8kB".toDataSize,
      "2048MB".toDataSize,
      "10B".toDataSize,
      "20MB".toDataSize,
      "10PB".toDataSize,
      "20PB".toDataSize
    )
    val sorted = input.sorted
    sorted shouldBe Seq(
      "10B".toDataSize,
      "8kB".toDataSize,
      "20MB".toDataSize,
      "1GB".toDataSize,
      "2048MB".toDataSize,
      "10PB".toDataSize,
      "20PB".toDataSize
    )
  }

  def `be reprsented as bytes`: Unit = {
    "1".toDataSize.toBytes shouldBe 1L
    "1B".toDataSize.toBytes shouldBe 1L
    "1kB".toDataSize.toBytes shouldBe 1L * 1024
    "1MB".toDataSize.toBytes shouldBe 1L * 1024 * 1024
    "1GB".toDataSize.toBytes shouldBe 1L * 1024 * 1024 * 1024
    "1TB".toDataSize.toBytes shouldBe 1L * 1024 * 1024 * 1024 * 1024
    "1PB".toDataSize.toBytes shouldBe 1L * 1024 * 1024 * 1024 * 1024 * 1024
  }
}
