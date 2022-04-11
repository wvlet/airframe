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
package wvlet.airframe.rx

/**
  * This code is copied from airframe-di so that airframe-rx doesn't need to depend on airframe-di.
  */
private[rx] object LazyF0 {
  def apply[R](f: => R): LazyF0[R] = new LazyF0(f)
}

/**
  * This class is used to obtain the class names of the call-by-name functions (Function0[R]).
  *
  * This wrapper do not directly access the field f (Function0[R]) in order to avoid the evaluation of the function.
  * @param f
  * @tparam R
  */
private[rx] class LazyF0[+R](f: => R) extends Serializable with Cloneable {
  // Generates uuid to make sure the identity of this LazyF0 instance after serde
  private val objectId = new Object().hashCode()

  def copy: LazyF0[R] = clone().asInstanceOf[this.type]

  /**
    * Obtain the function class
    *
    * @return
    */
  def functionClass: Class[_] = {
    val field = this.getClass.getDeclaredField("f")
    field.get(this).getClass
  }

  def functionInstance: Function0[R] = {
    this.getClass.getDeclaredField("f").get(this).asInstanceOf[Function0[R]]
  }

  /**
    * This definition is necessary to let compiler generate the private field 'f' that holds a reference to the
    * call-by-name function.
    *
    * @return
    */
  def eval: R = f

  override def hashCode(): Int = objectId

  def canEqual(other: Any): Boolean = other.isInstanceOf[LazyF0[_]]

  override def equals(other: Any): Boolean = {
    other match {
      case that: LazyF0[_] =>
        // Scala 2.12 generates Lambda for Function0, and the class might be generated every time, so
        // comparing functionClasses doesn't work
        (that canEqual this) && this.objectId == that.objectId
      case _ => false
    }
  }
}
