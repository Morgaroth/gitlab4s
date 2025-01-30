import Syntax.*
import com.jsuereth.sbtpgp.PgpKeys.publishSigned

val circeVersion    = "0.14.2"
val circeExtVersion = "0.14.2"
val sttpVersion     = "3.9.7"

val validate = Def.taskKey[Unit]("Validates entire project")

val scala2                   = "2.13.13"
val scala3                   = "3.3.1"
val projectScala             = scala2
val crossScalaVersionsValues = Seq(scala2, scala3, projectScala).distinct

val publishSettings = Seq(
  homepage := Some(url("https://gitlab.com/mateuszjaje/gitlab4s")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  developers       := List(Developer("Mateusz Jaje", "Mateusz Jaje", "mateuszjaje@gmail.com", new URL("https://gitlab.com/mateuszjaje"))),
  organizationName := "Mateusz Jaje",
  organizationHomepage := Some(url("https://gitlab.com/mateuszjaje")),
  versionScheme        := Some("semver-spec"),
  crossScalaVersions   := crossScalaVersionsValues,
  scalaVersion         := projectScala,
  publishMavenStyle    := true,
  publishTo            := sonatypeCentralPublishToBundle.value,
  releaseProcess := {
    import sbtrelease.ReleaseStateTransformations.*
    Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges,
    )
  },
)

val commonSettings = publishSettings ++ Seq(
  organization := "io.gitlab.mateuszjaje",
  resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/",
  scalaVersion := projectScala,
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
  ) ++ {
    if (scalaVersion.value.startsWith("2.13"))
      Seq(
        "-Ymacro-annotations",
        "-Ywarn-unused:imports",
        "-Xsource:3",
        "-P:kind-projector:underscore-placeholders",
      )
    else if (scalaVersion.value.startsWith("3."))
      Seq(
        "-rewrite",
        "-source",
        "3.2-migration",
        "-Ykind-projector",
        "-Xmax-inlines",
        "150",
      )
    else Seq.empty
  },
  libraryDependencies ++= {
    if (scalaVersion.value.startsWith("2.13"))
      Seq(compilerPlugin("org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full))
    else Seq.empty
  },
  idePackagePrefix.invisible := Some("io.gitlab.mateuszjaje.gitlabclient"),
  logBuffered                := false,
  doc / sources              := Seq.empty,
  Test / testOptions += Tests.Filter(suiteName => !suiteName.endsWith("ISpec")),
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest-flatspec"       % "3.2.13" % Test,
  "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.13" % Test,
  "ch.qos.logback" % "logback-classic"          % "1.2.11" % Test,
)

val core = project
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-core",
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-core"     % "2.8.0",
      "io.circe"                   %% "circe-core"    % circeVersion,
      "io.circe"                   %% "circe-generic" % circeVersion,
      "io.circe"                   %% "circe-parser"  % circeVersion,
      "io.circe"                   %% "circe-generic" % circeExtVersion,
      "com.typesafe"                % "config"        % "1.4.2",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    ) ++ testDeps,
  )

val sttpjdk = project
  .in(file("sttp-jdk"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-sttp-jdk",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
    ) ++ testDeps,
  )

val sttpzio1 = project
  .in(file("sttp-zio1"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-sttp-zio1",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "zio1" % sttpVersion,// for ZIO 1.x
    ) ++ testDeps,
  )

val sttpzio2 = project
  .in(file("sttp-zio"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-sttp-zio",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "zio"  % sttpVersion,
    ) ++ testDeps,
  )

val sttptry = project
  .in(file("sttp-try"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "gitlab4s-sttp-try",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
    ) ++ testDeps,
  )

val gitlab4s = project
  .in(file("."))
  .aggregate(core, sttpjdk, sttptry, sttpzio1, sttpzio2)
  .settings(publishSettings)
  .settings(
    organization                 := "io.gitlab.mateuszjaje",
    name                         := "gitlab4s",
    publish                      := {},
    publishSigned                := {},
    publishLocal                 := {},
    doc / sources                := Seq.empty,
    packageDoc / publishArtifact := false,
    validate := Def.sequential {
      Test / test
      // tut.value
    }.value,
    // Release
    releaseTagComment        := s"Releasing ${(ThisBuild / version).value}",
    releaseCommitMessage     := s"Setting version to ${(ThisBuild / version).value}\n[release commit]",
    releaseNextCommitMessage := s"Setting version to ${(ThisBuild / version).value}\n[skip ci]",
    releaseCrossBuild        := true,
  )
