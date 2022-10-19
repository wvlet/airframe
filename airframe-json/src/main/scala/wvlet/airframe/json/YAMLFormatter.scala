package wvlet.airframe.json
import wvlet.airframe.json.JSON._

/**
  * Convert JSON as Yaml
  */
object YAMLFormatter {
  def toYaml(json: String): String = {
    val visitor = new YamlWriter
    JSONTraverser.traverse(json, visitor)
    visitor.toYaml
  }

  private sealed trait YamlContext {
    def getAndAdd: Int
  }
  private case class OBJECT(private var count: Int) extends YamlContext {
    def getAndAdd: Int = {
      val v = count
      count += 1
      v
    }
  }
  private case class OBJECT_ARRAY(private var count: Int) extends YamlContext {
    def getAndAdd: Int = {
      val v = count
      count += 1
      v
    }
  }
  private case class ARRAY(private var count: Int) extends YamlContext {
    def getAndAdd: Int = {
      val v = count
      count += 1
      v
    }
  }

  class YamlWriter() extends JSONVisitor {
    private val lines                           = Seq.newBuilder[String]
    def toYaml: String                          = lines.result().mkString("\n")
    private var contextStack: List[YamlContext] = Nil

    private def indent(levelOffset: Int = 0): String = {
      "  " * (contextStack.length - 1 + levelOffset)
    }

    private def emitKey(k: String): Unit = {
      lines += s"${indent()}${quoteKey(k)}:"
    }
    private def emitKeyValue(k: String, v: JSONValue): Unit = {
      lines += s"${indent()}${quoteKey(k)}: ${quoteValue(v)}"
    }
    private def emitArrayKeyValue(k: String, v: JSONValue): Unit = {
      lines += s"${"  " * (contextStack.length - 2)}- ${quoteKey(k)}: ${quoteValue(v)}"
    }
    private def emitArrayElement(v: JSONValue): Unit = {
      lines += s"${indent()}- ${quoteValue(v)}"
    }
    private def quoteKey(k: String): String = {
      def isNumber(k: String): Boolean = {
        k.forall {
          case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' => true
          case _                                                         => false
        }
      }

      if (isNumber(k))
        s"'${k}'"
      else
        k
    }

    private def blockString(s: String): String = {
      s.split("\n")
        .map { line =>
          if (line.isEmpty) {
            ""
          } else {
            s"${indent(1)}${line}"
          }
        }
        .mkString("\n")
    }

    private def quoteValue(v: JSONValue): String = {
      val letterPattern = """[\w]+""".r
      def isLetter(s: String): Boolean = {
        s match {
          case letterPattern() => true
          case _               => false
        }
      }
      def hasNewLine(s: String): Boolean      = s.contains('\n')
      def hasSingleQuotes(s: String): Boolean = s.contains("'")

      val str = v.toString
      v match {
        case s: JSONString if hasNewLine(str) || hasSingleQuotes(str) =>
          // output literal
          s"|\n${blockString(str)}"
        case s: JSONString if !isLetter(str) =>
          s"'${str}'"
        case _ =>
          str
      }
    }

    override def visitObject(o: JSON.JSONObject): Unit = {
      contextStack.headOption match {
        case Some(a: ARRAY) =>
          contextStack = OBJECT_ARRAY(0) :: contextStack
        case _ =>
          contextStack = OBJECT(0) :: contextStack
      }
    }
    override def leaveObject(o: JSON.JSONObject): Unit = {
      contextStack = contextStack.tail
      contextStack.headOption.map(_.getAndAdd)
    }

    private def isPrimitive(v: JSON.JSONValue): Boolean = {
      v match {
        case o: JSONObject => false
        case a: JSONArray  => false
        case _             => true
      }
    }

    override def visitKeyValue(k: String, v: JSON.JSONValue): Unit = {
      if (isPrimitive(v)) {
        contextStack.head match {
          case OBJECT_ARRAY(0) =>
            // The first object element inside array should have `-` prefix
            emitArrayKeyValue(k, v)
          case _ =>
            emitKeyValue(k, v)
        }
      } else {
        v match {
          case o: JSONObject if o.isEmpty =>
          // do not output empty key-value pair
          case _ =>
            emitKey(k)
        }
      }
    }
    override def leaveKeyValue(k: String, v: JSON.JSONValue): Unit = {}

    override def visitArray(a: JSON.JSONArray): Unit = {
      contextStack = ARRAY(0) :: contextStack
    }
    override def leaveArray(a: JSONArray): Unit = {
      contextStack = contextStack.tail
      contextStack.headOption.map(_.getAndAdd)
    }

    private def emitPrimitive(v: JSONValue): Unit = {
      contextStack.head match {
        case a: ARRAY =>
          emitArrayElement(v)
        case _ =>
      }
      contextStack.headOption.map(_.getAndAdd)
    }
    override def visitString(v: JSONString): Unit   = emitPrimitive(v)
    override def visitNumber(n: JSONNumber): Unit   = emitPrimitive(n)
    override def visitBoolean(n: JSONBoolean): Unit = emitPrimitive(n)
    override def visitNull: Unit                    = emitPrimitive(JSONNull)
  }

}
