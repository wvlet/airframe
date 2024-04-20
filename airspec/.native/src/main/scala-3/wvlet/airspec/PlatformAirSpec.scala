package wvlet.airspec

import scala.scalanative.reflect.annotation.EnableReflectiveInstantiation

/**
  * Scala.native platform-specific implementation
  *
  * EnableReflectiveInstantiation annotation is necessary to support (test class).getInstance() in Scala.native
  */
@EnableReflectiveInstantiation
trait PlatformAirSpec:
  this: AirSpecSpi =>
