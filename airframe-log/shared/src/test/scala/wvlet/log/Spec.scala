package wvlet.log

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}
import wvlet.log.LogFormatter.SourceCodeLogFormatter

/**
  *
  */
trait Spec extends WordSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll with LogSupport {

  // Use console log output for both Scala (JVM) and Scala.js
  Logger.setDefaultHandler(new ConsoleLogHandler(SourceCodeLogFormatter))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
  }

  private[log] object CompatParColls {
    val Converters = {
      import Compat._

      {
        import scala.collection.parallel._

        CollectionConverters
      }
    }

    object Compat {
      object CollectionConverters
    }
  }
}
