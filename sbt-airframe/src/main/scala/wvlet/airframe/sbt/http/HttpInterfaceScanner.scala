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
package wvlet.airframe.sbt.http
import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.jar.JarFile

import wvlet.log.LogSupport

/**
  *
  */
object HttpInterfaceScanner extends LogSupport {

  def scanClasses(cl: ClassLoader, targetPackageNames: Seq[String]): Seq[String] = {
    def loop(c: ClassLoader): Seq[URL] = {
      c match {
        case null =>
          Seq.empty
        case u: URLClassLoader =>
          u.getURLs.toSeq ++ loop(u.getParent)
        case _ =>
          loop(c.getParent)
      }
    }

    val urls = loop(cl)
    val b    = Seq.newBuilder[String]

    def findClasses(url: URL): Seq[String] = {
      url match {
        case dir if dir.getProtocol == "file" && { val d = new File(dir.getPath); d.exists() && d.isDirectory } =>
          scanClassesInDirectory(dir.getPath, targetPackageNames)
        case jarFile if jarFile.getProtocol == "file" && jarFile.getPath.endsWith(".jar") =>
          scanClassesInJar(jarFile.getPath, targetPackageNames)
        case _ =>
          Seq.empty
      }
    }

    urls.foreach(x => b ++= findClasses(x))

    b.result().filterNot { x => x.contains("$anon$") }.distinct
  }

  private def toFilePath(packageName: String): String = {
    packageName.replaceAll("\\.", "/") + "/"
  }

  private def scanClassesInDirectory(dir: String, targetPackageNames: Seq[String]): Seq[String] = {
    val classes = Seq.newBuilder[String]

    def loop(baseDir: File, f: File): Unit = {
      f match {
        case d: File if f.isDirectory =>
          val files = d.listFiles()
          if (files != null) {
            files.foreach(loop(baseDir, _))
          }
        case f: File if f.getPath.endsWith(".class") =>
          val className = f.getPath
            .stripSuffix(".class").replaceAllLiterally(baseDir.getPath, "").replaceFirst("\\/", "")
            .replaceAll("\\/", ".")

          classes += className
        case _ =>
      }
    }

    val baseDir = new File(dir)
    if (baseDir.exists() && baseDir.isDirectory) {
      val dirs = targetPackageNames.map(toFilePath).map { path => new File(baseDir, path) }
      dirs.foreach(x => loop(baseDir, x))
    }

    classes.result()
  }

  private def scanClassesInJar(jarFile: String, targetPackageNames: Seq[String]): Seq[String] = {
    val jf: JarFile = new JarFile(jarFile)
    val entryEnum   = jf.entries

    val targetPaths = targetPackageNames.map(toFilePath)

    val classes = Seq.newBuilder[String]

    while (entryEnum.hasMoreElements) {
      val jarEntry  = entryEnum.nextElement
      val entryName = jarEntry.getName
      if (entryName.endsWith(".class") && targetPaths.exists(p => entryName.startsWith(p))) {
        val clsName = entryName
          .stripSuffix(".class")
          .replaceAll("\\/", ".")
        classes += clsName
      }
    }

    classes.result()
  }
}
