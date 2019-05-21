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
package wvlet.airframe.canvas
import java.nio.ByteBuffer

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import wvlet.airframe.AirframeSpec
import wvlet.airframe.control.Control

/**
  *
  */
class CanvasTest extends AirframeSpec with ScalaCheckPropertyChecks {

  def check[A](v: A, canvas: Canvas, writer: Canvas => Unit, reader: Canvas => A): Unit = {
    writer(canvas)
    val v2 = reader(canvas)
    v2 shouldBe v
  }

  def checkReadWritePrimitiveValues(c: Canvas): Unit = {
    for (offset <- 0L until c.size) {
      forAll { (v: Boolean) =>
        check(v, c, _.writeBoolean(offset, v), _.readBoolean(offset))
      }
    }
    for (offset <- 0L until c.size - 4) {
      forAll { (v: Int) =>
        check(v, c, _.writeInt(offset, v), _.readInt(offset))
      }
    }
    for (offset <- 0L until c.size - 4) {
      forAll { (v: Int) =>
        check(v, c, _.writeIntBigEndian(offset, v), _.readIntBigEndian(offset))
      }
    }
    for (offset <- 0L until c.size - 8) {
      forAll { (v: Long) =>
        check(v, c, _.writeLong(offset, v), _.readLong(offset))
      }
    }
    for (offset <- 0L until c.size - 8) {
      forAll { (v: Long) =>
        check(v, c, _.writeLongBigEndian(offset, v), _.readLongBigEndian(offset))
      }
    }
    for (offset <- 0L until c.size - 2) {
      forAll { (v: Short) =>
        check(v, c, _.writeShort(offset, v), _.readShort(offset))
      }
    }

    for (offset <- 0L until c.size) {
      forAll { (v: Byte) =>
        check(v, c, _.writeByte(offset, v), _.readByte(offset))
      }
    }

    for (offset <- 0L until c.size - 4) {
      forAll { (v: Float) =>
        check(v, c, _.writeFloat(offset, v), _.readFloat(offset))
      }
    }
    for (offset <- 0L until c.size - 8) {
      forAll { (v: Double) =>
        check(v, c, _.writeDouble(offset, v), _.readDouble(offset))
      }
    }

    forAll { (v: Array[Byte]) =>
      for (offset <- 0L to c.size - v.size) {
        check(v, c, _.writeBytes(offset, v), _.readBytes(offset, v.size))
      }
    }

  }

  val canvasSize = 64

  def withCanvas(creator: => Canvas)(body: Canvas => Unit): Unit = {
    Control.withResource(creator) { c =>
      body(c)
    }
  }

  "Canvas" should {
    "create on-heap canvas" in {
      withCanvas(Canvas.newCanvas(canvasSize)) { c =>
        checkReadWritePrimitiveValues(c)
      }
    }

    "create off-heap canvas" in {
      withCanvas(Canvas.newOffHeapCanvas(canvasSize)) { c =>
        checkReadWritePrimitiveValues(c)
      }
    }

    "create array-wrapped canvas" in {
      withCanvas(Canvas.wrap(Array.ofDim[Byte](canvasSize))) { c =>
        checkReadWritePrimitiveValues(c)
      }
    }

    "create sub-array wrapped canvas" in {
      val b = Array.ofDim[Byte](canvasSize)
      withCanvas(Canvas.wrap(b, 10, 30)) { c =>
        checkReadWritePrimitiveValues(c)
      }
    }

    "create ByteBuffer-based canvas" in {
      withCanvas(Canvas.wrap(ByteBuffer.allocate(canvasSize))) { c =>
        checkReadWritePrimitiveValues(c)
      }
    }

    "create DirectByteBuffer-based canvas" in {
      val b = ByteBuffer.allocateDirect(canvasSize)
      withCanvas(Canvas.wrap(b)) { c =>
        checkReadWritePrimitiveValues(c)
      }
    }

    "create slices" in {
      val c = Canvas.newCanvas(100)
      for (i <- 0L until c.size) {
        c.writeByte(i, i.toByte)
      }
      c.slice(0, c.size) shouldBe c
      val c1 = c.slice(10, 20)
      c1.toByteArray shouldBe c.readBytes(10, 20)
    }

    "check invalid slice size" in {
      val c = Canvas.newCanvas(10)
      intercept[IllegalArgumentException] {
        c.slice(130, 10)
      }

      intercept[IllegalArgumentException] {
        c.slice(0, 20)
      }
    }

    "check invalid read size" in {
      val c = Canvas.newCanvas(10)
      intercept[IllegalArgumentException] {
        c.readBytes(0, Long.MaxValue)
      }
    }

    "copy between Canvases" in {
      val c1 = Canvas.newCanvas(100)
      val c2 = Canvas.newCanvas(100)

      for (i <- 0L until c1.size) {
        c1.writeByte(i, i.toByte)
      }
      c1.readBytes(30, c2, 20, 10)
      c1.readBytes(30, 10) shouldBe c2.readBytes(20, 10)

      val c3 = Canvas.newCanvas(100)
      c3.writeBytes(0, c1, 50, 5)
      c3.readBytes(0, 5) shouldBe c1.readBytes(50, 5)
    }

  }
}
