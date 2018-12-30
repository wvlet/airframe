package wvlet.msgframe.sql.model
import java.io.PrintWriter

/**
  *
  */
object ModelTreePrinter {

  def print(m: SQLModel, out: PrintWriter, level: Int): Unit = {
    val ws = " " * (level * 2)
    out.println(s"${ws}- ${m.modelName}")
    for (c <- m.children) {
      out.println(print(c, out, level + 1))
    }
  }
}
