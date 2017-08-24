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
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
  }

}
