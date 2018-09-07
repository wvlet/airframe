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
package wvlet.surface
import wvlet.surface

object RecursiveHigherKindTypeTest {
  trait UserAccountRepository[M[_]]

  trait MyTask[A]

  object UserAccountRepository {
    type BySkinny[A] = MyTask[A]
    def bySkinny: UserAccountRepository[BySkinny] = new UserAccountRepositoryBySkinny
  }

  import UserAccountRepository._
  class UserAccountRepositoryBySkinny extends UserAccountRepository[BySkinny] {}
}

/**
  *
  */
class RecursiveHigherKindTypeTest extends SurfaceSpec {
  import RecursiveHigherKindTypeTest._
  import UserAccountRepository.BySkinny

  "Surface" should {
    "support recursive higher kind types" in {
      val s = surface.of[UserAccountRepository[BySkinny]]
      debug(s)
    }
  }
}
