package wvlet.airframe.tablet.text

import wvlet.airframe.tablet.msgpack.MessageCodec
import wvlet.airframe.tablet.obj.ObjectTabletReader
import wvlet.log.LogSupport
import wvlet.surface.Surface
import wvlet.surface.reflect.SurfaceFactory

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
object PrettyPrint extends LogSupport {

  private val defaultPrinter = new PrettyPrint()

  def show[A: ru.TypeTag](seq: Seq[A], limit: Int = 20) {
    defaultPrinter.pp(seq.take(limit))
  }

  def pp[A: ru.TypeTag](seq: Seq[A]) {
    info(defaultPrinter.pf(seq).mkString("\n"))
  }

  def screenTextLength(s: String): Int = {
    (for (i <- 0 until s.length) yield {
      val ch = s.charAt(i)
      if (ch < 128) {
        1
      } else {
        2
      }
    }).sum
  }

  def maxColWidths(rows: Seq[Seq[String]], max: Int): IndexedSeq[Int] = {
    if (rows.isEmpty) {
      IndexedSeq(0)
    } else {
      val maxWidth = (0 until rows.head.length).map(i => 0).toArray
      for (r <- rows; (c, i) <- r.zipWithIndex) {
        maxWidth(i) = math.min(max, math.max(screenTextLength(c), maxWidth(i)))
      }
      maxWidth.toIndexedSeq
    }
  }

  def pad(s: String, colWidth: Int): String = {
    val str = (" " * Math.max(0, colWidth - screenTextLength(s))) + s
    if (str.length >= colWidth) {
      str.substring(0, colWidth)
    } else {
      str
    }
  }

}

class PrettyPrint(codec: Map[Surface, MessageCodec[_]] = Map.empty, maxColWidth: Int = 100) extends LogSupport {
  def show[A: ru.TypeTag](seq: Seq[A], limit: Int = 20) {
    pp(seq.take(limit))
  }

  def pp[A: ru.TypeTag](seq: Seq[A]) {
    println(pf(seq).mkString("\n"))
  }

  def pf[A: ru.TypeTag](seq: Seq[A]): Seq[String] = {
    val b = Seq.newBuilder[Seq[String]]
    val paramNames = seq.headOption.map { x =>
      val schema = SurfaceFactory.of[A]
      b += schema.params.map(_.name).toSeq
    }

    val reader = new ObjectTabletReader(seq, codec)
    b ++= (reader | RecordPrinter)
    val s        = Seq.newBuilder[String]
    val rows     = b.result
    val colWidth = PrettyPrint.maxColWidths(rows, maxColWidth)
    for (r <- rows) {
      val cols = for ((c, i) <- r.zipWithIndex) yield PrettyPrint.pad(c, colWidth(i))
      s += cols.mkString(" ")
    }
    s.result()
  }

}
