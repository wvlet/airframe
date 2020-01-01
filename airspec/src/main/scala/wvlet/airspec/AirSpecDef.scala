package wvlet.airspec

import wvlet.airframe._
import wvlet.airframe.AirframeException.MISSING_DEPENDENCY
import wvlet.airframe.surface.{Surface, MethodSurface}
import wvlet.airspec.spi.{AirSpecContext, MissingTestDependency}

private[airspec] sealed trait AirSpecDef {
  def name: String
  def design: Design
  def run(context: AirSpecContext, session: Session): Any

  protected def resolveArg(
      context: AirSpecContext,
      session: Session,
      surface: Surface,
      paramName: Option[String] = None
  ): Any = {
    try {
      surface.rawType match {
        case cls if classOf[AirSpecContext].isAssignableFrom(cls) =>
          context
        case _ =>
          session.getInstanceOf(surface)
      }
    } catch {
      case e @ MISSING_DEPENDENCY(stack, _) =>
        val paramPrefix = paramName.map(x => s"${x}:").getOrElse("")
        throw MissingTestDependency(
          s"Failed to call ${context.currentSpec.leafSpecName}.`${name}`. Missing dependency for ${paramPrefix}${surface}:\n${e.getMessage}"
        )
    }
  }
}

private[airspec] case class MethodAirSpecDef(methodSurface: MethodSurface) extends AirSpecDef {
  override def name: String   = methodSurface.name
  override def design: Design = Design.empty

  override def run(context: AirSpecContext, session: Session): Any = {
    // Build a list of method arguments
    val args: Seq[Any] = for (p <- methodSurface.args) yield {
      resolveArg(context, session, p.surface, paramName = Some(p.name))
    }
    // Call the test method
    methodSurface.call(context.currentSpec, args: _*)
  }
}

case class AirSpecDefF0[R](name: String, design: Design, returnType: Surface, body: LazyF0[R]) extends AirSpecDef {
  override def run(context: AirSpecContext, session: Session): Any = {
    body.eval
  }
}

case class AirSpecDefF1[D1, R](
    name: String,
    design: Design,
    dep1Type: Surface,
    returnType: Surface,
    body: D1 => R
) extends AirSpecDef {

  override def run(context: AirSpecContext, session: Session): Any = {
    val arg: D1 = resolveArg(context, session, dep1Type).asInstanceOf[D1]
    body(arg)
  }
}

case class AirSpecDefF2[D1, D2, R](
    name: String,
    design: Design,
    dep1Type: Surface,
    dep2Type: Surface,
    returnType: Surface,
    body: (D1, D2) => R
) extends AirSpecDef {

  override def run(context: AirSpecContext, session: Session): Any = {
    val arg1: D1 = resolveArg(context, session, dep1Type).asInstanceOf[D1]
    val arg2: D2 = resolveArg(context, session, dep2Type).asInstanceOf[D2]
    body(arg1, arg2)
  }
}

case class AirSpecDefF3[D1, D2, D3, R](
    name: String,
    design: Design,
    dep1Type: Surface,
    dep2Type: Surface,
    dep3Type: Surface,
    returnType: Surface,
    body: (D1, D2, D3) => R
) extends AirSpecDef {

  override def run(context: AirSpecContext, session: Session): Any = {
    val arg1: D1 = resolveArg(context, session, dep1Type).asInstanceOf[D1]
    val arg2: D2 = resolveArg(context, session, dep2Type).asInstanceOf[D2]
    val arg3: D3 = resolveArg(context, session, dep3Type).asInstanceOf[D3]
    body(arg1, arg2, arg3)
  }
}
