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
package wvlet.airframe.ulid

import scala.scalanative.runtime.ByteArray
import scala.scalanative.unsafe.*

/**
  * This code is based on
  * https://github.com/lolgab/scala-native-crypto/blob/main/scala-native-crypto/src/java/security/SecureRandom.scala
  * (APL2 license)
  */
class NativeSecureRandom() extends java.util.Random(0L):

  override def setSeed(x: Long): Unit = ()

  override def nextBytes(bytes: Array[Byte]): Unit =
    val len = bytes.length
    nextBytes(bytes.asInstanceOf[ByteArray].at(0), len)

  // Adapted from Scala.js
  // https://github.com/scala-js/scala-js-java-securerandom/blob/d6cf1c8651006047bb8c8bfc754ef10500d3bd06/src/main/scala/java/security/SecureRandom.scala#L47
  override protected final def next(numBits: Int): Int =
    if numBits <= 0 then 0 // special case because the formula on the last line is incorrect for numBits == 0
    else
      val bytes = stackalloc[Int]()
      nextBytes(bytes.asInstanceOf[Ptr[Byte]], 4)
      val rand32 = !bytes
      rand32 & (-1 >>> (32 - numBits)) // Clear the (32 - numBits) higher order bits

  private def nextBytes(bytes: Ptr[Byte], len: Int): Unit =
    if crypto.RAND_bytes(bytes, len) < 0 then throw new IllegalStateException("Failed to generate secure random bytes")

@link("crypto")
@extern
private object crypto:
  def RAND_bytes(buf: Ptr[Byte], num: CInt): CInt = extern

  def MD5_Init(c: Ptr[Byte]): CInt                                = extern
  def MD5_Update(c: Ptr[Byte], data: Ptr[Byte], len: CSize): CInt = extern
  def MD5_Final(md: CString, c: Ptr[Byte]): CInt                  = extern

  def SHA1_Init(c: Ptr[Byte]): CInt                                = extern
  def SHA1_Update(c: Ptr[Byte], data: Ptr[Byte], len: CSize): CInt = extern
  def SHA1_Final(md: CString, c: Ptr[Byte]): CInt                  = extern

  def SHA224_Init(c: Ptr[Byte]): CInt                                = extern
  def SHA224_Update(c: Ptr[Byte], data: Ptr[Byte], len: CSize): CInt = extern
  def SHA224_Final(md: CString, c: Ptr[Byte]): CInt                  = extern

  def SHA256_Init(c: Ptr[Byte]): CInt                                = extern
  def SHA256_Update(c: Ptr[Byte], data: Ptr[Byte], len: CSize): CInt = extern
  def SHA256_Final(md: CString, c: Ptr[Byte]): CInt                  = extern

  def SHA384_Init(c: Ptr[Byte]): CInt                                = extern
  def SHA384_Update(c: Ptr[Byte], data: Ptr[Byte], len: CSize): CInt = extern
  def SHA384_Final(md: CString, c: Ptr[Byte]): CInt                  = extern

  def SHA512_Init(c: Ptr[Byte]): CInt                                = extern
  def SHA512_Update(c: Ptr[Byte], data: Ptr[Byte], len: CSize): CInt = extern
  def SHA512_Final(md: CString, c: Ptr[Byte]): CInt                  = extern
