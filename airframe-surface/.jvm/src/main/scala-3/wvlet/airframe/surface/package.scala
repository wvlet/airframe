package wvlet.airframe

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable
import scala.jdk.CollectionConverters._

package object surface {
  val surfaceCache = new ConcurrentHashMap[String, Surface]().asScala
  val methodSurfaceCache = new ConcurrentHashMap[String, Seq[MethodSurface]]().asScala

  def getCached(fullName: String): Surface = {
    surfaceCache(fullName)
  }

  def newCacheMap[A, B]: scala.collection.mutable.Map[A, B] = new mutable.WeakHashMap[A, B]()
}
