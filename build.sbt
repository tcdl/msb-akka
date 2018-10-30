
val akka_version = "2.4.17"

lazy val root = (project in file(".")).
  settings (
    organization := "io.github.tcdl.msb",
    name := "msb-akka",
    version := "0.1.19-SNAPSHOT",
    scalaVersion := "2.12.7",
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-encoding", "utf8"
    ),
    bintrayOrganization := Some("tcdl"),
    bintrayRepository := "releases",
    libraryDependencies ++= (dependencies ++ testDependencies),
    resolvers ++= dependencyResolvers,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishArtifact in (Test, packageBin) := true,
    crossScalaVersions := Seq("2.11.8", "2.12.7")
  )

val dependencyResolvers = Seq(
  "TCDL" at "https://dl.bintray.com/tcdl/releases",
  "Maven Central" at "http://repo1.maven.org/maven2"
)

val dependencies = Seq (
  //TODO: fix for SBT issue https://github.com/sbt/sbt/issues/2451
  "io.github.tcdl.msb" % "msb-java-core" % "1.6.4" artifacts(
    Artifact("msb-java-core", "jar", "jar", classifier = None, configurations = List(Compile), url = None),
    Artifact("msb-java-core", "jar", "jar", classifier = Some("tests"), configurations = List(Test), url = None)
    ),
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.5",
  "com.typesafe.akka" %% "akka-actor" % akka_version
)

val testDependencies = Seq (
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akka_version % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test"
)



