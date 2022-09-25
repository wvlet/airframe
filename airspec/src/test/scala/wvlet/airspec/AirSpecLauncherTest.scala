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
package wvlet.airspec

/**
  */
class AirSpecLauncherTest extends AirSpec {
  test("run main") {
    AirSpecLauncher.execute(Array.empty)
  }

  test("run --help") {
    AirSpecLauncher.execute(Array("--help"))
  }

  test("run test without arg") {
    AirSpecLauncher.execute(Array("test"))
  }

  test("run test") {
    AirSpecLauncher.execute(Array("test", "examples.CommonSpec"))
  }

  test("run tests with args") {
    AirSpecLauncher.execute(Array("test", "examples.CommonSpec", "hello"))
  }

  test("try to run test with non-existing test") {
    intercept[IllegalArgumentException] {
      AirSpecLauncher.main(Array("test", "dummy.NonExistingSpec"))
    }
  }

  test("run a test extending object") {
    AirSpecLauncher.execute(Array("test", "examples.ObjectSpec"))
  }
}
