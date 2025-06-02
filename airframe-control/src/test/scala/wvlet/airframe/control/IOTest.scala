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
package wvlet.airframe.control

import wvlet.airspec.AirSpec
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets

/**
  * Test for IO utilities
  */
class IOTest extends AirSpec {

  test("copy from InputStream to OutputStream") {
    val testData   = "Hello, World! This is a test string for IO.copy functionality."
    val inputBytes = testData.getBytes(StandardCharsets.UTF_8)

    val inputStream  = new ByteArrayInputStream(inputBytes)
    val outputStream = new ByteArrayOutputStream()

    IO.copy(inputStream, outputStream)

    val copiedBytes  = outputStream.toByteArray
    val copiedString = new String(copiedBytes, StandardCharsets.UTF_8)

    copiedString shouldBe testData
    copiedBytes.length shouldBe inputBytes.length
  }

  test("copy with null input stream should throw NullPointerException") {
    val outputStream = new ByteArrayOutputStream()

    intercept[NullPointerException] {
      IO.copy(null, outputStream)
    }
  }

  test("copy with null output stream should throw NullPointerException") {
    val testData    = "Test data"
    val inputStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8))

    intercept[NullPointerException] {
      IO.copy(inputStream, null)
    }
  }

  test("copy with both null streams should throw NullPointerException") {
    intercept[NullPointerException] {
      IO.copy(null, null)
    }
  }

  test("copy with empty input stream") {
    val inputStream  = new ByteArrayInputStream(Array.empty[Byte])
    val outputStream = new ByteArrayOutputStream()

    IO.copy(inputStream, outputStream)

    outputStream.toByteArray.length shouldBe 0
  }

  test("copy large data") {
    // Test with data larger than buffer size (8192 bytes)
    val largeData  = "A" * 10000 // 10KB of 'A' characters
    val inputBytes = largeData.getBytes(StandardCharsets.UTF_8)

    val inputStream  = new ByteArrayInputStream(inputBytes)
    val outputStream = new ByteArrayOutputStream()

    IO.copy(inputStream, outputStream)

    val copiedBytes  = outputStream.toByteArray
    val copiedString = new String(copiedBytes, StandardCharsets.UTF_8)

    copiedString shouldBe largeData
    copiedBytes.length shouldBe inputBytes.length
  }
}
