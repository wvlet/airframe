package dotty.test

object TestMain {

  def main(args:Array[String]): Unit = {
    args match {
      case Array("log") => LogTest.run
      case Array("surface") => Surface3Test.run
      case _ => 
        LogTest.run
        Surface3Test.run
    }
  }

}