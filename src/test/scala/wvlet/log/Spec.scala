package wvlet.log

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpec, _}
import wvlet.log.LogFormatter.SourceCodeLogFormatter

trait Spec extends WordSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll with LogSupport {

  Logger.setDefaultFormatter(SourceCodeLogFormatter)

  override def run(testName: Option[String], args: Args): Status = {
    Logger.scheduleLogLevelScan
    try {
      super.run(testName, args)
    }
    finally {
      Logger.stopScheduledLogLevelScan
    }
  }
}
