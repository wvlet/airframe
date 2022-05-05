ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

enablePlugins(AirframeHttpPlugin)

name    := "Open API Test"
version := "1.0.0"

airframeHttpOpenAPIPackages := Seq("example.api")
airframeHttpOpts            := "-l debug"
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe-http" % sys.props("airframe.version")
)

TaskKey[Unit]("check") := {
  val yaml = IO.read(target.value / "openapi.yaml")
  val expected = Seq(
    "title: 'Open API Test'",
    "version: '1.0.0'",
    "/example.api.OpenAPIRPCExample/rpcWithPrimitiveAndOption:",
    "/example.api.OpenAPIRPCExample/rpcWithPrimitive:",
    "$ref: '#/components/schemas/OpenAPIRPCExample.RPCRequest'",
    "OpenAPIRPCExample.RPCRequest:"
  )
  expected.foreach { x =>
    if (!yaml.contains(x)) {
      sys.error(s"Generated YAML file doesn't contain line: ${x}")
    }
  }
}
