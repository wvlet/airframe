package wvlet.log

/**
  *
  */
object LogUtil {
  def getSuccinctLoggerName(cl: Class[_]): String = {
    val name =
      if (cl.getName.contains("$anon$")) {
        val interfaces = cl.getInterfaces
        if (interfaces != null && interfaces.length > 0) {
          // Use the first interface name instead of annonimized name
          interfaces(0).getName
        }
        else {
          cl.getName
        }
      }
      else {
        cl.getName
      }

    if (name.endsWith("$")) {
      // Remove trailing $ of Scala Object name
      name.substring(0, name.length - 1)
    }
    else {
      name
    }
  }
}
