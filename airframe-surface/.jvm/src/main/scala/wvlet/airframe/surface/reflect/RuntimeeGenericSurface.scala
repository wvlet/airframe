package wvlet.airframe.surface.reflect

import wvlet.airframe.surface.{Surface, Parameter, GenericSurface}
import wvlet.airframe.surface.ObjectFactory
import wvlet.log.LogSupport
import java.lang.reflect.{Constructor, InvocationTargetException}

/**
  * Used when we can use reflection to instantiate objects of this surface
  *
  * @param rawType
  * @param typeArgs
  * @param params
  */
class RuntimeGenericSurface(
    override val rawType: Class[_],
    override val typeArgs: Seq[Surface] = Seq.empty,
    override val params: Seq[Parameter] = Seq.empty,
    val outer: Option[AnyRef] = None,
    isStatic: Boolean
) extends GenericSurface(rawType, typeArgs, params, None)
    with LogSupport {
  self =>

  override def withOuter(outer: AnyRef): Surface = {
    new RuntimeGenericSurface(rawType, typeArgs, params, Some(outer), isStatic = false)
  }

  private class ReflectObjectFactory extends ObjectFactory {

    private def getPrimaryConstructorOf(cls: Class[_]): Option[Constructor[_]] = {
      val constructors = cls.getConstructors
      if (constructors.size == 0) {
        None
      } else {
        Some(constructors(0))
      }
    }

    private def getFirstParamTypeOfPrimaryConstructor(cls: Class[_]): Option[Class[_]] = {
      getPrimaryConstructorOf(cls).flatMap { constructor =>
        val constructorParamTypes = constructor.getParameterTypes
        if (constructorParamTypes.size == 0) {
          None
        } else {
          Some(constructorParamTypes(0))
        }
      }
    }
    //private val isStatic = mirror.classSymbol(rawType).isStatic
    private def outerInstance: Option[AnyRef] = {
      if (isStatic) {
        None
      } else {
        // Inner class
        outer.orElse {
          val contextClass = getFirstParamTypeOfPrimaryConstructor(rawType)
          val msg = contextClass
            .map(x =>
              s" Call Surface.of[${rawType.getSimpleName}] or bind[${rawType.getSimpleName}].toXXX where `this` points to an instance of ${x}"
            )
            .getOrElse(
              ""
            )
          throw new IllegalStateException(
            s"Cannot build a non-static class ${rawType.getName}.${msg}"
          )
        }
      }
    }

    // Create instance with Reflection
    override def newInstance(args: Seq[Any]): Any = {
      try {
        // We should not store the primary constructor reference here to avoid including java.lang.reflect.Constructor,
        // which is non-serializable, within this RuntimeGenericSurface class
        getPrimaryConstructorOf(rawType)
          .map { primaryConstructor =>
            val argList = Seq.newBuilder[AnyRef]
            if (!isStatic) {
              // Add a reference to the context instance if this surface represents an inner class
              outerInstance.foreach { x =>
                argList += x
              }
            }
            argList ++= args.map(_.asInstanceOf[AnyRef])
            val a = argList.result()
            if (a.isEmpty) {
              logger.trace(s"build ${rawType.getName} using the default constructor")
              primaryConstructor.newInstance()
            } else {
              logger.trace(s"build ${rawType.getName} with args: ${a.mkString(", ")}")
              primaryConstructor.newInstance(a: _*)
            }
          }
          .getOrElse {
            throw new IllegalStateException(s"No primary constructor is found for ${rawType}")
          }
      } catch {
        case e: InvocationTargetException =>
          logger.warn(
            s"Failed to instantiate ${self}: [${e.getTargetException.getClass.getName}] ${e.getTargetException.getMessage}\nargs:[${args
              .mkString(", ")}]"
          )
          throw e.getTargetException
        case e: Throwable =>
          logger.warn(
            s"Failed to instantiate ${self}: [${e.getClass.getName}] ${e.getMessage}\nargs:[${args
              .mkString(", ")}]"
          )
          throw e
      }
    }
  }

  override val objectFactory: Option[ObjectFactory] = {
    if (rawType.getConstructors.isEmpty) {
      None
    } else {
      Some(new ReflectObjectFactory())
    }
  }
}
