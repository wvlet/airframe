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
import wvlet.airframe.surface.Surface

trait LauncherCompat {

  /**
    * Create a new Launcher of the given type
    *
    * @tparam A
    * @return
    */
  inline def of[A]: Launcher = {
    val cl = newCommandLauncher(Surface.of[A], Surface.methodsOf[A], name = "", description = "")
    Launcher(LauncherConfig(), cl)
  }

  inline def execute[A](argLine: String): A = execute[A](CommandLineTokenizer.tokenize(argLine))
  inline def execute[A](args: Array[String]): A = {
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
  inline def addModule[M](name: String, description: String): Launcher = {
    Launcher(config, mainLauncher.addCommandModule[M](name, description))
  }
}

trait CommandLauncherBaseCompat { self: CommandLauncher =>
  inline private[launcher] def addCommandModule[B](name: String, description: String): CommandLauncher = {
    val subLauncher = Launcher.newCommandLauncher(Surface.of[B], Surface.methodsOf[B], name, description)
    add(name, description, subLauncher)
  }
}
