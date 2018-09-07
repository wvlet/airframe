enablePlugins(ScalaJSPlugin)

name := "airframe-log Scala.js example"
scalaVersion := "2.12.4"

// This is an application with a main method
scalaJSUseMainModuleInitializer := true

libraryDependencies += "org.wvlet.airframe" %%% "airframe-log" % "0.62"
