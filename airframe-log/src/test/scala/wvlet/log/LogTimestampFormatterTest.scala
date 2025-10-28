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
package wvlet.log

/**
  * Test to verify that the native LogTimestampFormatter produces reasonable output
  */
class LogTimestampFormatterTest extends Spec {
  test("should format timestamps correctly") {
    val testTime = 1642680000000L // January 20, 2022 12:00:00 UTC

    val formatted        = LogTimestampFormatter.formatTimestamp(testTime)
    val formattedNoSpace = LogTimestampFormatter.formatTimestampWithNoSpaace(testTime)

    debug(s"formatTimestamp: $formatted")
    debug(s"formatTimestampWithNoSpaace: $formattedNoSpace")

    // Check basic format structure
    formatted shouldContain "2022"
    formatted shouldContain " " // Should have space separator
    formatted shouldContain ":"
    formatted shouldContain "."
    formatted shouldContain "Z"

    formattedNoSpace shouldContain "2022"
    formattedNoSpace shouldContain "T" // Should have T separator
    formattedNoSpace shouldContain ":"
    formattedNoSpace shouldContain "."
    formattedNoSpace shouldContain "Z"
  }

  test("should handle current time") {
    val currentTime = System.currentTimeMillis()

    val formatted        = LogTimestampFormatter.formatTimestamp(currentTime)
    val formattedNoSpace = LogTimestampFormatter.formatTimestampWithNoSpaace(currentTime)

    debug(s"Current time formatTimestamp: $formatted")
    debug(s"Current time formatTimestampWithNoSpaace: $formattedNoSpace")

    // Basic sanity checks
    formatted.length should be > 20
    formattedNoSpace.length should be > 20
    formatted shouldContain "2025" // We know we're in 2025
    formattedNoSpace shouldContain "2025"
  }
}
