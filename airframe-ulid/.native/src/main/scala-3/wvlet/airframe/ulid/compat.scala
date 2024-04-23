package wvlet.airframe.ulid

import java.security.{NoSuchAlgorithmException, SecureRandom}
import scala.util.Random

object compat:
  val random: Random =
    // TODO: Use secure random generator
    scala.util.Random

  def sleep(millis: Int): Unit =
    Thread.sleep(millis)
