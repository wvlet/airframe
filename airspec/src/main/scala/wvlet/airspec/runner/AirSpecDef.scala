package wvlet.airspec.runner

import wvlet.airframe._
import wvlet.airframe.AirframeException.MISSING_DEPENDENCY
import wvlet.airframe.surface.MethodSurface
import wvlet.airspec.AirSpecSpi
import wvlet.airspec.spi.{AirSpecContext, MissingTestDependency}

sealed trait AirSpecDef {
  def name: String
  def run(context: AirSpecContext, session: Session): Any
}

case class MethodAirSpecDef(methodSurface: MethodSurface) extends AirSpecDef {
  override def name: String = methodSurface.name

  override def run(context: AirSpecContext, session: Session): Any = {
    // Build a list of method arguments
    val args: Seq[Any] = for (p <- methodSurface.args) yield {
      try {
        p.surface.rawType match {
          case cls if classOf[AirSpecContext].isAssignableFrom(cls) =>
            context
          case _ =>
            session.getInstanceOf(p.surface)
        }
      } catch {
        case e @ MISSING_DEPENDENCY(stack, _) =>
          throw MissingTestDependency(
            s"Failed to call ${context.currentSpec.leafSpecName}.`${methodSurface.name}`. Missing dependency for ${p.name}:${p.surface}:\n${e.getMessage}"
          )
      }
    }
    // Call the test method
    methodSurface.call(context.currentSpec, args: _*)
  }
}

case class AirSpecDefF0[R](name: String, design: Design, body: LazyF0[R]) extends AirSpecDef {
  override def run(context: AirSpecContext, session: Session): Any = {
    body.eval
  }
}
