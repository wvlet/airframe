package wvlet.airspec.internal.diff

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
