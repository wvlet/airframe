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

//--------------------------------------
//
// CommandModule.scala
// Since: 2012/11/08 5:51 PM
//
//--------------------------------------

package wvlet.airframe.opts

/**
  *
  * {{{
  * // Define your command set
  * class MyCommandSet {
  *    \@command
  *    def hello = println("hello")
  *
  *    \@command
  *    def world = println("world")
  * }
  *
  * // Integrate the above command set as a module with a given name.
  * // Command can be invoked as "sample hello" and "sample world".
  * class MyModule extends CommandModule {
  *   def modules = Seq(ModuleDef("sample", classOf[MyCommandSet], description="my command set"))
  * }
  *
  * Launcher[MyModule].execute("sample hello") // prints hello
  *
  * }}}
  *
  *
  * @author leo
  */
trait CommandModule {
  def modules: Seq[ModuleDef[_]]

  /**
    * Place holder for the executed module
    */
  var executedModule: Option[(String, AnyRef)] = None

}

case class ModuleDef[A](name: String, moduleClass: Class[A], description: String = "")

object CommandModule {
  def isModuleClass[A](cl: Class[A]) = classOf[CommandModule].isAssignableFrom(cl)

}
