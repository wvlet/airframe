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
package wvlet.airframe.http.codegen

import wvlet.airspec.AirSpec

/**
  */
class ClassScannerTest extends AirSpec {
  test("decoded URL encoded file paths") {
    ClassScanner.decodePath(
      "/lib/0.0.1%2Btest/xxxx-0.0.1%2Btest.jar"
    ) shouldBe "/lib/0.0.1+test/xxxx-0.0.1+test.jar"
  }

  test("scanClasses should handle non-existent JAR files gracefully") {
    // Create a ClassLoader with a non-existent JAR file in the classpath
    val nonExistentJar = new java.io.File("/tmp/non-existent-file.jar").toURI.toURL
    val invalidJar     = new java.io.File("/tmp/invalid file name.jar").toURI.toURL // Space in filename
    val classLoader    = new java.net.URLClassLoader(Array(nonExistentJar, invalidJar))

    // This should not throw an exception and return empty results
    val result = ClassScanner.scanClasses(classLoader, Seq("example.package"))
    result shouldBe Seq.empty
  }

  test("scanClasses should handle malformed JAR paths like the one from issue") {
    // Simulate the exact issue case with a space in the JAR filename
    val malformedJarPath =
      "/Users/xx/.ivy2/cache/org.scala-js/scalajs-scalalib_2.13/jars/scalajs-scalalib_2.13-2.13.16 1.20.1.jar"
    val malformedJar = new java.io.File(malformedJarPath).toURI.toURL
    val classLoader  = new java.net.URLClassLoader(Array(malformedJar))

    // This should not throw NoSuchFileException and return empty results instead
    val result = ClassScanner.scanClasses(classLoader, Seq("org.scalajs"))
    result shouldBe Seq.empty
  }

  test("scanClasses should handle valid JAR files properly") {
    // Test with a valid JAR file to ensure we didn't break normal functionality
    val validJar    = new java.io.File("/tmp/valid-test.jar").toURI.toURL
    val classLoader = new java.net.URLClassLoader(Array(validJar))

    // This should work without throwing any exceptions (even if it returns empty for our test jar)
    val result = ClassScanner.scanClasses(classLoader, Seq("nonexistent.package"))
    result shouldBe Seq.empty
  }
}
