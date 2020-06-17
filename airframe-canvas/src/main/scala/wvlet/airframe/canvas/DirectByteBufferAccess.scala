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

/**
  */
import java.lang.reflect.{Constructor, InvocationTargetException, Method}

import scala.util.Try

/**
  * Wraps the difference of access methods to DirectBuffers between Android and others.
  *
  * This code is a Scala-port from msgpack-java
  */
private[canvas] object DirectBufferAccess {
  sealed trait DirectBufferConstructorType
  case object ARGS_LONG_INT_REF extends DirectBufferConstructorType
  case object ARGS_LONG_INT     extends DirectBufferConstructorType
  case object ARGS_INT_INT      extends DirectBufferConstructorType
  case object ARGS_MB_INT_INT   extends DirectBufferConstructorType

  private var mGetAddress: Method    = null
  private var mCleaner: Method       = null
  private var mClean: Option[Method] = None

  // TODO We should use MethodHandle for efficiency, but it is not available in JDK6
  private var byteBufferConstructor: Constructor[_]                                       = null
  private var directByteBufferClass: Class[_]                                             = null
  private var directBufferConstructorType: DirectBufferAccess.DirectBufferConstructorType = null
  private var memoryBlockWrapFromJni: Method                                              = null

  {
    // Find the hidden constructor for DirectByteBuffer
    directByteBufferClass = ClassLoader.getSystemClassLoader.loadClass("java.nio.DirectByteBuffer")
    var directByteBufferConstructor: Constructor[_]  = null
    var constructorType: DirectBufferConstructorType = null
    var mbWrap: Method                               = null
    try { // TODO We should use MethodHandle for Java7, which can avoid the cost of boxing with JIT optimization
      directByteBufferConstructor =
        directByteBufferClass.getDeclaredConstructor(classOf[Long], classOf[Int], classOf[Any])
      constructorType = ARGS_LONG_INT_REF
    } catch {
      case e0: NoSuchMethodException =>
        try { // https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/java/nio/DirectByteBuffer.java
          // DirectByteBuffer(long address, int capacity)
          directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(classOf[Long], classOf[Int])
          constructorType = ARGS_LONG_INT
        } catch {
          case e1: NoSuchMethodException =>
            try {
              directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(classOf[Int], classOf[Int])
              constructorType = ARGS_INT_INT
            } catch {
              case e2: NoSuchMethodException =>
                val aClass = Class.forName("java.nio.MemoryBlock")
                mbWrap = aClass.getDeclaredMethod("wrapFromJni", classOf[Int], classOf[Long])
                mbWrap.setAccessible(true)
                directByteBufferConstructor =
                  directByteBufferClass.getDeclaredConstructor(aClass, classOf[Int], classOf[Int])
                constructorType = ARGS_MB_INT_INT
            }
        }
    }
    byteBufferConstructor = directByteBufferConstructor
    directBufferConstructorType = constructorType
    memoryBlockWrapFromJni = mbWrap
    if (byteBufferConstructor == null)
      throw new RuntimeException("Constructor of DirectByteBuffer is not found")
    byteBufferConstructor.setAccessible(true)
    mGetAddress = directByteBufferClass.getDeclaredMethod("address")
    mGetAddress.setAccessible(true)
    mCleaner = directByteBufferClass.getDeclaredMethod("cleaner")
    mCleaner.setAccessible(true)
    Try {
      val cleanMethod = mCleaner.getReturnType.getDeclaredMethod("clean")
      // In JDK11, java.internal.ref.Cleaner.clean is not accessible
      cleanMethod.setAccessible(true)
      mClean = Some(cleanMethod)
    }
  }

  def getAddress(base: Any): Long = mGetAddress.invoke(base).asInstanceOf[Long]

  def clean(base: Any): Unit = {
    val cleaner = mCleaner.invoke(base)
    mClean.foreach(_.invoke(cleaner))
  }
//
//  def isDirectByteBufferInstance(s: Any): Boolean = directByteBufferClass.isInstance(s)
//
//  def newByteBuffer(address: Long, index: Int, length: Int, reference: AnyRef): ByteBuffer = {
//    val ret: Any = directBufferConstructorType match {
//      case ARGS_LONG_INT_REF =>
//        byteBufferConstructor.newInstance(Long.box(address + index), Int.box(length), reference)
//      case ARGS_LONG_INT =>
//        byteBufferConstructor.newInstance(Long.box(address + index), Int.box(length))
//      case ARGS_INT_INT =>
//        byteBufferConstructor.newInstance(Long.box(address.toInt + index), Int.box(length))
//      case ARGS_MB_INT_INT =>
//        byteBufferConstructor
//          .newInstance(memoryBlockWrapFromJni.invoke(null, Long.box(address + index), Int.box(length)),
//                       Int.box(length),
//                       Int.box(0))
//      case _ =>
//        throw new IllegalStateException("Unexpected value")
//    }
//    ret.asInstanceOf[ByteBuffer]
//  }
//
}
