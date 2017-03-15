package wvlet.log

/**
  *
  */
object LogUtil {
  def getSuccinctLoggerName(cl: Class[_]): String = {
    val name =cl.getName

    if (name.contains("$")) {
      // Remove trailing $ of Scala Object name
      name.substring(0, name.length - 1)
    }
    else {
      name
    }
  }
}
