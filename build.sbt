import sbt.Keys._

val commonSettings = Seq(
  organization := "com.spotify",
  scalaVersion := "2.11.8",
  version := "0.1.0-SNAPSHOT",
  fork := true
)

val commonLibraryDependencies = Seq(

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

lazy val modules: Project = project.in(file("hyperflo-modules")).settings(
  commonSettings
).dependsOn(
  core
)

lazy val examples: Project = project.in(file("hyperflo-examples")).settings(
  commonSettings
).dependsOn(
  core,
  modules
)