package wvlet.airframe.msgpack.spi
import java.io.{InputStream, OutputStream}

import wvlet.airframe.msgpack.impl.{PureScalaBufferPacker, PureScalaBufferUnpacker}
import wvlet.airframe.msgpack.io.ByteArrayBuffer

/**
  * Compatibility layer for Scala.js
  */
object Compat:
  def isScalaJS = false

  def floatToIntBits(v: Float): Int     = java.lang.Float.floatToIntBits(v)
  def doubleToLongBits(v: Double): Long = java.lang.Double.doubleToLongBits(v)

  def newBufferPacker: BufferPacker =
    new PureScalaBufferPacker
  def newPacker(out: OutputStream): Packer   = ???
  def newUnpacker(in: InputStream): Unpacker = ???
  def newUnpacker(msgpack: Array[Byte]): Unpacker =
    newUnpacker(msgpack, 0, msgpack.length)
  def newUnpacker(msgpack: Array[Byte], offset: Int, len: Int): Unpacker =
    new PureScalaBufferUnpacker(ByteArrayBuffer.fromArray(msgpack, offset, len))
