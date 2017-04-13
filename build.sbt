import sbt.Keys._

val commonLibraryDependencies = Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  // Test
  "org.scalatest" %% "scalatest" % "3.0.1"
)

val commonSettings = Seq(
  organization := "com.spotify",
  scalaVersion := "2.11.8",
  version := "0.1.0-SNAPSHOT",
  fork := true,
  libraryDependencies ++= commonLibraryDependencies
)

lazy val root: Project = Project(
  "hyperflo",
  file(".")
).aggregate(
  core,
  modules,
  examples
)

lazy val core : Project = project.in(file("hyperflo-core")).settings(
  commonSettings
)

lazy val examples: Project = project.in(file("hyperflo-examples")).settings(
  commonSettings,
  libraryDependencies ++= commonLibraryDependencies ++ Seq(
    "com.spotify" % "hype-submitter" % "0.0.13-SNAPSHOT"
  )
).dependsOn(
  gsutilcp,
  localsplit
)

// Modules
// FIXME: is this a good way to handle a modules? (esp. if a lot of them)

lazy val modules: Project = project.in(file("hyperflo-modules")).settings(
  commonSettings
  ).aggregate (
  word2vecModule,
  lexvecModule,
  gsutilcp
)

lazy val gsutilcp : Project = project.in(file("hyperflo-modules/gsutilcp")).settings(
  commonSettings
).dependsOn(
  core
)

lazy val localsplit : Project = project.in(file("hyperflo-modules/localsplit")).settings(
  commonSettings
).dependsOn(
  core
)

lazy val word2vecModule : Project = project.in(file("hyperflo-modules/word2vev")).settings(
  commonSettings
).dependsOn(
  core
)

lazy val lexvecModule : Project = project.in(file("hyperflo-modules/lexvec")).settings(
  commonSettings
).dependsOn(
  core
)

