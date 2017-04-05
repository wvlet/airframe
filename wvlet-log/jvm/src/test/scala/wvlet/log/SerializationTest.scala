package wvlet.log

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import wvlet.log.io.IOUtil

object SerializationTest {

  trait A extends LogSupport {
    info("new A")
    def hello = info("hello")
  }
}

/**
  *
  */
class SerializationTest extends Spec {

  import SerializationTest._

  "Logger" should {
    "serializable" in {
      val a = new A {}
      val b = new ByteArrayOutputStream()
      IOUtil.withResource(new ObjectOutputStream(b)) {out =>
        out.writeObject(a)
      }
      val ser = b.toByteArray
      IOUtil.withResource(new ObjectInputStream(new ByteArrayInputStream(ser))) { in =>
        info("deserialization")
        val a = in.readObject().asInstanceOf[A]
        a.hello
      }
    }
  }
}
