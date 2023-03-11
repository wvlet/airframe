package wvlet.airspec.internal.diff

import java.util
import java.util.{Collections, Comparator}

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
