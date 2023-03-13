package wvlet.airspec.internal.diff

import wvlet.airspec.internal.PrettyPrint

import java.util
import java.util.{Collections, Comparator}
import scala.collection.JavaConverters._

class Diff(val obtained: String, val expected: String) extends Serializable {
  val obtainedClean: String = AnsiColors.filterAnsi(obtained)
  val expectedClean: String = AnsiColors.filterAnsi(expected)
  val obtainedLines: Seq[String] = splitIntoLines(obtainedClean)
  val expectedLines: Seq[String] = splitIntoLines(expectedClean)
  val unifiedDiff: String = createUnifiedDiff(obtainedLines, expectedLines)
  def isEmpty: Boolean = unifiedDiff.isEmpty()

  def createReport(
                    title: String,
                    printObtainedAsStripMargin: Boolean = true
                  ): String = {
    val sb = new StringBuilder
    if (title.nonEmpty) {
      sb.append(title)
        .append("\n")
    }
    if (obtainedClean.length < 1000) {
      header("Obtained", sb).append("\n")
      if (printObtainedAsStripMargin) {
        sb.append(asStripMargin(obtainedClean))
      } else {
        sb.append(obtainedClean)
      }
      sb.append("\n")
    }
    appendDiffOnlyReport(sb)
    sb.toString()
  }

  def createDiffOnlyReport(): String = {
    val out = new StringBuilder
    appendDiffOnlyReport(out)
    out.toString()
  }

  private def appendDiffOnlyReport(sb: StringBuilder): Unit = {
    header("Diff", sb)
    sb.append(
      s" (${AnsiColors.LightRed}- obtained${AnsiColors.Reset}, ${AnsiColors.LightGreen}+ expected${AnsiColors.Reset})"
    ).append("\n")
    sb.append(unifiedDiff)
  }

  private def asStripMargin(obtained: String): String = {
    if (!obtained.contains("\n")) {
      PrettyPrint.pp(obtained)
    } else {
      val out = new StringBuilder
      val lines = obtained.trim.linesIterator
      val head = if (lines.hasNext) lines.next() else ""
      out.append("    \"\"\"|" + head + "\n")
      lines.foreach(line => {
        out.append("       |").append(line).append("\n")
      })
      out.append("       |\"\"\".stripMargin")
      out.toString()
    }
  }

  private def header(t: String, sb: StringBuilder): StringBuilder = {
    sb.append(AnsiColors.c(s"=> $t", AnsiColors.Bold))
  }

  private def createUnifiedDiff(
                                 original: Seq[String],
                                 revised: Seq[String]
                               ): String = {
    val diff = DiffUtils.diff(original.asJava, revised.asJava)
    val result =
      if (diff.getDeltas.isEmpty) ""
      else {
        DiffUtils
          .generateUnifiedDiff(
            "obtained",
            "expected",
            original.asJava,
            diff,
            1
          )
          .asScala
          .iterator
          .drop(2)
          .filterNot(_.startsWith("@@"))
          .map { line =>
            if (line.isEmpty()) line
            else if (line.last == ' ') line + "∙"
            else line
          }
          .map { line =>
            if (line.startsWith("-")) AnsiColors.c(line, AnsiColors.LightRed)
            else if (line.startsWith("+"))
              AnsiColors.c(line, AnsiColors.LightGreen)
            else line
          }
          .mkString("\n")
      }
    result
  }

  private def splitIntoLines(string: String): Seq[String] = {
    string.trim().replace("\r\n", "\n").split("\n").toIndexedSeq
  }
}


object DiffUtils {
  def generateUnifiedDiff(
                           original: String,
                           revised: String,
                           originalLines: util.List[String],
                           patch: Patch[String],
                           contextSize: Int
                         ): util.List[String] = {
    if (!patch.getDeltas.isEmpty) {
      val ret: util.List[String] = new util.ArrayList()
      ret.add("--- " + original)
      ret.add("+++ " + revised)
      val patchDeltas: util.List[Delta[String]] =
        new util.ArrayList(patch.getDeltas)
      val deltas: util.List[Delta[String]] = new util.ArrayList()

      var delta = patchDeltas.get(0)
      deltas.add(delta)

      if (patchDeltas.size() > 1) {
        for (i <- 1 until patchDeltas.size) {
          val position = delta.getOriginal.getPosition // store
          // the
          // current
          // position
          // of
          // the first Delta
          // Check if the next Delta is too close to the current
          // position.
          // And if it is, add it to the current set
          val nextDelta = patchDeltas.get(i)
          if (
            (position + delta.getOriginal.size + contextSize) >=
              (nextDelta.getOriginal.getPosition - contextSize)
          ) {
            deltas.add(nextDelta)
          } else { // if it isn't, output the current set,
            // then create a new set and add the current Delta to
            // it.
            val curBlock = processDeltas(originalLines, deltas, contextSize)
            ret.addAll(curBlock)
            deltas.clear()
            deltas.add(nextDelta)
          }
          delta = nextDelta
        }
      }
      val curBlock = processDeltas(originalLines, deltas, contextSize)
      ret.addAll(curBlock)
      ret
    } else {
      new util.ArrayList[String]()
    }
  }
  def diff(
            original: util.List[String],
            revised: util.List[String]
          ): Patch[String] =
    new MyersDiff[String]().diff(original, revised)

  private def processDeltas(
                             origLines: util.List[String],
                             deltas: util.List[Delta[String]],
                             contextSize: Int
                           ) = {
    val buffer = new util.ArrayList[String]
    var origTotal = 0 // counter for total lines output from Original
    var revTotal = 0
    var line = 0
    var curDelta = deltas.get(0)
    // NOTE: +1 to overcome the 0-offset Position
    var origStart = curDelta.getOriginal.getPosition + 1 - contextSize
    if (origStart < 1) origStart = 1
    var revStart = curDelta.getRevised.getPosition + 1 - contextSize
    if (revStart < 1) revStart = 1
    // find the start of the wrapper context code
    var contextStart = curDelta.getOriginal.getPosition - contextSize
    if (contextStart < 0) contextStart = 0 // clamp to the start of the file
    // output the context before the first Delta
    line = contextStart
    while ({
      line < curDelta.getOriginal.getPosition
    }) { //
      buffer.add(" " + origLines.get(line))
      origTotal += 1
      revTotal += 1

      line += 1
    }
    // output the first Delta
    buffer.addAll(getDeltaText(curDelta))
    origTotal += curDelta.getOriginal.getLines.size
    revTotal += curDelta.getRevised.getLines.size
    var deltaIndex = 1
    while ({
      deltaIndex < deltas.size
    }) { // for each of the other Deltas
      val nextDelta = deltas.get(deltaIndex)
      val intermediateStart =
        curDelta.getOriginal.getPosition + curDelta.getOriginal.getLines.size
      line = intermediateStart
      while ({
        line < nextDelta.getOriginal.getPosition
      }) { // output the code between the last Delta and this one
        buffer.add(" " + origLines.get(line))
        origTotal += 1
        revTotal += 1

        line += 1
      }
      buffer.addAll(getDeltaText(nextDelta)) // output the Delta

      origTotal += nextDelta.getOriginal.getLines.size
      revTotal += nextDelta.getRevised.getLines.size
      curDelta = nextDelta
      deltaIndex += 1
    }
    // Now output the post-Delta context code, clamping the end of the file
    contextStart =
      curDelta.getOriginal.getPosition + curDelta.getOriginal.getLines.size
    line = contextStart
    while ({
      (line < (contextStart + contextSize)) && (line < origLines.size)
    }) {
      buffer.add(" " + origLines.get(line))
      origTotal += 1
      revTotal += 1

      line += 1
    }
    // Create and insert the block header, conforming to the Unified Diff
    // standard
    val header = new StringBuffer
    header.append("@@ -")
    header.append(origStart)
    header.append(",")
    header.append(origTotal)
    header.append(" +")
    header.append(revStart)
    header.append(",")
    header.append(revTotal)
    header.append(" @@")
    buffer.add(0, header.toString)
    buffer
  }

  private def getDeltaText(delta: Delta[String]) = {
    import scala.collection.JavaConverters._
    val buffer = new util.ArrayList[String]
    for (line <- delta.getOriginal.getLines.asScala) {
      buffer.add("-" + line)
    }
    for (line <- delta.getRevised.getLines.asScala) {
      buffer.add("+" + line)
    }
    buffer
  }

}

trait DiffAlgorithm[T] {
  def diff(original: util.List[T], revised: util.List[T]): Patch[T]
}

class Chunk[T](position: Int, lines: util.List[T]) {

  def getPosition: Int = position
  def getLines: util.List[T] = lines
  def size: Int = lines.size()

  override def toString: String = s"Chunk($getPosition, $getLines, $size)"
}

sealed abstract class Delta[T](original: Chunk[T], revised: Chunk[T]) {

  sealed abstract class TYPE
  object TYPE {
    case object CHANGE extends TYPE
    case object DELETE extends TYPE
    case object INSERT extends TYPE
  }
  def getType: TYPE
  def getOriginal: Chunk[T] = original
  def getRevised: Chunk[T] = revised

  override def toString: String = s"Delta($getType, $getOriginal, $getRevised)"
}
class ChangeDelta[T](original: Chunk[T], revised: Chunk[T])
  extends Delta(original, revised) {
  override def getType: TYPE = TYPE.CHANGE
}
class InsertDelta[T](original: Chunk[T], revised: Chunk[T])
  extends Delta(original, revised) {
  override def getType: TYPE = TYPE.INSERT
}
class DeleteDelta[T](original: Chunk[T], revised: Chunk[T])
  extends Delta(original, revised) {
  override def getType: TYPE = TYPE.DELETE
}


class Patch[T] {
  private val deltas: util.List[Delta[T]] = new util.ArrayList()
  private val comparator: Comparator[Delta[T]] = new Comparator[Delta[T]] {
    override def compare(o1: Delta[T], o2: Delta[T]): Int =
      o1.getOriginal.getPosition.compareTo(o2.getOriginal.getPosition)
  }
  def addDelta(delta: Delta[T]): Unit = {
    deltas.add(delta)
  }
  def getDeltas: util.List[Delta[T]] = {
    Collections.sort(deltas, comparator)
    deltas
  }

  override def toString: String = s"Patch($deltas)"
}

sealed abstract class PathNode(val i: Int, val j: Int, val prev: PathNode) {

  def isSnake: Boolean
  final def isBootstrap: Boolean = {
    i < 0 || j < 0
  }
  final def previousSnake: PathNode = {
    if (isBootstrap) null
    else if (!isSnake && prev != null) prev.previousSnake
    else this
  }

  override def toString: String = {
    val buf = new StringBuffer("[")
    var node = this
    while (node != null) {
      buf.append("(")
      buf.append(Integer.toString(node.i))
      buf.append(",")
      buf.append(Integer.toString(node.j))
      buf.append(")")
      node = node.prev
    }
    buf.append("]")
    buf.toString
  }
}

final class DiffNode(i: Int, j: Int, prev: PathNode)
  extends PathNode(i, j, if (prev == null) null else prev.previousSnake) {
  override def isSnake: Boolean = false
}

final class Snake(i: Int, j: Int, prev: PathNode) extends PathNode(i, j, prev) {
  override def isSnake: Boolean = true
}

trait Equalizer[T] {
  def equals(original: T, revised: T): Boolean
}
object Equalizer {
  def default[T]: Equalizer[T] = new Equalizer[T] {
    override def equals(original: T, revised: T): Boolean = {
      original.equals(revised)
    }
  }
}

import wvlet.airframe.SourceCode

trait ComparisonFailExceptionHandler {
  def handle(
              message: String,
              obtained: String,
              expected: String,
              location: SourceCode
            ): Nothing
}

class MyersDiff[T](equalizer: Equalizer[T])
  extends DiffAlgorithm[T] {
  def this() = this(Equalizer.default[T])
  override def diff(
                     original: util.List[T],
                     revised: util.List[T]
                   ): Patch[T] = {
    try {
      buildRevision(buildPath(original, revised), original, revised)
    } catch {
      case e: IllegalStateException =>
        e.printStackTrace()
        new Patch[T]()
    }
  }
  private def buildRevision(
                             _path: PathNode,
                             orig: util.List[T],
                             rev: util.List[T]
                           ): Patch[T] = {
    var path = _path
    val patch = new Patch[T]
    if (path.isSnake) path = path.prev
    while (
      path != null &&
        path.prev != null &&
        path.prev.j >= 0
    ) {
      if (path.isSnake)
        throw new IllegalStateException(
          "bad diffpath: found snake when looking for diff"
        )
      val i = path.i
      val j = path.j
      path = path.prev
      val ianchor = path.i
      val janchor = path.j
      val original =
        new Chunk[T](
          ianchor,
          copyOfRange(orig, ianchor, i)
        )
      val revised =
        new Chunk[T](
          janchor,
          copyOfRange(rev, janchor, j)
        )
      val delta: Delta[T] =
        if (original.size == 0 && revised.size != 0) {
          new InsertDelta[T](original, revised)
        } else if (original.size > 0 && revised.size == 0) {
          new DeleteDelta[T](original, revised)
        } else {
          new ChangeDelta[T](original, revised)
        }
      patch.addDelta(delta)
      if (path.isSnake) {
        path = path.prev
      }
    }
    patch
  }

  private def copyOfRange(original: util.List[T], fromIndex: Int, to: Int) =
    new util.ArrayList[T](original.subList(fromIndex, to))

  def buildPath(
                 orig: util.List[T],
                 rev: util.List[T]
               ): PathNode = {

    val N = orig.size()
    val M = rev.size()

    val MAX = N + M + 1
    val size = 1 + 2 * MAX
    val middle = size / 2
    val diagonal = new Array[PathNode](size)

    diagonal(middle + 1) = new Snake(0, -1, null)
    var d = 0
    while (d < MAX) {
      var k = -d
      while (k <= d) {
        val kmiddle = middle + k
        val kplus = kmiddle + 1
        val kminus = kmiddle - 1
        var prev: PathNode = null
        var i = 0
        if ((k == -d) || (k != d && diagonal(kminus).i < diagonal(kplus).i)) {
          i = diagonal(kplus).i
          prev = diagonal(kplus)
        } else {
          i = diagonal(kminus).i + 1
          prev = diagonal(kminus)
        }
        diagonal(kminus) = null // no longer used

        var j = i - k
        var node: PathNode = new DiffNode(i, j, prev)
        // orig and rev are zero-based
        // but the algorithm is one-based
        // that's why there's no +1 when indexing the sequences
        while (
          i < N &&
            j < M &&
            equalizer.equals(orig.get(i), rev.get(j))
        ) {
          i += 1
          j += 1
        }
        if (i > node.i) {
          node = new Snake(i, j, node)
        }
        diagonal(kmiddle) = node
        if (i >= N && j >= M) {
          return diagonal(kmiddle)
        }

        k += 2
      }
      diagonal(middle + d - 1) = null
      d += 1
    }
    // According to Myers, this cannot happen
    throw new IllegalStateException("could not find a diff path")
  }
}


object AnsiColors {
  val LightRed = "\u001b[91m"
  val LightGreen = "\u001b[92m"
  val Reset = "\u001b[0m"
  val Reversed = "\u001b[7m"
  val Bold = "\u001b[1m"
  val Faint = "\u001b[2m"
  val RED = "\u001B[31m"
  val YELLOW = "\u001B[33m"
  val BLUE = "\u001B[34m"
  val Magenta = "\u001B[35m"
  val CYAN = "\u001B[36m"
  val GREEN = "\u001B[32m"
  val DarkGrey = "\u001B[90m"

  def c(s: String, colorSequence: String): String =
    if (colorSequence == null) s
    else colorSequence + s + Reset

  def filterAnsi(s: String): String = {
    if (s == null) {
      null
    } else {
      val len = s.length
      val r = new java.lang.StringBuilder(len)
      var i = 0
      while (i < len) {
        val c = s.charAt(i)
        if (c == '\u001B') {
          i += 1
          while (i < len && s.charAt(i) != 'm') i += 1
        } else {
          r.append(c)
        }
        i += 1
      }
      r.toString()
    }
  }

}
