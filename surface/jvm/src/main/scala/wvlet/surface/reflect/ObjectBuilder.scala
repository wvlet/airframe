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
package wvlet.surface.reflect

import java.util.Locale

import wvlet.log.LogSupport
import wvlet.surface.reflect.ReflectTypeUtil.{canBuildFromBuffer, canBuildFromString, isArray}
import wvlet.surface.{OptionSurface, Parameter, Surface, Zero}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

//--------------------------------------
//
// ObjectBuilder.scala
// Since: 2012/01/25 12:41
//
//--------------------------------------

/**
  *
  *
  */
object ObjectBuilder extends LogSupport {

  def apply(s: Surface): ObjectBuilder = {
    new SimpleObjectBuilder(s)
  }

  def fromObject[A](surface: Surface, obj:A): ObjectBuilder = {
    val b = new SimpleObjectBuilder(surface)
    for (p <- surface.params) {
      b.set(p.name, p.get(obj))
    }
    b
  }

  sealed trait BuilderElement
  case class Holder(holder: ObjectBuilder) extends BuilderElement
  case class Value(value: Any) extends BuilderElement
  case class ArrayHolder(holder: mutable.ArrayBuffer[Any]) extends BuilderElement

  trait ParameterNameFormatter
  case object CanonicalNameFormatter extends ParameterNameFormatter {
    def format(name: String): String = {
      name.toLowerCase(Locale.US).replaceAll("[ _\\.-]", "")
    }
  }

  implicit class ToCanonicalNameFormatter(name: String) {
    def canonicalName: String = {
      CanonicalNameFormatter.format(name)
    }
  }

}

trait GenericBuilder {

  def set(path: String, value: Any): Unit = set(Path(path), value)
  def set(path: Path, value: Any): Unit

  def get(name: String): Option[Any]
}

/**
  * Generic object builder
  *
  * @author leo
  */
trait ObjectBuilder extends GenericBuilder {

  def build: Any

}

trait StandardBuilder extends GenericBuilder with LogSupport {

  import ObjectBuilder._

  protected val holder = collection.mutable.Map.empty[String, BuilderElement]

  protected def findParameter(name: String): Option[Parameter]
  protected def getParameterTypeOf(name: String) = findParameter(name).get

  protected def defaultValues: collection.immutable.Map[String, Any]

  // set the default values of the object
  for ((name, value) <- defaultValues) {
    val v: BuilderElement = findParameter(name).map {
      case p if canBuildFromBuffer(p.surface) => Value(value)
      case p if canBuildFromStringValue(p.surface) => Value(value)
      case p => {
        // nested object
        // TODO handling of recursive objects
        val b = new SimpleObjectBuilder(p.surface)
        for (x <- p.surface.params) {
          b.set(x.name.canonicalName, x.get(value))
        }
        Holder(b)
      }
    } getOrElse Value(value)

    holder += name -> v
  }

  private def canBuildFromStringValue(t: Surface): Boolean = {
    t match {
      case t if canBuildFromString(t) => true
      case o: OptionSurface => canBuildFromStringValue(o.elementSurface)
      case _ => false
    }
  }

  def set(path: Path, value: Any) {
    if (!path.isEmpty) {
      val name = CanonicalNameFormatter.format(path.head)
      val p = findParameter(name)
      if (p.isEmpty) {
        error(s"no parameter is found for path $path")
        return
      }

      trace(s"set path $path : $value")

      if (path.isLeaf) {
        val valueType = p.get.surface
        trace(s"update value holder name:$name, valueType:$valueType (isArray:${isArray(valueType)}) with value:$value")
        if (canBuildFromBuffer(valueType)) {
          val gt = valueType.typeArgs(0)

          holder.get(name) match {
            case Some(Value(v)) =>
              // remove the default value
              holder.remove(name)
            case _ => // do nothing
          }
          val arr = holder.getOrElseUpdate(name, ArrayHolder(new ArrayBuffer[Any])).asInstanceOf[ArrayHolder]
          TypeConverter.convert(value, gt) map {arr.holder += _}
        }
        else if (canBuildFromStringValue(valueType)) {
          TypeConverter.convert(value, valueType).map {v =>
            holder += name -> Value(v)
          }
        }
        else {
          error(s"failed to set $value to path $path")
        }
      }
      else {
        // nested object
        val paramName = CanonicalNameFormatter.format(path.head)
        val h = holder.getOrElseUpdate(paramName, Holder(ObjectBuilder(p.get.surface)))
        h match {
          case Holder(b) => b.set(path.tailPath, value)
          case other =>
            // overwrite the existing holder
            throw new IllegalStateException("invalid path:%s, value:%s, holder:%s".format(path, value, other))
        }
      }
    }
  }

  def get(name: String): Option[Any] = {
    val paramName = name.canonicalName
    holder.get(paramName) flatMap {
      case Holder(h) => Some(h.build)
      case Value(v) => Some(v)
      case ArrayHolder(h) => {
        val p = getParameterTypeOf(paramName)
        debug(s"convert array holder:$h into $p")
        TypeConverter.convert(h, p.surface)
      }
    }
  }
}

class SimpleObjectBuilder(surface: Surface) extends ObjectBuilder with StandardBuilder with LogSupport {
  require(surface.objectFactory.isDefined, s"No object factory is found for ${surface}")

  import ObjectBuilder._

  protected def findParameter(name: String) = {
    assert(surface != null)
    val cname = name.canonicalName
    surface.params.find(_.name.canonicalName == cname)
  }

  protected def defaultValues = {
    // collect default values of the object
    val prop = Map.newBuilder[String, Any]

    // get the default values of the constructor
    for (p <- surface.params; v <- p.getDefaultValue) {
      trace(s"set default parameter $p to $v")
      prop += p.name.canonicalName -> v
    }
    val r = prop.result
    trace(s"surface ${surface}. values to set: $r")
    r
  }

  def build: Any = {
    trace(s"holder contents: $holder")
    val factory = surface.objectFactory.get
    val args = for (p <- surface.params) yield {
      get(p.name.canonicalName).getOrElse(Zero.zeroOf(p.surface))
    }
    trace(s"build: ${surface} from args:${args.mkString(", ")}")
    factory.newInstance(args)
  }
}
