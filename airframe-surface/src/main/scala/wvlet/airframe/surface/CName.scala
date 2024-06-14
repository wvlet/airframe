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

package wvlet.airframe.surface

import java.util.regex.Pattern
import wvlet.airframe.surface
import scala.collection.mutable.WeakHashMap

//--------------------------------------
//
// CName.scala
// Since: 2012/02/16 14:58
//
//--------------------------------------

/**
  * Utility for managing names written in different spellings. For example, variable name <i>localAddress</i> can be
  * written as "local address", "local_address", etc.
  *
  * CanonicalName is the representative name of these variants.
  *
  * <pre> CName("localAddress") == CName("local address") == CName("local_address") </pre>
  *
  * @author
  *   leo
  */
object CName {
  private val cnameTable = surface.newCacheMap[String, CName]

  def apply(name: String): CName = {
    cnameTable.getOrElseUpdate(name, new CName(toCanonicalName(name), toNaturalName(name)))
  }

  private val paramNameReplacePattern = Pattern.compile("[\\s-_]");
  private val canonicalNameTable      = surface.newCacheMap[String, String]
  private val naturalNameTable        = surface.newCacheMap[String, String]

  private def isSplitChar(c: Char)    = c.isUpper || c == '_' || c == '-' || c == ' '
  private def isUpcasePrefix(c: Char) = c.isUpper || c.isDigit

  def toCanonicalName(paramName: String): String = {
    if paramName == null then {
      ""
    } else {
      canonicalNameTable.getOrElseUpdate(
        paramName,
        paramNameReplacePattern.matcher(paramName).replaceAll("").toLowerCase
      )
    }
  }

  def toNaturalName(varName: String): String = {
    if varName == null then {
      ""
    } else {
      def translate(varName: String) = {
        def wikiNameComponents: List[String] = {
          findWikiNameComponent(0)
        }

        def findWikiNameComponent(index: Int): List[String] = {
          val len = varName.length

          def skipUpcasePrefix(i: Int): Int = {
            if i < len && isUpcasePrefix(varName(i)) then {
              skipUpcasePrefix(i + 1)
            } else {
              i
            }
          }

          def parseWikiComponent(i: Int): Int =
            if i < len && !isSplitChar(varName(i)) then {
              parseWikiComponent(i + 1)
            } else {
              i
            }

          val start  = index
          var cursor = index
          if cursor >= len then {
            Nil
          } else {
            cursor = skipUpcasePrefix(cursor)
            // Upcase prefix length is longer than or equals to 2
            if cursor - start >= 2 then {
              if start == 0 && varName(cursor).isLower then {
                cursor -= 1
              }
              varName.substring(start, cursor) :: findWikiNameComponent(cursor)
            } else {
              cursor = parseWikiComponent(cursor)
              if start < cursor then {
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
}

/**
  * Canonical name. This name is used as a common name of wording variants (e.g., difference of capital letter usage,
  * hyphenation, etc.)
  *
  * @param canonicalName
  * @param naturalName
  */
class CName(val canonicalName: String, val naturalName: String) extends Comparable[CName] {
  def compareTo(o: CName) = canonicalName.compareTo(o.canonicalName)
  override def toString   = canonicalName

  override def hashCode = canonicalName.hashCode()
  override def equals(other: Any) = {
    other match {
      case o: CName if canonicalName.equals(o.canonicalName) => true
      case _                                                 => false
    }
  }

  lazy val snakeCase: String = naturalName.toLowerCase.replace(' ', '_')
  lazy val dashCase: String  = naturalName.toLowerCase.replace(' ', '-')
  lazy val upperCamelCase: String = {
    val sb               = new StringBuilder()
    var prevIsWhitespace = false
    naturalName.toLowerCase.map { c =>
      if c != ' ' then {
        if sb.length == 0 || prevIsWhitespace then {
          sb.append(c.toUpper)
        } else {
          sb.append(c)
        }
      }
      prevIsWhitespace = (c == ' ')
    }
    sb.toString
  }

  lazy val lowerCamelCase: String = {
    val sb               = new StringBuilder()
    var prevIsWhitespace = false
    naturalName.toLowerCase.map { c =>
      if c != ' ' then {
        if prevIsWhitespace then {
          sb.append(c.toUpper)
        } else {
          sb.append(c)
        }
      }
      prevIsWhitespace = (c == ' ')
    }
    sb.toString
  }
}
