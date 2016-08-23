package wvlet.airframe

import wvlet.log.LogSupport
import wvlet.obj.ObjectType

/**
  *
  */
class SessionBuilder(design: Design, lifeCycleManager:LifeCycleManager = new DefaultlLifeCycleManager) extends LogSupport {

  def withLifeCycleManager(newLifeCycleManager: LifeCycleManager): SessionBuilder = {
    new SessionBuilder(design, newLifeCycleManager)
  }

  def create: Session = {
    // Override preceding bindings
    val effectiveBindings = for ((key, lst) <- design.binding.groupBy(_.from)) yield {
      lst.last
    }
    val keyIndex: Map[ObjectType, Int] = design.binding.map(_.from).zipWithIndex.map(x => x._1 -> x._2).toMap
    val sortedBindings = effectiveBindings.toSeq.sortBy(x => keyIndex(x.from))
    val session = new SessionImpl(sortedBindings, lifeCycleManager)
    info(f"Creating a new session[${session.hashCode()}%x]")
    Airframe.setSession(session)
    session.init
    session
  }
}
