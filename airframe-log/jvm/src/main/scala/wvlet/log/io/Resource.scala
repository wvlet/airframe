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
package wvlet.log.io

//--------------------------------------
//
// Resource.scala
// Since: 2012/07/17 9:12
//
//--------------------------------------

import java.io.{BufferedInputStream, File}
import java.lang.reflect.Modifier
import java.net.{URL, URLClassLoader}
import java.util.jar.JarFile

import scala.annotation.tailrec

/**
  * Extend this trait to add support your class for reading resources
  */
trait ResourceReader {
  def open[U](resourceFileName: String)(f: BufferedInputStream => U): U = {
    Resource.open(this.asInstanceOf[AnyRef].getClass, resourceFileName)(f)
  }
}

/**
  * Resource file manager.
  *
  * @author leo
  */
object Resource {

  /**
    * Open a resource as a stream, then execute the code block using the stream
    *
    * @param referenceClass   context class to specify the package containing the resource file
    * @param resourceFileName file name
    * @param body             code block
    * @tparam U
    */
  def open[U](referenceClass: Class[_], resourceFileName: String)(
      body: BufferedInputStream => U): U = {
    val u = find(referenceClass, resourceFileName)
    if (u.isEmpty) {
      sys.error(
        "Resource %s (in %s) not found".format(resourceFileName,
                                               referenceClass.getSimpleName))
    }

    val s = new BufferedInputStream(u.get.openStream())
    try body(s)
    finally s.close
  }

  private def packagePath(referenceClass: Class[_]): String = {
    return packagePath(referenceClass.getPackage)
  }

  private def packagePath(basePackage: Package): String = {
    return packagePath(basePackage.getName)
  }

  private def packagePath(packageName: String): String = {
    val packageAsPath: String = packageName.replaceAll("\\.", "/")
    if (packageAsPath.endsWith("/")) packageAsPath else packageAsPath + "/"
  }

  private def currentClassLoader = Thread.currentThread().getContextClassLoader

  /**
    * Return a URLClassLoader if the given ClassLoader or its parent has the URLClassLoader
    */
  private def findNextURLClassLoader(
      cl: ClassLoader): Option[URLClassLoader] = {
    cl match {
      case u: URLClassLoader => Some(u)
      case null              => None
      case other             => findNextURLClassLoader(other.getParent)
    }
  }

  private def resolveResourcePath(packageName: String,
                                  resourceFileName: String) = {
    val path: String = packagePath(packageName)
    prependSlash(path + resourceFileName)
  }

  private def prependSlash(name: String): String = {
    if (name.startsWith("/")) {
      name
    } else {
      "/" + name
    }
  }

  def find(referenceClass: Class[_], resourceFileName: String): Option[URL] = {
    find(packagePath(referenceClass), resourceFileName)
  }

  /**
    * Find a resource from the give absolute path
    *
    * @param absoluteResourcePath
    * @return
    */
  def find(absoluteResourcePath: String): Option[URL] =
    find(
      "",
      if (absoluteResourcePath.startsWith("/"))
        absoluteResourcePath.substring(1)
      else absoluteResourcePath
    )

  /**
    * Finds the java.net.URL of the resource
    *
    * @param packageName
    * the base package name to find the resource
    * @param resourceFileName
    * the resource file name relative to the package folder
    * @return the URL of the specified resource
    */
  def find(packageName: String, resourceFileName: String): Option[URL] = {
    val resourcePath = resolveResourcePath(packageName, resourceFileName)

    @tailrec
    def loop(cl: ClassLoader): Option[URL] = {
      findNextURLClassLoader(cl) match {
        case Some(urlClassLoader) =>
          urlClassLoader.getResource(resourcePath) match {
            case path: URL =>
              Some(path)
            case _ =>
              loop(urlClassLoader.getParent)
          }
        case None => None
      }
    }

    loop(currentClassLoader) orElse Option(
      this.getClass.getResource(resourcePath))
  }

  /**
    * VirtualFile is a common interface to handle system files and file resources in JAR.
    *
    * System file resources have an URL prefixed with "file:".
    * e.g., "file:/C:/Program Files/Software/classes/org/xerial/util/FileResource.java"
    * JAR file contents have an URL prefixed with "jar:file:
    * e.g., "jar:file:/C:/Program Files/Software/something.jar!/org/xerial/util/FileResource.java"
    *
    * @author leo
    */
  abstract trait VirtualFile {

    /**
      * Gets the logical path of the file.
      * For example, if this VirtualFile' URL is "file:/somewhere/org/xerial/util/FileResource.java",
      * its logical name is org/xerial/util/FileResource.java, beginning from the root package.
      *
      * @return
      */
    def logicalPath: String

    /**
      * is directory?
      *
      * @return true when the file is a directory, otherwise false
      */
    def isDirectory: Boolean

    /**
      * Gets the URL of this file
      *
      * @return
      */
    def url: URL
  }

  /**
    * A virtual file implementation for usual files
    *
    * @author leo
    */
  case class SystemFile(file: java.io.File, logicalPath: String)
      extends VirtualFile {
    def url: URL = file.toURI.toURL

    def isDirectory: Boolean = file.isDirectory
  }

  /**
    * A virtual file implementation for file resources contained in a JAR file
    *
    * @author leo
    */
  case class FileInJar(resourceURL: URL,
                       logicalPath: String,
                       isDirectory: Boolean)
      extends VirtualFile {
    if (resourceURL == null) {
      sys.error("resource URL cannot be null: " + logicalPath)
    }

    def url = resourceURL
  }

  private def extractLogicalName(packagePath: String,
                                 resourcePath: String): String = {
    val p = if (!packagePath.endsWith("/")) packagePath + "/" else packagePath
    val pos: Int = resourcePath.indexOf(p)
    if (pos < 0) return null
    val logicalName: String = resourcePath.substring(pos + p.length)
    logicalName
  }

  private def collectFileResources(
      resourceURLString: String,
      packagePath: String,
      resourceFilter: String => Boolean
  ): Seq[VirtualFile] = {
    val logicalName = extractLogicalName(packagePath, resourceURLString)
    if (logicalName == null) {
      throw new IllegalArgumentException(
        "packagePath=" + packagePath + ", resourceURL=" + resourceURLString)
    }

    val b = Seq.newBuilder[VirtualFile]
    val file: File = new File(new URL(resourceURLString).toURI)
    if (resourceFilter(file.getPath)) {
      b += SystemFile(file, logicalName)
    }
    if (file.isDirectory) {
      for (childFile <- file.listFiles) {
        val childResourceURL =
          resourceURLString + (if (resourceURLString.endsWith("/")) "" else "/") + childFile.getName
        b ++= collectFileResources(childResourceURL,
                                   packagePath,
                                   resourceFilter)
      }
    }
    b.result()
  }

  /**
    * Create a list of all resources under the given resourceURL recursively. If the
    * resourceURL is a file, this method searches directories under the path. If the resource is contained
    * in a Jar file, it searches contents of the Jar file.
    *
    * @param resourceURL
    * @param packageName package name under consideration
    * @param resourceFilter
    * @return the list of resources matching the given resource filter
    */
  private def listResources(
      resourceURL: URL,
      packageName: String,
      resourceFilter: String => Boolean
  ): Seq[VirtualFile] = {
    val pkgPath = packagePath(packageName)
    val fileList = Seq.newBuilder[VirtualFile]
    if (resourceURL == null) {
      return Seq.empty
    }

    val protocol = resourceURL.getProtocol
    if (protocol == "file") {
      val resourceURLString = resourceURL.toString
      fileList ++= collectFileResources(resourceURLString,
                                        pkgPath,
                                        resourceFilter)
    } else if (protocol == "jar") {
      val path: String = resourceURL.getPath
      val pos: Int = path.indexOf("!")
      if (pos < 0) {
        throw new IllegalArgumentException(
          "invalid resource URL: " + resourceURL)
      }

      val jarPath = path.substring(0, pos).replaceAll("%20", " ")
      val filePath =
        path
          .substring(0, pos)
          .replaceAll("%20", " ")
          .replaceAll("%25", "%") // %25 => %
          .replace("file:", "")
      val jarURLString = "jar:" + jarPath
      val jf: JarFile = new JarFile(filePath)
      val entryEnum = jf.entries
      while (entryEnum.hasMoreElements) {
        val jarEntry = entryEnum.nextElement
        val physicalURL = jarURLString + "!/" + jarEntry.getName
        val jarFileURL = new URL(physicalURL)
        val logicalName = extractLogicalName(pkgPath, jarEntry.getName)
        if (logicalName != null && resourceFilter(logicalName)) {
          fileList += FileInJar(jarFileURL, logicalName, jarEntry.isDirectory)
        }
      }
    } else {
      throw new UnsupportedOperationException(
        "resources other than file or jar are not supported: " + resourceURL)
    }

    fileList.result()
  }

  /**
    * Collect resources under the given package
    *
    * @param packageName
    * @return
    */
  def listResources(packageName: String): Seq[VirtualFile] =
    listResources(
      packageName, { (f: String) =>
        true
      }
    )

  /**
    * Collect resources under the given package
    *
    * @param classLoader
    * @param packageName
    * @param resourceFilter
    * @return
    */
  def listResources(
      packageName: String,
      resourceFilter: String => Boolean,
      classLoader: ClassLoader = Thread.currentThread.getContextClassLoader
  ): Seq[VirtualFile] = {
    val b = Seq.newBuilder[VirtualFile]
    for (u <- findResourceURLs(classLoader, packageName)) {
      b ++= listResources(u, packageName, resourceFilter)
    }
    b.result()
  }

  /**
    * Find resource URLs that can be found from a given class loader and its ancestors
    *
    * @param cl   class loader
    * @param name resource name
    * @return
    */
  def findResourceURLs(cl: ClassLoader, name: String): Seq[URL] = {
    val path = packagePath(name)
    val b = Seq.newBuilder[URL]

    @tailrec
    def loop(cl: ClassLoader): Unit = {
      findNextURLClassLoader(cl) match {
        case Some(urlClassLoader) =>
          val e = urlClassLoader.findResources(path)
          while (e.hasMoreElements) {
            val elem = e.nextElement()
            b += elem
          }
          loop(urlClassLoader.getParent)
        case None =>
        // Do nothing
      }
    }

    loop(currentClassLoader)
    b.result()
  }

  def findClasses[A](
      packageName: String,
      toSearch: Class[A],
      classLoader: ClassLoader = Thread.currentThread.getContextClassLoader
  ): Seq[Class[A]] = {
    val classFileList = listResources(
      packageName, { (f: String) =>
        f.endsWith(".class")
      },
      classLoader
    )

    def componentName(path: String): Option[String] = {
      val dot: Int = path.lastIndexOf(".")
      if (dot <= 0) {
        None
      } else {
        Some(path.substring(0, dot).replaceAll("/", "."))
      }
    }
    def findClass(name: String): Option[Class[_]] = {
      try Some(Class.forName(name, false, classLoader))
      catch {
        case e: ClassNotFoundException => None
      }
    }

    val b = Seq.newBuilder[Class[A]]
    for (vf <- classFileList; cn <- componentName(vf.logicalPath)) {
      val className: String = packageName + "." + cn
      for (cl <- findClass(className)) {
        if (!Modifier.isAbstract(cl.getModifiers) && toSearch.isAssignableFrom(
              cl)) {
          b += cl.asInstanceOf[Class[A]]
        }
      }
    }
    b.result()
  }

  def findClasses[A](searchPath: Package,
                     toSearch: Class[A],
                     classLoader: ClassLoader): Seq[Class[A]] = {
    findClasses(searchPath.getName, toSearch, classLoader)
  }
}
