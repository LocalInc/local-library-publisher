import sbt.Keys._
import sbtassembly.AssemblyPlugin.assemblySettings

lazy val commonSettings = Seq(
  organization := "com.local.publisher.gcc",
  version := "1.0.0",
  scalaVersion := "2.11.8",
  fork in run := true,
  parallelExecution in ThisBuild := false,
  parallelExecution in Test := false
)

lazy val projectAssemblySettings = Seq(
  assemblyJarName in assembly := name.value + ".jar",
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
    case PathList("META-INF", "sun-jaxb.episode") => MergeStrategy.first
    case other => MergeStrategy.defaultMergeStrategy(other)
  }
)


lazy val commonResolvers = Seq(
  "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/releases/",
  Resolver.sonatypeRepo("snapshots"),
  Resolver.typesafeRepo("releases"),
  Resolver.mavenLocal
)

lazy val versions = new {
  val gcs = "0.8.0-beta"
  val gcPubSub = "0.8.0"

  val akkaHttpSprayJson = "10.0.0"
  val akkaHttpJackson = "10.0.0"
}

lazy val publisher = project.in(file("publisher")).
  settings(commonSettings: _*).
  settings(assemblySettings: _*).
  settings(projectAssemblySettings: _*).
  settings(
    name := "local-publisher",
    resolvers ++= Seq(
      Resolver.bintrayRepo("websudos", "oss-releases"),
      "The New Motion Public Repo" at "http://nexus.thenewmotion.com/content/groups/public/"
    ) ++ commonResolvers,

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-spray-json" % versions.akkaHttpSprayJson,
      "com.typesafe.akka" %% "akka-http-jackson" % versions.akkaHttpJackson,
      "com.google.cloud" % "google-cloud-pubsub" % versions.gcPubSub
    )
  )