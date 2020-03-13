val akkaV = "2.5.6"
val akkaHttpVer = "10.0.9"
val circeVersion = "0.12.2"
val silencerVersion = "1.6.0"

val validate = Def.taskKey[Unit]("Validates entire project")

val crossScalaVersionsValues = Seq("2.12.10", "2.13.1")

val commonSettings = Seq(
  organization := "io.morgaroth",
  scalaVersion := "2.13.1",
  crossScalaVersions := crossScalaVersionsValues,
  resolvers ++= Seq(
    ("Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/").withAllowInsecureProtocol(true),
    Resolver.bintrayRepo("morgaroth", "maven"),
  ),
  scalacOptions ++= Seq(
    "-unchecked", "-deprecation", "-encoding", "utf8", "-Xfatal-warnings", "-feature",
    "-language:higherKinds", "-language:postfixOps", "-language:implicitConversions",
    "-Ywarn-unused:imports",
    "-P:silencer:checkUnused",
  ),
  libraryDependencies ++= Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
  ),

  logBuffered := false,

  testOptions in Test += Tests.Filter(suiteName => !suiteName.endsWith("ISpec")),

  // Bintray
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("https://gitlab.com/morgaroth/gitlab4s.git"),
)


val core = project
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.0.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "com.typesafe" % "config" % "1.3.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.wickedsource" % "diffparser" % "1.0",
      "io.github.java-diff-utils" % "java-diff-utils" % "4.5",
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    )
  )

val sttp = project.in(file("sttp")).dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-sttp",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client" %% "core" % "2.0.0-RC6",
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    )
  )

val akka = project.in(file("akka-http")).dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-akka-http",
    libraryDependencies ++= Seq(

    )
  )

val gitlab4s = project.in(file(".")).aggregate(core, sttp, akka)
  .settings(
    name := "gitlab4s",
    publish := {},
    publishLocal := {},
    crossScalaVersions := crossScalaVersionsValues,

    validate := Def.sequential {
      Test / test
      // tut.value
    }.value,

    // Release
    releaseTagComment := s"Releasing ${(version in ThisBuild).value} [skip ci]",
    releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [skip ci]",
    releaseNextCommitMessage := s"Setting version to ${(version in ThisBuild).value} [skip ci]",
    releaseCrossBuild := true,
  )