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
package wvlet.airframe

import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airspec.AirSpec

class JSR250Test {
  var initialized      = false
  var stopped          = false
  var stoppedCallCount = 0

  @PostConstruct
  def init: Unit = {
    initialized = true
  }

  @PreDestroy
  def stop: Unit = {
    stopped = true
    stoppedCallCount += 1
  }
}

class InheritedHookTest extends JSR250Test

class HookOverrideTest extends JSR250Test {
  var parentDestoyHookIsCalled = false

  @PreDestroy
  override def stop: Unit = {
    stopped = true
    parentDestoyHookIsCalled = true
    stoppedCallCount += 1
  }
}

/**
  */
class JSR250LifeCycleExecutorTest extends AirSpec {
  test("support JSR250 event") {
    val s = newSilentDesign.newSession

    val t = s.build[JSR250Test]
    t.initialized shouldBe true
    t.stopped shouldBe false
    s.start {}
    t.initialized shouldBe true
    t.stopped shouldBe true
  }

  test("support inherited JSR250 annotations") {
    val s = newSilentDesign.newSession

    val t = s.build[InheritedHookTest]
    t.initialized shouldBe true
    t.stopped shouldBe false
    s.start {}
    t.initialized shouldBe true
    t.stopped shouldBe true
  }

  test("do not call overwritten hooks") {
    val s = newSilentDesign.newSession

    val t = s.build[HookOverrideTest]
    t.initialized shouldBe true
    t.stopped shouldBe false
    t.parentDestoyHookIsCalled shouldBe false
    s.start {}
    t.initialized shouldBe true
    t.stopped shouldBe true
    t.stoppedCallCount shouldBe 1
    t.parentDestoyHookIsCalled shouldBe true
  }
}
