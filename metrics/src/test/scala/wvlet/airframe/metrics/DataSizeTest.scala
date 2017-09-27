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

import wvlet.airframe.AirframeSpec

/**
  *
  */
class DataSizeTest extends AirframeSpec {

  "DataSize" should {
    "parse string" in {
      DataSize("1234B") shouldBe DataSize(1234, DataSize.BYTE)
      DataSize("1234kB") shouldBe DataSize(1234, DataSize.KILOBYTE)
      DataSize("1234MB") shouldBe DataSize(1234, DataSize.MEGABYTE)
      DataSize("1234GB") shouldBe DataSize(1234, DataSize.GIGABYTE)
      DataSize("1234TB") shouldBe DataSize(1234, DataSize.TERABYTE)
      DataSize("1234PB") shouldBe DataSize(1234, DataSize.PETABYTE)

      DataSize("1234.56B") shouldBe DataSize(1234.56, DataSize.BYTE)
      DataSize("1234.56kB") shouldBe DataSize(1234.56, DataSize.KILOBYTE)
      DataSize("1234.56MB") shouldBe DataSize(1234.56, DataSize.MEGABYTE)
      DataSize("1234.56GB") shouldBe DataSize(1234.56, DataSize.GIGABYTE)
      DataSize("1234.56TB") shouldBe DataSize(1234.56, DataSize.TERABYTE)
      DataSize("1234.564PB") shouldBe DataSize(1234.56, DataSize.PETABYTE)
    }

  }

}
