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

import wvlet.airframe.surface.reflect.ReflectSurfaceFactory
import wvlet.airspec.AirSpec

/**
  */
class ClassScannerTest extends AirSpec {
  test("decoded URL encoded file paths") {
    ClassScanner.decodePath(
      "/lib/0.0.1%2Btest/xxxx-0.0.1%2Btest.jar"
    ) shouldBe "/lib/0.0.1+test/xxxx-0.0.1+test.jar"
  }

  test("Skip abstract class") {
    // https://github.com/wvlet/airframe/issues/1607
    val cl = classOf[io.grpc.stub.AbstractStub[_]]
    val s  = ReflectSurfaceFactory.ofClass(cl)
    val m  = ReflectSurfaceFactory.methodsOfClass(cl)
  }
}
