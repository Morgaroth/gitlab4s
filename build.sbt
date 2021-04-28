val circeVersion    = "0.13.0"
val circeExtVersion = "0.13.0"
val silencerVersion = "1.7.1"

val validate = Def.taskKey[Unit]("Validates entire project")

val projectScalaVersion      = "2.13.4"
val crossScalaVersionsValues = Seq(projectScalaVersion, "2.12.13")

val commonSettings = Seq(
  organization := "io.morgaroth",
  scalaVersion := projectScalaVersion,
  crossScalaVersions := crossScalaVersionsValues,
  versionScheme := Some("semver-spec"),
  publishTo := Some("Mateusz Jaje JFrog Artifactory" at "https://mateuszjajedev.jfrog.io/artifactory/maven"),
  credentials += Credentials(file(sys.env.getOrElse("JFROG_CREDENTIALS_FILE", ".credentials"))),
  resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/",
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
  Test / testOptions += Tests.Filter(suiteName => !suiteName.endsWith("ISpec")),
  doc / sources := Seq.empty,
  publishMavenStyle := false,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest-flatspec"       % "3.2.3" % Test,
  "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.3" % Test,
  "ch.qos.logback" % "logback-classic"          % "1.2.3" % Test,
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
    ) ++ testDeps,
  )

val sttpjdk = project
  .in(file("sttp-jdk"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-sttp",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core"               % "3.0.0",
      "com.softwaremill.sttp.client3" %% "httpclient-backend" % "3.0.0",
    ) ++ testDeps,
  )

val sttptry = project
  .in(file("sttp-try"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-sttp-try",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.0.0",
    ) ++ testDeps,
  )

val gitlab4s = project
  .in(file("."))
  .aggregate(core, sttpjdk, sttptry)
  .settings(
    name := "gitlab4s",
    publish := {},
    publishLocal := {},
    publishTo := Some("Mateusz Jaje JFrog Artifactory" at "https://mateuszjajedev.jfrog.io/artifactory/maven"),
    crossScalaVersions := crossScalaVersionsValues,
    scalaVersion := projectScalaVersion,
    validate := Def.sequential {
      Test / test
      // tut.value
    }.value,
    // Release
    releaseTagComment := s"Releasing ${(ThisBuild / version).value} [skip ci]",
    releaseCommitMessage := s"Setting version to ${(ThisBuild / version).value} [skip ci]",
    releaseNextCommitMessage := s"Setting version to ${(ThisBuild / version).value} [skip ci]",
    releaseCrossBuild := true,
  )
