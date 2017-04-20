import sbt.Keys._

val commonLibraryDependencies = Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "com.spotify" %% "hype" % "0.0.16",
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
  examples
)

lazy val core : Project = project.in(file("hyperflo-core")).settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "org.scalanlp" %% "breeze" % "0.13"
  )
)

lazy val examples: Project = project.in(file("hyperflo-examples")).settings(
  commonSettings
).dependsOn(
  localsplitModule,
  word2vecModule,
  lexvecModule,
  evalEmbeddings,
  sciosplitModule
)

// Modules
// FIXME: is this a good way to handle a modules? (esp. if a lot of them)

lazy val localsplitModule : Project = project.in(file("hyperflo-modules/localsplit")).settings(
  commonSettings
).dependsOn(
  core
)

lazy val sciosplitModule : Project = project.in(file("hyperflo-modules/sciosplit")).settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "com.spotify" %% "scio-core" % "0.3.0-beta3"
  )
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

lazy val evalEmbeddings : Project = project.in(file("hyperflo-modules/eval-embeddings")).settings(
  commonSettings
).dependsOn(
  core
)

