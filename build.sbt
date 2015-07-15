lazy val root = (project in file(".")).
  settings (
    organization := "io.github.tcdl.msb",
    name := "msb-akka",
    version := "0.1.0",
    scalaVersion := "2.11.4",
    libraryDependencies ++= dependencies,
    resolvers ++= dependencyResolvers
  )

val dependencyResolvers = Seq(
  "TCDL" at "https://dl.bintray.com/tcdl/releases",
  "Maven Central" at "http://repo1.maven.org/maven2"
)

val dependencies = Seq (
  "io.github.tcdl.msb" % "msb-java" % "1.0.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.3"
)
