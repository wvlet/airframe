package wvlet.airframe

import wvlet.log.LogSupport
import wvlet.obj.ObjectType

/**
  *
  */
class SessionBuilder(design: Design, handler:LifeCycleEventHandler = LifeCycleManager.defaultLifeCycleEventHandler) extends LogSupport {

  /**
    * @param e
    * @return
    */
  def addEventHandler(e:LifeCycleEventHandler): SessionBuilder = {
    new SessionBuilder(design, handler.wraps(e))
  }

  def create: Session = {
    // Override preceding bindings
    val effectiveBindings = for ((key, lst) <- design.binding.groupBy(_.from)) yield {
      lst.last
    }
    val keyIndex: Map[ObjectType, Int] = design.binding.map(_.from).zipWithIndex.map(x => x._1 -> x._2).toMap
    val sortedBindings = effectiveBindings.toSeq.sortBy(x => keyIndex(x.from))
    val session = new SessionImpl(sortedBindings, new LifeCycleManager(handler))
    debug(f"Creating a new session[${session.hashCode()}%x]")
    Airframe.setSession(session)
    session.init
    session
  }
}
