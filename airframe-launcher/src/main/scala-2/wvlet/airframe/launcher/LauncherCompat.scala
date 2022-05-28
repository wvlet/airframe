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
package wvlet.airframe.launcher

import wvlet.airframe.control.CommandLineTokenizer
import wvlet.airframe.launcher.Launcher.newCommandLauncher
import wvlet.airframe.surface.reflect.ReflectSurfaceFactory

import scala.reflect.runtime.{universe => ru}
import scala.language.experimental.macros

trait LauncherCompat {

  /**
    * Create a new Launcher of the given type
    *
    * @tparam A
    * @return
    */
  def of[A: ru.WeakTypeTag]: Launcher = {
    val cl =
      newCommandLauncher(ReflectSurfaceFactory.of[A], ReflectSurfaceFactory.methodsOf[A], name = "", description = "")
    Launcher(LauncherConfig(), cl)
  }

  def execute[A: ru.WeakTypeTag](argLine: String): A = execute(CommandLineTokenizer.tokenize(argLine))
  def execute[A: ru.WeakTypeTag](args: Array[String]): A = {
    val l      = of[A]
    val result = l.execute(args)
    result.getRootInstance.asInstanceOf[A]
  }
}

trait LauncherBaseCompat { self: Launcher =>

  /**
    * Add a sub command module to the launcher
    *
    * @param name
    *   sub command name
    * @param description
    * @tparam M
    * @return
    */
  def addModule[M: ru.TypeTag](name: String, description: String): Launcher = {
    Launcher(config, mainLauncher.addCommandModule[M](name, description))
  }
}

trait CommandLauncherBaseCompat { self: CommandLauncher =>
  private[launcher] def addCommandModule[B: ru.TypeTag](name: String, description: String): CommandLauncher = {
    val moduleSurface = ReflectSurfaceFactory.ofType(implicitly[ru.TypeTag[B]].tpe)
    val methods       = ReflectSurfaceFactory.methodsOfType(implicitly[ru.TypeTag[B]].tpe)
    val subLauncher   = Launcher.newCommandLauncher(moduleSurface, methods, name, description)
    add(name, description, subLauncher)
  }
}
