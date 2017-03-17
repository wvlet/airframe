package wvlet.log

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}
import wvlet.log.LogFormatter.SourceCodeLogFormatter

/**
  *
  */
trait Spec extends WordSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with LogSupport {

  Logger.setDefaultFormatter(SourceCodeLogFormatter)

  override protected def beforeAll(): Unit = {
    // Run LogLevel scanner (log-test.properties or log.properties in classpath) every 1 minute
    Logger.scheduleLogLevelScan
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    Logger.stopScheduledLogLevelScan
    super.afterAll()
  }

}
