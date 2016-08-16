package wvlet.airframe

import org.scalatest._
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogSupport, Logger}

import scala.language.implicitConversions
/**
  *
  */
trait AirframeSpec extends WordSpec
  with ShouldMatchers
  with GivenWhenThen
  with BeforeAndAfter
  with BeforeAndAfterAll
  with LogSupport {

  // Add source code location to the debug logs
  Logger.setDefaultFormatter(SourceCodeLogFormatter)

  implicit def toTag(s:String) = Tag(s)
}