import org.junit.Assert._
import org.junit.Test

class MyTest:
  @Test def nativeTest(): Unit = {
    println("Hello JUnit in Scala Native")
    assertTrue("this assertion should pass", true)
  }
