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

//--------------------------------------
//
// ClosureSerializer.scala
// Since: 2012/12/28 0:22
//
//--------------------------------------

import java.io._
import java.lang.reflect.Constructor
import java.net.URL
import java.util.UUID

import org.objectweb.asm._
import org.objectweb.asm.tree.analysis._
import org.objectweb.asm.tree.{MethodInsnNode, MethodNode}
import wvlet.log.LogSupport
import wvlet.obj.Primitive

import scala.collection.mutable.Set
import scala.language.existentials

/**
  * Closure serializer
  */
private[airframe] object ClosureSerializer extends LogSupport {

  private val accessedFieldTable = collection.mutable.Map[Class[_], Map[String, Set[String]]]()

  case class OuterObject(obj: AnyRef, cl: Class[_]) {
    override def toString = s"${cl.getName}"
  }

  private def isClosure(cl: Class[_]) = cl.getName.contains("$anonfun$")

  private def getOuterObjects(obj: AnyRef, cl: Class[_]): List[OuterObject] = {
    for (f <- cl.getDeclaredFields if f.getName == "$outer") {
      f.setAccessible(true)
      val outer = f.get(obj)
      val e = OuterObject(outer, f.getType)
      if (isClosure(e.cl)) {
        return e :: getOuterObjects(e.obj, e.cl)
      }
      else {
        return e :: Nil
      }
    }
    return Nil
  }

  def cleanupF1[A, B](f: A => B): A => B = {
    val cl = f.getClass
    val accessedFields = accessedFieldTable.getOrElseUpdate(cl, findAccessedFieldsInClosureF1(cl))

    // cleanup unused fields recursively
    val obj_clean = cleanupObject(f, cl, accessedFields)
    obj_clean.asInstanceOf[A => B]
  }

  def cleanupF1_3[A, B, C](f: (A, B) => C): (A, B) => C = {
    val cl = f.getClass
    val accessedFields = accessedFieldTable.getOrElseUpdate(cl, findAccessedFieldsInClosureF1(cl))

    // cleanup unused fields recursively
    val obj_clean = cleanupObject(f, cl, accessedFields)
    obj_clean.asInstanceOf[(A, B) => C]
  }

  def cleanupClosure[R](f: LazyF0[R]) = {
    val cl = f.functionClass
    trace(s"cleanup closure class: ${cl.getName}")

    //    val outer = getOuterObjects(f.functionInstance, f.functionClass)
    //    debug(s"outer: [${outer.mkString(", ")}]")

    //    val inner = findInnerFieldAccess(f.functionClass)
    //    debug(s"inner: [${inner.mkString(", ")}}]")

    val accessedFields = accessedFieldTable.getOrElseUpdate(cl, findAccessedFieldsInClosureF0(cl))

    // cleanup unused fields recursively
    val obj_clean = cleanupObject(f.functionInstance, f.functionClass, accessedFields)
    obj_clean
  }

  def cleanupObject(obj: AnyRef, cl: Class[_], accessedFields: Map[String, Set[String]]) = {
    trace(s"cleanup object: class ${cl.getName}")
    if (cl.isPrimitive || cl.isArray) {
      obj
    }
    else {
      obj match {
        case d: Design => obj // TODO clean f
        case a: UUID => obj
        case u: URL => obj
        case s: String => obj
        // var references (xxxRef) must be serialized
        case a: scala.runtime.IntRef => obj
        case a: scala.runtime.ShortRef => obj
        case a: scala.runtime.LongRef => obj
        case a: scala.runtime.FloatRef => obj
        case a: scala.runtime.DoubleRef => obj
        case a: scala.runtime.BooleanRef => obj
        case a: scala.runtime.ByteRef => obj
        case a: scala.runtime.CharRef => obj
        case a: scala.runtime.ObjectRef[_] => obj
        case st if cl.getName.startsWith("org.scalatest") => null
        case st if cl.getName.startsWith("akka.actor") => null
        case _ => instantiateClass(obj, cl, accessedFields)
      }
    }
  }

  private def instantiateClass(orig: AnyRef, cl: Class[_], accessedFields: Map[String, Set[String]]): Any = {
    val m = classOf[ObjectStreamClass].getDeclaredMethod("getSerializableConstructor", classOf[Class[_]])
    m.setAccessible(true)
    val constructor = m.invoke(null, cl).asInstanceOf[Constructor[_]]

    if (constructor == null) {
      trace(s"Cannot instantiate the class. Use the original data: ${orig.getClass.getName}")
      orig
    }
    else {
      trace("create a blank instance")
      val obj = constructor.newInstance()

      // copy accessed fields
      val clName = cl.getName

      for (accessed <- accessedFields.getOrElse(clName, Set.empty)) {
        try {
          val f = orig.getClass.getDeclaredField(accessed)
          f.setAccessible(true)
          trace(s"set field $accessed:${f.getType.getName} in $clName")
          val v = f.get(orig)
          val v_cleaned = cleanupObject(v, f.getType, accessedFields)
          f.set(obj, v_cleaned)
        }
        catch {
          case e: NoSuchFieldException =>
            warn(s"no such field: $accessed in class ${cl.getName}")
        }
      }
      obj
    }
  }

  def accessedFieldsInClosure[A, B](target: Class[_], closure: Function[A, B]): Seq[String] = {
    new ParamAccessFinder(target).findFrom(closure)
  }

  /**
    * Find the accessed parameters of the target class in the closure.
    * This function is used for optimizing data retrieval in Silk.
    *
    * @param target
    * @param closure
    * @return
    */
  def accessedFields(target: Class[_], closure: AnyRef): Seq[String] = {
    new ParamAccessFinder(target).findFrom(closure.getClass)
  }

  def accessedFieldsIn[R](f: => R) = {
    val lf = LazyF0(f)
    val cl = lf.functionClass
    val accessedFields = accessedFieldTable.getOrElseUpdate(cl, findAccessedFieldsInClosureF0(cl))
    trace(s"accessed fields: ${accessedFields.mkString(", ")}")
    accessedFields
  }

  def serializeClosure[R](f: => R): Array[Byte] = {
    val lf = LazyF0(f)
    trace(s"Serializing closure class ${lf.functionClass}")
    val clean = cleanupClosure(lf)
    val b = new ByteArrayOutputStream()
    val o = new ObjectOutputStream(b)
    o.writeObject(clean)
    //o.writeObject(lf.functionInstance)
    o.flush()
    o.close
    b.close
    val ser = b.toByteArray
    trace(f"closure size: ${ser.length}%,d")
    ser
  }

  def serializeF1[A, B](f: A => B): Array[Byte] = {
    trace(s"Serializing closure class ${f.getClass}")
    val clean = cleanupF1(f)
    val b = new ByteArrayOutputStream()
    val o = new ObjectOutputStream(b)
    o.writeObject(clean)
    o.flush()
    o.close
    b.close
    val ser = b.toByteArray
    trace(f"closure size: ${ser.length}%,d")
    ser
  }

  private[airframe] def getClassReader(cl: Class[_]): ClassReader = {
    new ClassReader(cl.getResourceAsStream(cl.getName.replaceFirst("^.*\\.", "") + ".class"))
  }

  private[airframe] def descName(s: String) = s.replace(".", "/")

  private[airframe] def clName(s: String) = s.replace("/", ".")

  case class MethodCall(opcode: Int, name: String, desc: String, owner: String, stack: IndexedSeq[String]) {
    def methodDesc = s"${
      name
    }${desc}"
    override def toString = s"MethodCall[$opcode](${name}${desc}, owner:$owner, stack:[${stack.mkString(", ")}])"
    def toReportString = s"MethodCall[$opcode]:${
      name
    }${desc}\n -owner:${
      owner
    }${
      if (stack.isEmpty) {
        ""
      }
      else {
        "\n -stack:\n  -" + stack.mkString("\n  -")
      }
    }"
  }

  def findAccessedFieldsInClosureF1(cl: Class[_]) = {
    // Stack contents should have argument type
    findAccessedFieldsInClosure(cl, methodSig = Seq("()Ljava/lang/Object;", "(Ljava/lang/Object;)Ljava/lang/Object;"), IndexedSeq(cl.getName, classOf[java.lang.Object].getName))
  }

  def findAccessedFieldsInClosureF0(cl: Class[_]) = {
    findAccessedFieldsInClosure(cl, Seq("()V", "()Ljava/lang/Object;"), IndexedSeq(cl.getName))
  }

  def findAccessedFieldsInClosure(cl: Class[_], methodSig: Seq[String], initialStack: IndexedSeq[String]) = {
    val baseClsName = cl.getName
    var visited = Set[MethodCall]()
    var stack = methodSig.map(MethodCall(Opcodes.INVOKEVIRTUAL, "apply", _, cl.getName, initialStack)).toList
    var accessedFields = Map[String, Set[String]]() // (baseClsName -> {if(baseClsName.contains("$anon")) Set("$outer") else Set.empty[String]})
    while (!stack.isEmpty) {
      val mc = stack.head
      stack = stack.tail
      if (!visited.contains(mc)) {
        visited += mc
        try {
          trace(s"current head: $mc")
          // Resolve a class defining the target method. For example, a class overriding apply(x) in Function1 needs to be scanned.
          val methodOwner = mc.opcode match {
            case Opcodes.INVOKESTATIC => mc.owner
            case Opcodes.INVOKESPECIAL => mc.owner
            case _ => mc.stack.headOption getOrElse (mc.owner)
          }
          // TODO handle <init> method call
          val scanner = new ClassScanner(methodOwner, mc.methodDesc, mc.opcode, mc.stack)
          val targetCls = Class.forName(methodOwner, false, Thread.currentThread().getContextClassLoader)
          if (Primitive.isPrimitive(targetCls) || targetCls == classOf[AnyRef] || methodOwner.startsWith("java.")) {
            // do nothing
          }
          else if (methodOwner.startsWith("scala.")) {
            if (mc.desc.contains("scala/Function1;")) {
              for (anonfun <- mc.stack.filter(x => x.startsWith(baseClsName) && x.contains("$anonfun"))) {
                val m = MethodCall(Opcodes.INVOKESTATIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", anonfun, IndexedSeq(anonfun))
                debug(s"add $m to stack")
                stack = m :: stack
              }
            }
          }
          else {
            getClassReader(targetCls).accept(scanner, ClassReader.SKIP_DEBUG)
            for ((cls, lst) <- scanner.accessedFields) {
              accessedFields += cls -> (accessedFields.getOrElse(cls, {
                if (cls.contains("$anon")) {
                  Set.empty[String]
                }//Set("$outer") // include $outer for anonymous functions
                else {
                  Set.empty[String]
                }
              }) ++ lst)
            }
            for (m <- scanner.found) {
              stack = m :: stack
            }
          }
        }
        catch {
          case e: Exception => warn(e.getMessage)
        }
      }
    }

    // Resolve $outer field accesses
    val outerClHierarchy = findOuterClasses(cl)
    trace(s"outer classes: $outerClHierarchy")
    outerClHierarchy.reverse.find(outer => accessedFields.contains(outer.getName)) match {
      case Some(outer) => // need to create links to this outer class
        for (left <- outerClHierarchy.takeWhile(_ != outer)) {
          val ln = left.getName
          accessedFields += ln -> (accessedFields.getOrElse(ln, Set.empty) ++ Set("$outer"))
        }
      case None =>
    }

    if (!accessedFields.isEmpty) {
      debug(s"accessed fields: ${accessedFields.mkString(", ")}")
    }
    accessedFields
  }

  private def findOuterClasses(cl: Class[_]): List[Class[_]] = {
    def loop(c: Class[_]): List[Class[_]] = {
      try {
        if (c.getName.contains("$anon")) {
          val outer = c.getDeclaredField("$outer")
          val outerCl = outer.getType
          c :: loop(outerCl)
        }
        else {
          List(c)
        }
      }
      catch {
        case e: NoSuchFieldException => List(c) // do nothing
      }
    }
    loop(cl)
  }

  private def findInnerFieldAccess(cl: Class[_]): Set[Class[_]] = {
    val f = new InnerFieldAccessFinder(cl)
    f.find
    f.found
  }

  private class InnerFieldAccessFinder(cl: Class[_]) {
    var found = Set[Class[_]]()
    var stack = List[Class[_]](cl)

    private class InitDefScanner extends ClassVisitor(Opcodes.ASM4) {
      var current: String = _
      override def visit(version: Int, access: Int, name: String, signature: String, superName: String, interfaces: Array[String]) {
        logger.info(s"visit $name")
        current = name
      }

      override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]) =
        new MethodVisitor(Opcodes.ASM4) {
          override def visitMethodInsn(opcode: Int, owner: String, name: String, desc: String) {
            val argTypes = Type.getArgumentTypes(desc)
            logger.debug(s"visit invokespecial: $opcode ${name}, desc:$desc, argTypes:(${argTypes.mkString(",")}) in\nclass ${clName(owner)}")
            if (name == "<init>"
              && argTypes.length > 0
              && argTypes(0).toString.startsWith("L") // for object?
              && argTypes(0).getInternalName == current) {

              val ownerCl = clName(owner)
              logger.info(s"push accessed class: $ownerCl")
              stack = Class.forName(ownerCl, false, Thread.currentThread().getContextClassLoader) :: stack
            }
          }
        }
    }

    def find {
      logger.debug("Scanning inner classes")
      while (!stack.isEmpty) {
        val scanner = new InitDefScanner
        val c = stack.head
        if (!found.contains(c)) {
          found += c
          getClassReader(c).accept(scanner, 0)
        }
        stack = stack.tail
      }
    }
  }
}

import wvlet.airframe.ClosureSerializer._

private[airframe] class ParamAccessFinder(target: Class[_]) extends LogSupport {

  private val targetClassDesc = descName(target.getName)
  private var visitedClass    = Set.empty[Class[_]]
  private var currentTarget   = List("apply")

  private var accessedFields = Seq.newBuilder[String]
  trace(s"targetClass:${clName(target.getName)}")

  def findFrom[A, B](closure: Function[A, B]): Seq[String] = {
    info(s"findFrom closure:${closure.getClass.getName}")
    findFrom(closure.getClass)
  }

  def findFrom(cl: Class[_]): Seq[String] = {
    if (visitedClass.contains(cl)) {
      return Seq.empty
    }

    val visitor = new ClassVisitor(Opcodes.ASM4) {
      override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]) = {
        if (desc.contains(targetClassDesc) && name.contains(currentTarget.head)) {
          logger.debug(s"visit method ${name}${desc} in ${clName(cl.getName)}")
          new MethodVisitor(Opcodes.ASM4) {
            override def visitMethodInsn(opcode: Int, owner: String, name: String, desc: String) {
              if (opcode == Opcodes.INVOKEVIRTUAL) {

                logger.trace(s"visit invokevirtual: $opcode ${name}$desc in $owner")
                if (clName(owner) == target.getName) {
                  logger.debug(s"Found a accessed parameter: $name")
                  accessedFields += name
                }

                //info(s"Find the target function: $name")
                val ownerCls = Class.forName(clName(owner))
                currentTarget = name :: currentTarget
                findFrom(ownerCls)
                currentTarget = currentTarget.tail
              }
            }
          }
        }
        else {
          new MethodVisitor(Opcodes.ASM4) {}
        } // empty visitor
      }
    }
    visitedClass += cl
    getClassReader(cl).accept(visitor, 0)

    accessedFields.result
  }
}

private[airframe] class ClassScanner(owner: String, targetMethod: String, opcode: Int, argStack: IndexedSeq[String]) extends ClassVisitor(Opcodes.ASM4) with LogSupport {

  //trace(s"Scanning method [$opcode] $targetMethod, owner:$owner, stack:$argStack)")

  var accessedFields = Map[String, Set[String]]()
  var found          = List[MethodCall]()

  override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]) = {
    val fullDesc = s"${name}${desc}"

    if (fullDesc != targetMethod) {
      null // empty visitor
    }
    else {
      trace(s"visit method: $fullDesc")
      // Replace method descriptor to use argStack variables in Analyzer
      // TODO return type might need to be changed
      val ret = Type.getReturnType(desc)
      def toDesc(ot: Type, t: String) = {
        try {
          t match {
            case arr if arr.endsWith("[]") =>
              val elemType = Class.forName(arr.dropRight(2), false, Thread.currentThread().getContextClassLoader)
              s"[${Type.getDescriptor(elemType)}"
            case _ =>
              val cl = Class.forName(t, false, Thread.currentThread().getContextClassLoader)
              Type.getDescriptor(cl)
          }
        }
        catch {
          case e: Exception => ot.getDescriptor
        }
      }

      // Resolve the stack contents to the actual types
      // zip argStack and original type descriptors
      val methodArgTypes = Type.getArgumentTypes(desc)
      trace(s"method arg types: ${methodArgTypes.mkString(", ")}")
      val newArgDescs = methodArgTypes.reverse.zip(argStack.reverse.take(methodArgTypes.length)).map {
        case (ot: Type, t: String) => toDesc(ot, t)
      }.reverse
      //        val newArg = opcode match {
      //          case Opcodes.INVOKESTATIC =>
      //            argStack.map(toDesc).mkString
      //          case _ => if(argStack.length > 1) argStack.drop(1).map(toDesc).mkString else ""
      //        }
      val newDesc = s"(${newArgDescs.mkString})$ret"
      trace(s"Replace desc\nold:$desc\nnew:$newDesc")
      val mn = new MethodNode(Opcodes.ASM4, access, name, newDesc, signature, exceptions)
      new MethodScanner(access, name, desc, signature, exceptions, mn)
    }
  }

  class MethodScanner(access: Int, name: String, desc: String, signature: String, exceptions: Array[String], m: MethodNode)
    extends MethodVisitor(Opcodes.ASM4, m) {

    override def visitFieldInsn(opcode: Int, fieldOwner: String, name: String, desc: String) {
      super.visitFieldInsn(opcode, fieldOwner, name, desc)
      if (opcode == Opcodes.GETFIELD) {
        // || opcode == Opcodes.GETSTATIC) {
        //debug(s"visit field insn: $opcode name:$name, owner:$owner desc:$desc")
        val fclName = clName(fieldOwner)
        //if(!fclName.startsWith("scala.") && !fclName.startsWith("xerial.core.")) {
        logger.trace(s"Found an accessed field: $name in class $fclName")
        accessedFields += fclName -> (accessedFields.getOrElse(fclName, Set.empty) + name)
        //}
      }
    }

    override def visitEnd() {
      super.visitEnd()
      try {
        val a = new Analyzer(new SimpleVerifier())
        val mn = mv.asInstanceOf[MethodNode]
        logger.trace(s"analyze: owner:$owner, method:$name")
        a.analyze(owner, mn)
        val inst = for (i <- 0 until mn.instructions.size()) yield mn.instructions.get(i)
        //trace(s"instructions ${inst.mkString(", ")}, # of frames:${a.getFrames.length}")
        for ((f, m: MethodInsnNode) <- a.getFrames.zip(inst) if f != null) {
          val stack = (for (i <- 0 until f.getStackSize) yield f.getStack(i).asInstanceOf[BasicValue].getType.getClassName).toIndexedSeq
          val local = (for (i <- 0 until f.getLocals) yield f.getLocal(i))
          m.owner match {
            case p if p.startsWith("xerial.core") =>
            case _ =>
              val mc = MethodCall(m.getOpcode, m.name, m.desc, clName(m.owner), stack)
              logger.trace(s"Found ${mc.toReportString}\n -local\n  -${local.mkString("\n  -")}")
              found = mc :: found
          }
        }
      }
      catch {
        case e: Exception => logger.error(e)
      }
    }
  }

}
