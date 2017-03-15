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
}
