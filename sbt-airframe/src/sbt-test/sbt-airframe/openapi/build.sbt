enablePlugins(AirframeHttpPlugin)

name := "Open API Test"
version := "1.0.0"

airframeHttpOpenAPIPackages := Seq("example.api")
airframeHttpOpts := "-l debug"
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe-http" % sys.props("plugin.version")
)

TaskKey[Unit]("check") := {
  val yaml = IO.read(target.value / "openapi.yaml")
  val expected = Seq(
    "title: 'Open API Test'",
    "version: '1.0.0'",
    "/example.api.OpenAPIRPCExample/rpcWithPrimitiveAndOption:",
    "/example.api.OpenAPIRPCExample/rpcWithPrimitive:",
    "$ref: '#/components/schemas/example.api.OpenAPIRPCExample.RPCRequest'",
    "example.api.OpenAPIRPCExample.RPCRequest:"
  )
  expected.foreach { x =>
    if (!yaml.contains(x)) {
      sys.error(s"Generated YAML file doesn't contain line: ${x}")
    }
  }
}
