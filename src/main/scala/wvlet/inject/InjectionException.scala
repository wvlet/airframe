package wvlet.inject

import wvlet.inject.InjectionException.ErrorType
import wvlet.obj.ObjectType

object InjectionException {

  sealed trait ErrorType {
    def errorCode: String = this.toString
  }
  case class MISSING_CONTEXT(cl:ObjectType) extends ErrorType
  case class CYCLIC_DEPENDENCY(deps: Set[ObjectType]) extends ErrorType

}
/**
  *
  */
class InjectionException(errorType: ErrorType) extends Exception(errorType.toString) {

}


