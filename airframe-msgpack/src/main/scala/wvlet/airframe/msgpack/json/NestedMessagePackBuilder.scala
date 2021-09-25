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
import wvlet.log.LogSupport

import scala.util.{Success, Try}

object NestedMessagePackBuilder {
  def fromJSON(json: JSONSource): MsgPack = {
    val context = new NestedMessagePackBuilder()
    JSONScanner.scanAny(json, context)
    context.mergedResult
  }
}

class NestedMessagePackBuilder extends JSONContext[Seq[MsgPack]] with LogSupport { parent =>
  protected val packer = MessagePack.newBufferPacker

  def mergedResult: MsgPack = {
    val buffers = result
    if (buffers.length == 1) {
      buffers.head
    } else {
      val size   = buffers.map(_.length).sum
      val result = new Array[Byte](size)
      var offset = 0
      for (x <- buffers) {
        Array.copy(x, 0, result, offset, x.length)
        offset += x.length
      }
      result
    }
  }

  private var cachedResult: Option[Seq[MsgPack]] = None
  override def result: Seq[MsgPack] = {
    synchronized {
      if (cachedResult.isEmpty) {
        cachedResult = Some(Seq(packer.toByteArray))
      }
      cachedResult.get
    }
  }

  override def isObjectContext: Boolean = false
  override def add(v: Seq[MsgPack]): Unit = {
    v.foreach { b => packer.writePayload(b) }
  }
  override def closeContext(s: JSONSource, end: Int): Unit          = {}
  override def addNull(s: JSONSource, start: Int, end: Int): Unit   = packer.packNil
  override def addString(s: JSONSource, start: Int, end: Int): Unit = packer.packString(s.substring(start, end))
  override def addUnescapedString(s: String): Unit = {
    packer.packString(s)
  }
  override def addNumber(s: JSONSource, start: Int, end: Int, dotIndex: Int, expIndex: Int): Unit = {
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
  override def addBoolean(s: JSONSource, v: Boolean, start: Int, end: Int): Unit = packer.packBoolean(v)

  override def singleContext(s: JSONSource, start: Int): JSONContext[Seq[MsgPack]] = new LocalStructureContext()

  override def objectContext(s: JSONSource, start: Int): JSONContext[Seq[MsgPack]] = {
    new LocalStructureContext {
      override def isObjectContext: Boolean = true
      override def closeContext(s: JSONSource, end: Int): Unit = {
        // Add msgpack data to the parent
        parent.add(result)
      }

      override def result: Seq[MsgPack] = {
        val mapElementCount = getElementCount / 2
        Seq(
          // Embed map header count
          MessagePack.newBufferPacker.packMapHeader(mapElementCount).toByteArray,
          packer.toByteArray
        )
      }
    }
  }

  override def arrayContext(s: JSONSource, start: Int): JSONContext[Seq[MsgPack]] = {
    new LocalStructureContext {
      override def closeContext(s: JSONSource, end: Int): Unit = {
        // Add msgpack data to the parent
        parent.add(result)
      }

      override def result: Seq[MsgPack] = {
        Seq(
          // Embed array header count
          MessagePack.newBufferPacker.packArrayHeader(getElementCount).toByteArray,
          packer.toByteArray
        )
      }
    }
  }
}

/**
  * A JSON parse context implementation for remembering how many elements are added to an object or array
  */
class LocalStructureContext extends NestedMessagePackBuilder { self =>
  private var elementCount: Int = 0

  protected def getElementCount: Int = elementCount

  override def addNull(s: JSONSource, start: Int, end: Int): Unit = {
    elementCount += 1
    super.addNull(s, start, end)
  }

  override def addNumber(s: JSONSource, start: Int, end: Int, dotIndex: Int, expIndex: Int): Unit = {
    elementCount += 1
    super.addNumber(s, start, end, dotIndex, expIndex)
  }

  override def addBoolean(s: JSONSource, v: Boolean, start: Int, end: Int): Unit = {
    elementCount += 1
    super
      .addBoolean(s, v, start, end)
  }

  override def addString(s: JSONSource, start: Int, end: Int): Unit = {
    elementCount += 1
    super.addString(s, start, end)
  }

  override def addUnescapedString(s: String): Unit = {
    elementCount += 1
    super.addUnescapedString(s)
  }

  override def add(v: Seq[MsgPack]): Unit = {
    elementCount += 1
    super.add(v)
  }
}
