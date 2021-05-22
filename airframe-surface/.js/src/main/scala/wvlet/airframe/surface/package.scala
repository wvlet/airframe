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
package wvlet.airframe

import java.util.concurrent.ConcurrentHashMap

import scala.jdk.CollectionConverters._
import scala.collection.mutable

/**
  */
package object surface {
  val surfaceCache       = new ConcurrentHashMap[String, Surface]().asScala
  val methodSurfaceCache = new ConcurrentHashMap[String, Seq[MethodSurface]]().asScala

  def getCached(fullName: String): Surface =
    surfaceCache.getOrElse(fullName, throw new IllegalArgumentException(s"Surface ${fullName} is not found in cache"))

  def newCacheMap[A, B]: mutable.Map[A, B] = new mutable.HashMap[A, B]()
}
