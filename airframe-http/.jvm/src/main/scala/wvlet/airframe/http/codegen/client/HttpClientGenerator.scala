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
package wvlet.airframe.http.codegen.client

import wvlet.airframe.http.codegen.HttpClientIR.{ClientServiceDef, ClientServicePackages, ClientSourceDef}
import wvlet.airframe.http.codegen.client.ScalaHttpClientGenerator.indent
import wvlet.airframe.surface.Surface

/**
  */
trait HttpClientGenerator {
  def name: String
  def defaultFileName: String
  def defaultClassName: String
  def generate(src: ClientSourceDef): String
}

object HttpClientGenerator {

  private[codegen] def fullTypeNameOf(s: Surface): String = {
    s match {
      case p if p.isPrimitive =>
        p.name
      case _ =>
        s.fullName.replaceAll("\\$", ".")
    }
  }

  private[client] implicit class RichSurface(val s: Surface) extends AnyVal {
    def fullTypeName: String = fullTypeNameOf(s)
  }

  def predefinedClients: Seq[HttpClientGenerator] =
    Seq(
      AsyncClientGenerator,
      SyncClientGenerator,
      ScalaJSClientGenerator,
      GrpcClientGenerator
    )

  def findClient(name: String): Option[HttpClientGenerator] = {
    predefinedClients.find(_.name == name)
  }

  private[client] def generateNestedStub(src: ClientSourceDef)(serviceStub: ClientServiceDef => String): String = {
    // Traverse nested packages
    def traverse(p: ClientServicePackages): String = {
      val serviceStubBody =
        p.services.map(svc => serviceStub(svc)).mkString("\n")
      val childServiceStubBody = p.children.map(traverse(_)).mkString("\n")

      val body =
        s"""${serviceStubBody}
           |${childServiceStubBody}""".stripMargin.trim
      if (p.packageLeafName.isEmpty) {
        body
      } else {
        s"""object ${p.packageLeafName} {
           |${indent(body)}
           |}""".stripMargin
      }
    }

    traverse(src.classDef.toNestedPackages)
  }
}
