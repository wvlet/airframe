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
package wvlet.airframe.control
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.reflect.Field

import wvlet.log.LogSupport

import scala.collection.mutable.WeakHashMap
import scala.sys.process.{Process, ProcessLogger}
import scala.jdk.CollectionConverters._

/**
  * Launch UNIX (or cygwin) commands from Scala
  */
object Shell extends LogSupport {
  private def withAccessTo[U](f: Field)(body: => U): U = {
    val a = f.isAccessible
    try {
      if (!a)
        f.setAccessible(true)
      body
    } finally {
      if (!a)
        f.setAccessible(a)
    }
  }

  /**
    * Kills the process
    * @param pid
    * @return
    */
  def kill(pid: Int, signal: String = "TERM"): Int = {
    val p = launchProcess("kill -%s %d".format(signal, pid))
    p.waitFor()
    val exitCode = p.exitValue()
    debug(s"killed process $pid with exit code $exitCode")
    exitCode
  }

  /**
    * Kill the process tree rooted from pid
    * @param pid
    * @return exit code
    */
  def killTree(pid: Int, signal: String = "TERM"): Int = {
    // stop the parent process first to keep it from forking another child process
    exec(f"kill -STOP $pid%d")

    // retrieve child processes
    val pb = prepareProcessBuilder(s"ps -o pid -p $pid | sed 1d", inheritIO = true)
    for (line <- Process(pb).lineStream_!) {
      val childPID = line.trim.toInt
      killTree(childPID, signal)
    }

    // Now send the signal
    exec(s"kill -$signal $pid")
    // Try and continue the process in case the signal is non-terminating
    // but doesn't continue the process
    exec(s"kill -CONT $pid")
  }

  def escape(s: String): String = {
    val r      = """([^\\])(\")""".r
    val b      = new StringBuilder
    var cursor = 0
    for (m <- r.findAllIn(s).matchData) {
      b append s.substring(cursor, m.start)
      b append m.group(2)
      cursor = m.end
    }
    b append s.substring(cursor)
    b.result
  }

  def unescape(s: String): String = {
    val r      = """(\\)([\\/\\"bfnrt])""".r
    val b      = new StringBuilder
    var cursor = 0
    for (m <- r.findAllIn(s).matchData) {
      b append s.substring(cursor, m.start)
      b append m.group(2)
      cursor = m.end
    }
    b append s.substring(cursor)
    b.result
  }

  /**
    * launch a command in the remote host. The target host needs to be accessed
    * via ssh command without password.
    * @param host
    * @param cmdLine
    */
  def launchRemoteDaemon(host: String, cmdLine: String): Unit = {
    execRemote(host, s"$cmdLine < /dev/null > /dev/null &")
  }

  /**
    * Return the process ID of the current JVM.
    *
    * @return process id or -1 when process ID is not found.
    */
  def getProcessIDOfCurrentJVM: Int = {
    val r = ManagementFactory.getRuntimeMXBean
    // This value is ought to be (process id)@(host name)
    val n = r.getName
    n.split("@").headOption.map { _.toInt } getOrElse { -1 }
  }

  /**
    * Returns process id
    * @param p
    * @return process id or -1 if pid cannot be detected
    */
  def getProcessID(p: java.lang.Process): Int = {
    try {
      // If the current OS is *Nix, the class of p is UNIXProcess and its pid can be obtained
      // from pid field by using reflection.
      val f = p.getClass().getDeclaredField("pid")
      val pid: Int = withAccessTo(f) {
        f.get(p).asInstanceOf[Int]
      }
      pid
    } catch {
      case e: Throwable => -1
    }
  }

  def launchJava(args: String) = {
    val javaCmd = Shell.findJavaCommand()
    if (javaCmd.isEmpty)
      throw new IllegalStateException("No JVM is found. Set JAVA_HOME environmental variable")

    val cmdLine = "%s %s".format(javaCmd.get, args)
    launchProcess(cmdLine)
  }

  /**
    * Launch a process then retrieves the exit code
    * @param cmdLine
    * @return
    */
  def exec(cmdLine: String): Int = {
    val pb       = prepareProcessBuilder(cmdLine, inheritIO = true)
    val exitCode = Process(pb).!(ProcessLogger { out: String => info(out) })
    debug(s"exec command $cmdLine with exitCode:$exitCode")
    exitCode
  }

  def launchProcess(cmdLine: String) = {
    val pb = prepareProcessBuilder(cmdLine, inheritIO = true)
    val p  = pb.start
    debug(s"exec command [pid:${getProcessID(p)}] ${pb.command.asScala.mkString(" ")}")
    p
  }

  def execRemote(hostname: String, cmdLine: String): Int = {
    val pb       = prepareProcessBuilderFromSeq(Seq("ssh", hostname, quote(cmdLine)), inheritIO = true)
    val exitCode = Process(pb).!(ProcessLogger { out: String => info(out) })
    debug(s"exec command $cmdLine with exitCode:$exitCode")
    exitCode
  }

  private def quote(s: String): String = {
    s.replaceAll("""\"""", """\\"""")
  }

  def prepareProcessBuilder(cmdLine: String, inheritIO: Boolean): ProcessBuilder = {
    trace(s"cmdLine: $cmdLine")
    val tokens = Array(Shell.getCommand("sh"), "-c", if (OS.isWindows) quote(cmdLine) else cmdLine)
    prepareProcessBuilderFromSeq(tokens.toIndexedSeq, inheritIO)
  }

  def prepareProcessBuilderFromSeq(tokens: Seq[String], inheritIO: Boolean): ProcessBuilder = {
    trace(s"command line tokens: ${tokens.mkString(", ")}")
    val pb = new ProcessBuilder(tokens: _*)
    if (inheritIO)
      pb.inheritIO()
    var env = getEnv
    if (OS.isWindows)
      env += ("CYGWIN" -> "notty")
    val envMap = pb.environment()
    env.foreach(e => envMap.put(e._1, e._2))
    pb
  }

  def getEnv: Map[String, String] = {
    System.getenv().asScala.toMap
  }

  def launchCmdExe(cmdLine: String) = {
    val c = "%s /c \"%s\"".format(Shell.getCommand("cmd"), cmdLine)
    debug(s"exec command: $c")
    Process(CommandLineTokenizer.tokenize(c), None, getEnv.toSeq: _*).run
  }

  // command name -> path
  private val cmdPathCache = new WeakHashMap[String, Option[String]]

  def getCommand(name: String): String = {
    findCommand(name) match {
      case Some(cmd) => cmd
      case None      => throw new IllegalStateException("CommandTrait not found: %s".format(name))
    }
  }

  def findSh: Option[String] = {
    findCommand("sh")
  }

  def getExecPath = {
    val path = (System.getenv("PATH") match {
      case null => ""
      case x    => x.toString
    }).split("[;:]")
    path
  }

  /**
    * Return OS-dependent program name. (e.g., sh in Unix, sh.exe in Windows)
    */
  def progName(p: String): String = {
    if (OS.isWindows)
      p + ".exe"
    else
      p
  }

  def findCommand(name: String): Option[String] = {
    cmdPathCache.getOrElseUpdate(
      name, {
        val path = {
          if (OS.isWindows)
            getExecPath ++ Seq("c:/cygwin/bin")
          else
            getExecPath
        }
        val prog = progName(name)

        val exe = path.map(new File(_, prog)).find(_.exists).map(_.getAbsolutePath)
        trace {
          if (exe.isDefined) "%s is found at %s".format(name, exe.get)
          else "%s is not found".format(name)
        }
        exe
      }
    )
  }

  def sysProp(key: String): Option[String] = Option(System.getProperty(key))
  def env(key: String): Option[String]     = Option(System.getenv(key))

  def findJavaHome: Option[String] = {
    // lookup environment variable JAVA_HOME first.
    // If JAVA_HOME is not defined, use java.home system property
    val e: Option[String] = env("JAVA_HOME") orElse sysProp("java.home")

    def resolveCygpath(p: String): String = {
      if (OS.isWindows) {
        // If the path is for Cygwin environment
        val m = """/cygdrive/(\w)(/.*)""".r.findFirstMatchIn(p)
        if (m.isDefined)
          "%s:%s".format(m.get.group(1), m.get.group(2))
        else
          p
      } else
        p
    }

    val p = e.map(resolveCygpath(_))
    debug(s"Found JAVA_HOME=${p.get}")
    p
  }

  def findJavaCommand(javaCmdName: String = "java"): Option[String] = {
    def search: Option[String] = {
      def javaBin(java_home: String): String = java_home + "/bin/" + Shell.progName(javaCmdName)

      def hasJavaCommand(java_home: String): Boolean = {
        val java_path = new File(javaBin(java_home))
        java_path.exists()
      }

      val java_home: Option[String] = {
        val e = findJavaHome

        import OSType._
        e match {
          case Some(x) => e
          case None => {
            def listJDKIn(path: String) = {
              // TODO Oracle JVM (JRockit) support
              new File(path)
                .listFiles().filter(x =>
                  x.isDirectory
                    && (x.getName.startsWith("jdk") || x.getName.startsWith("jre"))
                    && hasJavaCommand(x.getAbsolutePath)
                ).map(_.getAbsolutePath)
            }
            def latestJDK(jdkPath: Array[String]): Option[String] = {
              if (jdkPath.isEmpty)
                None
              else {
                // TODO parse version number
                val sorted = jdkPath.sorted.reverse
                Some(sorted(0))
              }
            }
            debug("No java command found. Searching for JDK...")

            OS.getType match {
              case Windows => latestJDK(listJDKIn("c:/Program Files/Java"))
              case Mac => {
                val l =
                  Seq(
                    "/System/Library/Frameworkds/JavaVM.framework/Home",
                    "/System/Library/Frameworkds/JavaVM.framework/Versions/CurrentJDK/Home"
                  ).filter(hasJavaCommand)
                if (l.isEmpty)
                  None
                else
                  Some(l(0))
              }
              case _ => None
            }
          }
        }
      }

      val ret: Option[String] = java_home match {
        case Some(x) => Some(javaBin(x).trim)
        case None => {
          val javaPath = Process("which %s".format(javaCmdName)).!!.trim
          if (javaPath.isEmpty)
            None
          else
            Some(javaPath)
        }
      }
      ret
    }

    cmdPathCache.getOrElseUpdate(javaCmdName, search)
  }
}
