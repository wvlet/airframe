package wvlet.airspec.internal

object PrettyPrint {
  private def defaultPrinter: PartialFunction[Any, String] = {
    case null =>
      "null"
    case a: Array[_] =>
      s"[${a.mkString(",")}]"
  }

  def pp(v: Any): String = {
    val printer = defaultPrinter
      .orElse(wvlet.airspec.compat.platformSpecificPrinter)
      .orElse[Any, String] { case _ =>
        v.toString
      }

    printer(v)
  }
}
