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

import java.util.regex.Pattern

import scala.collection.mutable.WeakHashMap

//--------------------------------------
//
// CName.scala
// Since: 2012/02/16 14:58
//
//--------------------------------------

/**
  * Utility for managing names written in different spellings. For example,
  * variable name <i>localAddress</i> can be written as "local address", "local_address", etc.
  *
  * CanonicalName is the representative name of these variants.
  *
  * <pre>
  * CName("localAddress") == CName("local address") == CName("local_address")
  * </pre>
  *
  * @author leo
  */
object CName {

  private val cnameTable = new WeakHashMap[String, CName]

  def apply(name: String): CName = {
    cnameTable.getOrElseUpdate(name, new CName(toCanonicalName(name), toNaturalName(name)))
  }

  private val paramNameReplacePattern = Pattern.compile("[\\s-_]");
  private val canonicalNameTable      = new WeakHashMap[String, String]
  private val naturalNameTable        = new WeakHashMap[String, String]

  def toCanonicalName(paramName: String): String = {
    if (paramName == null) {
      return paramName
    };

    canonicalNameTable.getOrElseUpdate(paramName, paramNameReplacePattern.matcher(paramName).replaceAll("").toLowerCase)
  }

  def toNaturalName(varName: String): String = {
    if (varName == null) {
      return null
    };

    def isSplitChar(c: Char)    = c.isUpper || c == '_' || c == '-' || c == ' '
    def isUpcasePrefix(c: Char) = c.isUpper || c.isDigit

    def translate(varName: String) = {
      //var components = Array[String]()

      def wikiNameComponents: List[String] = {
        findWikiNameComponent(0)
      }

      def findWikiNameComponent(index: Int): List[String] = {
        val len = varName.length

        def skipUpcasePrefix(i: Int): Int = {
          if (i < len && isUpcasePrefix(varName(i))) {
            skipUpcasePrefix(i + 1)
          } else {
            i
          }
        }

        def parseWikiComponent(i: Int): Int =
          if (i < len && !isSplitChar(varName(i))) {
            parseWikiComponent(i + 1)
          } else {
            i
          }

        val start  = index
        var cursor = index
        if (cursor >= len) {
          Nil
        } else {
          cursor = skipUpcasePrefix(cursor)
          // Upcase prefix length is longer than or equals to 2
          if (cursor - start >= 2) {
            if (start == 0 && varName(cursor).isLower) {
              cursor -= 1
            }
            varName.substring(start, cursor) :: findWikiNameComponent(cursor)
          } else {
            cursor = parseWikiComponent(cursor)
            if (start < cursor) {
              varName.substring(start, cursor).toLowerCase() :: findWikiNameComponent(cursor)
            } else {
              findWikiNameComponent(cursor + 1)
            }
          }
        }
      }

      val components = wikiNameComponents
      val nName      = components.mkString(" ")
      nName
    }

    naturalNameTable.getOrElseUpdate(varName, translate(varName))
  }

}

/**
  * Cannonical name. This name is used as a common name of wording variants (e.g., difference of capital letter usage, hyphenation, etc.)
  *
  * @param canonicalName
  * @param naturalName
  */
class CName(val canonicalName: String, val naturalName: String) extends Comparable[CName] {
  def compareTo(o: CName) = canonicalName.compareTo(o.canonicalName)
  override def toString   = canonicalName

  override def hashCode = canonicalName.hashCode()
  override def equals(other: Any) = {
    if (other.isInstanceOf[CName]) {
      canonicalName.equals(other.asInstanceOf[CName].canonicalName)
    } else {
      false
    }
  }

}
