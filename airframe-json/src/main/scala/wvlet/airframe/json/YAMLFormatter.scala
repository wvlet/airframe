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
    def indent: Int
  }
  private case object OBJECT extends YamlContext {
    override def indent = 1
  }
  private case object ARRAY extends YamlContext {
    override def indent = 1
  }

  class YamlWriter() extends JSONVisitor {
    private val lines                           = Seq.newBuilder[String]
    def toYaml: String                          = lines.result().mkString("\n")
    private var contextStack: List[YamlContext] = Nil

    private def indent: String = {
      "  " * (contextStack.map(_.indent).sum - 1)
    }

    private def emitKey(k: String): Unit = {
      lines += s"${indent}${quoteKey(k)}:"
    }
    private def emitKeyValue(k: String, v: JSONValue): Unit = {
      lines += s"${indent}${quoteKey(k)}: ${quoteValue(v)}"
    }
    private def emitArrayElement(v: JSONValue): Unit = {
      lines += s"${indent}- ${quoteValue(v)}"
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

    private def quoteValue(v: JSONValue): String = {
      val letterPattern = """[\w]+""".r
      def isLetter(s: String): Boolean = {
        s match {
          case letterPattern() => true
          case _               => false
        }
      }

      v match {
        case s: JSONString if !isLetter(s.toString) =>
          s"'${s.toString}'"
        case other =>
          v.toString
      }
    }

    override def visitObject(o: JSON.JSONObject): Unit = {
      contextStack = OBJECT :: contextStack
    }
    override def leaveObject(o: JSON.JSONObject): Unit = {
      contextStack = contextStack.tail
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
        emitKeyValue(k, v)
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
      contextStack = ARRAY :: contextStack
    }
    override def leaveArray(a: JSONArray): Unit = {
      contextStack = contextStack.tail
    }

    private def emitPrimitive(v: JSONValue): Unit = {
      contextStack.head match {
        case OBJECT =>
        case ARRAY =>
          emitArrayElement(v)
      }
    }
    override def visitString(v: JSONString): Unit   = emitPrimitive(v)
    override def visitNumber(n: JSONNumber): Unit   = emitPrimitive(n)
    override def visitBoolean(n: JSONBoolean): Unit = emitPrimitive(n)
    override def visitNull: Unit                    = emitPrimitive(JSONNull)
  }

}
