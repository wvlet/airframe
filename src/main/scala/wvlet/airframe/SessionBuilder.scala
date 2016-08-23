package wvlet.airframe

import wvlet.log.LogSupport
import wvlet.obj.ObjectType

/**
  *
  */
class SessionBuilder(design:Design, name:Option[String] = None, handler:LifeCycleEventHandler = LifeCycleManager.defaultLifeCycleEventHandler) extends LogSupport {

  /**
    * @param e
    * @return
    */
  def addEventHandler(e:LifeCycleEventHandler): SessionBuilder = {
    new SessionBuilder(design, name, handler.wraps(e))
  }

  def withName(sessionName:String) : SessionBuilder = {
    new SessionBuilder(design, Some(sessionName), handler)
  }

  def create: Session = {
    // Override preceding bindings
    val effectiveBindings = for ((key, lst) <- design.binding.groupBy(_.from)) yield {
      lst.last
    }
    val keyIndex: Map[ObjectType, Int] = design.binding.map(_.from).zipWithIndex.map(x => x._1 -> x._2).toMap
    val sortedBindings = effectiveBindings.toSeq.sortBy(x => keyIndex(x.from))
    val l =  new LifeCycleManager(handler)
    val session = new SessionImpl(name, sortedBindings, l)
    debug(f"Creating a new session: ${session.name}")
    l.setSession(session)
    Airframe.setSession(session)
    session.init
    session
  }
}
