
val akka_version = "2.3.9"

lazy val root = (project in file(".")).
  settings (
    organization := "io.github.tcdl.msb",
    name := "msb-akka",
    version := "0.1.12-SNAPSHOT",
    scalaVersion := "2.11.5",
    bintrayOrganization := Some("tcdl"),
    bintrayRepository := "releases",
    libraryDependencies ++= (dependencies ++ testDependencies),
    resolvers ++= dependencyResolvers,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishArtifact in (Test, packageBin) := true
  )

val dependencyResolvers = Seq(
  "TCDL" at "https://dl.bintray.com/tcdl/releases",
  "Maven Central" at "http://repo1.maven.org/maven2"
)

val dependencies = Seq (
  //TODO: fix for SBT issue https://github.com/sbt/sbt/issues/2451
  "io.github.tcdl.msb" % "msb-java-core" % "1.4.6" artifacts(
    Artifact("msb-java-core", "jar", "jar", classifier = None, configurations = List(Compile), url = None),
    Artifact("msb-java-core", "jar", "jar", classifier = Some("tests"), configurations = List(Test), url = None)
    ),
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.3",
  "com.typesafe.akka" %% "akka-actor" % akka_version
)

val testDependencies = Seq (
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akka_version % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test"
)



