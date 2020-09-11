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
package wvlet.airframe.rx.html

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  */
private[html] object RxHtmlMacros {
  def code(c: sm.Context)(rxElements: c.Tree*): c.Tree = {
    import c.universe._

    val codes = for (rxElement <- rxElements) yield {
      val pos        = rxElement.pos
      val src        = pos.source
      val lineBlocks = src.content.slice(pos.start, pos.`end`).mkString.replaceAll("^\\{\\n", "").replaceAll("\\}$", "")

      val lines = lineBlocks.split("\n")
      val columnOffsetInLine = lines.headOption
        .map { firstLine =>
          firstLine.size - firstLine.stripLeading().size
        }.getOrElse(0)

      val trimmedSource = lines
        .map { line =>
          line.replaceFirst(s"^\\s{0,${columnOffsetInLine}}", "")
        }.mkString("\n")
      (rxElement, trimmedSource)
    }
    val elems  = codes.map(_._1).toSeq
    val source = codes.map(_._2).mkString("\n")
    q"wvlet.airframe.rx.html.RxCode(Seq(${elems:_*}), ${source})"
  }

}
