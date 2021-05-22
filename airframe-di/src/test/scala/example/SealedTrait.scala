package example

/**
  */
object SealedTrait {
  sealed trait Adt
  object Adt {
    object Foo extends Adt
    object Bar extends Adt
  }
}
