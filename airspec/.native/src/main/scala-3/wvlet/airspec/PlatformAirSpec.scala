package wvlet.airspec

import org.portablescala.reflect.annotation.EnableReflectiveInstantiation

/**
  * Scala.native platform-specific implementation
  *
  * EnableReflectiveInstantiation annotation is necessary to support (test class).getInstance() in Scala.native
  */
@EnableReflectiveInstantiation
trait PlatformAirSpec:
  this: AirSpecSpi =>
