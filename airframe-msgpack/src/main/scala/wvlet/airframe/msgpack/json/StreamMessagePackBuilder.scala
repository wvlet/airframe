/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.msgpack.json

import wvlet.airframe.json.{JSONContext, JSONScanner, JSONSource}
import wvlet.airframe.msgpack.spi.{MessagePack, MsgPack}

import scala.util.{Success, Try}

object StreamMessagePackBuilder {

  def fromJSON(json: JSONSource): MsgPack = {
    val context = new StreamMessagePackBuilder
    JSONScanner.scanAny(json, context)
    context.result
  }

  sealed abstract class ParseContext(val offset: Long) {
    private var endOffset: Long   = 0
    private var elementCount: Int = 0
    def isObject: Boolean         = false
    def increment: Unit = {
      elementCount += 1
    }
    def numElements: Int = elementCount
    def setEndOffset(endOffset: Long): Unit = {
      this.endOffset = endOffset
    }
    def getEndOffset: Long = endOffset
  }
  class ObjectContext(offset: Long) extends ParseContext(offset) {
    override def isObject: Boolean = true
  }
  class ArrayContext(offset: Long) extends ParseContext(offset)
  class SingleContext              extends ParseContext(0)

}

class StreamMessagePackBuilder extends JSONContext[MsgPack] {
  import StreamMessagePackBuilder._
  protected val packer = MessagePack.newBufferPacker

  protected var contextStack: List[ParseContext]         = Nil
  protected var finishedContextStack: List[ParseContext] = Nil
  protected var context: ParseContext                    = new SingleContext

  override def addNull(s: JSONSource, start: Int, end: Int): Unit = {
    context.increment
    packer.packNil
  }
  override def addString(s: JSONSource, start: Int, end: Int): Unit = {
    context.increment
    packer.packString(s.substring(start, end))
  }
  override def addUnescapedString(s: String): Unit = {
    context.increment
    packer.packString(s)
  }
  override def addNumber(s: JSONSource, start: Int, end: Int, dotIndex: Int, expIndex: Int): Unit = {
    context.increment
    val v = s.substring(start, end)
    if (dotIndex >= 0 || expIndex >= 0) {
      packer.packDouble(v.toDouble)
    } else {
      Try(v.toLong) match {
        case Success(l) => packer.packLong(l)
        case _          =>
          // Integer overflow
          packer.packString(v)
      }
    }
  }
  override def addBoolean(s: JSONSource, v: Boolean, start: Int, end: Int): Unit = {
    context.increment
    packer.packBoolean(v)
  }

  private def addContext(newContext: ParseContext): Unit = {
    contextStack = context :: contextStack
    context = newContext
  }

  override def add(v: MsgPack): Unit = {}

  override def isObjectContext: Boolean = {
    context.isObject
  }

  override def result: MsgPack = {
    if (contextStack.length > 1) {
      Array.emptyByteArray
    } else {
      val src          = packer.toByteArray
      val out          = MessagePack.newBufferPacker
      var cursor: Long = 0
      def loop(stack: List[ParseContext]): Unit = {
        stack match {
          case Nil =>
          // do nothing
          case c :: remainings =>
            if (c.offset > cursor) {
              val len = c.offset - cursor
              out.addPayload(src, cursor.toInt, len.toInt)
              cursor += len
            }
            c match {
              case o: ObjectContext =>
                val numMapElements = o.numElements / 2
                out.packMapHeader(numMapElements)
                loop(remainings)
              case a: ArrayContext =>
                val numMapElements = a.numElements
                out.packArrayHeader(numMapElements)
                loop(remainings)
              case s: SingleContext =>
                loop(remainings)
            }
            if (cursor < c.getEndOffset) {
              val len = c.getEndOffset - cursor
              out.addPayload(src, cursor.toInt, len.toInt)
              cursor += len
            }
        }
      }
      loop(finishedContextStack)
      out.toByteArray
    }
  }

  override def closeContext(s: JSONSource, end: Int): Unit = {
    val currentOffset = packer.totalByteSize
    context.setEndOffset(currentOffset)
    finishedContextStack = context :: finishedContextStack
    context = contextStack.head
    contextStack = contextStack.tail
  }

  override def singleContext(s: JSONSource, start: Int): JSONContext[MsgPack] = this

  override def objectContext(s: JSONSource, start: Int): JSONContext[MsgPack] = {
    context.increment
    addContext(new ObjectContext(packer.totalByteSize))
    this
  }

  override def arrayContext(s: JSONSource, start: Int): JSONContext[MsgPack] = {
    context.increment
    addContext(new ArrayContext(packer.totalByteSize))
    this
  }
}
