val circeVersion = "0.13.0"
//val circeVersion = "0.14.3-M3"
val circeExtVersion = "0.13.0"
val silencerVersion = "1.7.1"

val validate = Def.taskKey[Unit]("Validates entire project")

val projectScalaVersion = "2.13.4"
//val projectScalaVersion = "3.0.0-M3"
val crossScalaVersionsValues = Seq(projectScalaVersion, "2.12.12")

val commonSettings = Seq(
  organization := "io.morgaroth",
  scalaVersion := projectScalaVersion,
  crossScalaVersions := crossScalaVersionsValues,
  resolvers ++= Seq(
    ("Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/").withAllowInsecureProtocol(true),
    Resolver.bintrayRepo("morgaroth", "maven"),
  ),
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-encoding",
    "utf8",
    "-Xfatal-warnings",
    "-feature",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-Ywarn-unused:imports",
    "-P:silencer:checkUnused",
  ) ++ {
    if (scalaVersion.value.startsWith("2.13")) Seq("-Ymacro-annotations") else Seq.empty
  },
  libraryDependencies ++= Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full,
  ) ++ {
    if (scalaVersion.value.startsWith("2.12")) Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
    else Seq.empty
  },
  logBuffered := false,
  testOptions in Test += Tests.Filter(suiteName => !suiteName.endsWith("ISpec")),
  sources in doc := Seq.empty,
  // Bintray
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayVcsUrl := Some("https://gitlab.com/morgaroth/gitlab4s.git"),
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest-flatspec" % "3.2.3" % Test,
  "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.3" % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
)

val core = project
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-core",
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-core"            % "2.3.1",
      "io.circe"                   %% "circe-core"           % circeVersion,
      "io.circe"                   %% "circe-generic"        % circeVersion,
      "io.circe"                   %% "circe-parser"         % circeVersion,
      "io.circe"                   %% "circe-generic-extras" % circeExtVersion,
      "com.typesafe"                % "config"               % "1.4.1",
      "com.typesafe.scala-logging" %% "scala-logging"        % "3.9.2",
      //      "org.wickedsource" % "diffparser" % "1.0",
      //      "io.github.java-diff-utils" % "java-diff-utils" % "4.5",
    ) ++ testDeps
  )

val sttpsync = project
  .in(file("sttp-sync"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-sttp-sync",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.0.0",
      "com.softwaremill.sttp.client3" %% "httpclient-backend" % "3.0.0"
    ) ++ testDeps
  )

val gitlab4s = project
  .in(file("."))
  .aggregate(core, sttpsync)
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
