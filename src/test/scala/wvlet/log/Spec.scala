package wvlet.log

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpec, _}
import wvlet.log.LogFormatter.SourceCodeLogFormatter

trait Spec extends WordSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with wvlet.log.io.Timer
  with LogSupport {
  Logger.setDefaultFormatter(SourceCodeLogFormatter)
}
